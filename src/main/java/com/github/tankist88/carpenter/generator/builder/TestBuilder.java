package com.github.tankist88.carpenter.generator.builder;

import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.command.*;
import com.github.tankist88.carpenter.generator.dto.unit.ClassExtInfo;
import com.github.tankist88.carpenter.generator.dto.unit.method.MethodExtInfo;
import com.github.tankist88.carpenter.generator.extension.assertext.AssertExtension;
import com.github.tankist88.carpenter.generator.extension.assertext.CalendarAssertExtension;
import com.github.tankist88.carpenter.generator.extension.assertext.DateAssertExtension;
import com.github.tankist88.carpenter.generator.extension.assertext.SimpleAssertExtension;

import java.util.*;

import static com.github.tankist88.carpenter.generator.command.CreateTestMethodCommand.TEST_METHOD_PREFIX;
import static com.github.tankist88.carpenter.generator.enums.TestFieldCategory.MOCK_FIELD;
import static com.github.tankist88.carpenter.generator.enums.TestFieldCategory.TEST_CLASS;

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
        commands.add(new CreateMockFieldCommand(TEST_CLASS, callInfo));
        return this;
    }

    public TestBuilder appendMockField(MethodCallInfo callInfo) {
        commands.add(new CreateMockFieldCommand(MOCK_FIELD, callInfo));
        return this;
    }

    public TestBuilder appendTestMethod(MethodCallInfo callInfo) {
        commands.add(new CreateTestMethodCommand(callInfo, providerSignatureMap, dataProviders, assertExtensions));
        return this;
    }

    @SuppressWarnings("unchecked")
    public int build() {
        for(ReturnCommand<ClassExtInfo> command : commands) {
            command.execute();
            addClassInfo(command.returnResult());
        }
        Command saveCommand = new CreateJavaClassesCommand(classInfoMap);
        saveCommand.execute();
        int generatedTests = 0;
        for (Set<ClassExtInfo> extInfos : classInfoMap.values()) {
            for (ClassExtInfo extInfo : extInfos) {
                if (extInfo instanceof MethodExtInfo &&
                    extInfo.getUnitName().startsWith(TEST_METHOD_PREFIX)) {
                    generatedTests++;
                }
            }
        }
        return generatedTests;
    }
}
