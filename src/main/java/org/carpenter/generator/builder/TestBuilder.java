package org.carpenter.generator.builder;

import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.enums.TestFieldCategory;
import org.carpenter.generator.command.*;

import java.util.*;

public class TestBuilder {
    private Map<String, Set<ClassExtInfo>> classInfoMap;
    private Map<String, Set<String>> providerSignatureMap;
    private Map<String, Set<ClassExtInfo>> dataProviders;

    private List<ReturnCommand> commands;

    public TestBuilder() {
        this.classInfoMap = new HashMap<>();
        this.providerSignatureMap = new HashMap<>();
        this.dataProviders = new HashMap<>();
        this.commands = new ArrayList<>();
    }

    private void addClassInfo(List<? extends ClassExtInfo> infoList) {
        if(infoList == null || infoList.isEmpty()) return;
        for(ClassExtInfo classExtInfo : infoList) {
            String className = classExtInfo.getClassName();
            Set<ClassExtInfo> units = classInfoMap.get(className);
            if (units == null) {
                units = new HashSet<>();
                classInfoMap.put(className, units);
            }
            units.add(classExtInfo);
        }
    }

    public TestBuilder appendPreviousGenerated() {
        commands.add(new PopulatePrevMethodsCommand(providerSignatureMap));
        return this;
    }

    public TestBuilder appendInitMock(MethodCallInfo callInfo) {
        commands.add(new CreateInitMockMethodCommand(callInfo));
        return this;
    }

    public TestBuilder appendMockTestField(MethodCallInfo callInfo) {
        commands.add(new CreateMockFieldCommand(TestFieldCategory.TEST_CLASS, callInfo));
        return this;
    }

    public TestBuilder appendMockField(MethodCallInfo callInfo) {
        commands.add(new CreateMockFieldCommand(TestFieldCategory.MOCK_FIELD, callInfo));
        return this;
    }

    public TestBuilder appendTestMethod(MethodCallInfo callInfo) {
        commands.add(new CreateTestMethodCommand(callInfo, providerSignatureMap, dataProviders));
        return this;
    }

    @SuppressWarnings("unchecked")
    public void build() {
        for(ReturnCommand<ClassExtInfo> command : commands) {
            command.execute();
            addClassInfo(command.returnResult());
        }
        Command saveCommand = new CreateJavaClassesCommand(classInfoMap);
        saveCommand.execute();
    }
}
