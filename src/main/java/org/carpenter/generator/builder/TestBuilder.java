package org.carpenter.generator.builder;

import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.command.*;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.enums.TestFieldCategory;
import org.carpenter.generator.extension.assertext.AssertExtension;
import org.carpenter.generator.extension.assertext.CalendarAssertExtension;
import org.carpenter.generator.extension.assertext.DateAssertExtension;
import org.carpenter.generator.extension.assertext.SimpleAssertExtension;

import java.util.*;

public class TestBuilder {
    private Map<String, Set<ClassExtInfo>> classInfoMap;
    private Map<String, Set<String>> providerSignatureMap;
    private Map<String, Set<ClassExtInfo>> dataProviders;

    private List<ReturnCommand> commands;

    private List<String> extensionClasses;
    private List<AssertExtension> assertExtensions;

    public TestBuilder() {
        this.classInfoMap = new HashMap<>();
        this.providerSignatureMap = new HashMap<>();
        this.dataProviders = new HashMap<>();
        this.commands = new ArrayList<>();
        this.extensionClasses = new ArrayList<>();
        this.assertExtensions = new ArrayList<>();
        initDefaultExtensions();
        initAssertExtensionsFromClassPath();
    }

    private void initDefaultExtensions() {
        registerExtension(new SimpleAssertExtension());
        registerExtension(new CalendarAssertExtension());
        registerExtension(new DateAssertExtension());
    }

    private void initAssertExtensionsFromClassPath() {
        GenerationProperties props = GenerationPropertiesFactory.loadProps();
        for(String classname : props.getExternalAssertExtensionClassNames()) {
            try {
                AssertExtension ext = (AssertExtension) Class.forName(classname).newInstance();
                registerExtension(ext);
            } catch (ReflectiveOperationException reflEx) {
                throw new IllegalStateException(reflEx);
            }
        }
    }

    private void registerExtension(AssertExtension extension) {
        if (!extensionClasses.contains(extension.getClass().getName())) {
            assertExtensions.add(0, extension);
            extensionClasses.add(extension.getClass().getName());
        }
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
        commands.add(new CreateTestMethodCommand(callInfo, providerSignatureMap, dataProviders, assertExtensions));
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
