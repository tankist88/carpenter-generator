package com.github.tankist88.carpenter.generator;

import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.builder.TestBuilder;
import com.github.tankist88.carpenter.generator.service.LoadDataService;

import java.lang.reflect.Modifier;

import static com.github.tankist88.carpenter.generator.util.GenerateUtil.allowedPackage;
import static com.github.tankist88.object2source.util.GenerationUtil.isAnonymousClass;

public class TestGenerator {
    public static final String GENERATED_TEST_CLASS_POSTFIX = "GeneratedTest";
    public static final String TEST_INST_VAR_NAME = "testInstance";

    private GenerationProperties props;
    private LoadDataService loadDataService;

    private TestGenerator() {
        this.props = GenerationPropertiesFactory.loadProps();
        this.loadDataService = new LoadDataService();
    }

    private boolean skipTestMethod(MethodCallInfo callInfo) {
        return skipTestMethod(callInfo, props);
    }

    private static boolean skipTestMethod(MethodCallInfo callInfo, GenerationProperties props) {
        boolean deniedModifier = Modifier.isPrivate(callInfo.getMethodModifiers());
        boolean deniedDeclarationPlace = !callInfo.getClassName().equals(callInfo.getDeclaringTypeName());
        boolean deniedClassType = callInfo.isMemberClass() && !Modifier.isStatic(callInfo.getClassModifiers());
        boolean anonymousClass = isAnonymousClass(callInfo.getClassName());
        boolean hasZeroArgConstructor = callInfo.isClassHasZeroArgConstructor();
        return !allowedPackage(callInfo.getClassName(), props) || deniedModifier || deniedClassType || deniedDeclarationPlace || anonymousClass || !hasZeroArgConstructor;
    }

    private int generate() {
        TestBuilder testBuilder = new TestBuilder();
        testBuilder.appendPreviousGenerated();
        for (MethodCallInfo callInfo : loadDataService.loadObjectDump()) {
            if(skipTestMethod(callInfo)) continue;
            testBuilder.appendMockTestField(callInfo);
            testBuilder.appendMockField(callInfo);
            testBuilder.appendInitMock(callInfo);
            testBuilder.appendTestMethod(callInfo);
        }
        return testBuilder.build();
    }

    public static void main(String args[]) {
        int generatedTests = (new TestGenerator()).generate();
        System.out.println("Generated tests count: " + generatedTests);
    }
}