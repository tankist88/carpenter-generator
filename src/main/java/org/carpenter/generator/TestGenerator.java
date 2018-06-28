package org.carpenter.generator;

import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.builder.TestBuilder;
import org.carpenter.generator.service.LoadDataService;

import java.lang.reflect.Modifier;

import static org.carpenter.generator.util.GenerateUtil.allowedPackage;
import static org.object2source.util.GenerationUtil.getLastClassShort;

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
        boolean anonymousClass = getLastClassShort(callInfo.getClassName()).matches("\\d+");
//        boolean hasNotZeroArgConstructor = !callInfo.isClassHasZeroArgConstructor();
        return !allowedPackage(callInfo.getClassName(), props) || deniedModifier || deniedClassType || deniedDeclarationPlace || anonymousClass;// || hasNotZeroArgConstructor;
    }

    private int generate() {
        int generatedTests = 0;
        TestBuilder testBuilder = new TestBuilder();
        testBuilder.appendPreviousGenerated();
        for(MethodCallInfo callInfo : loadDataService.loadObjectDump()) {
            if(skipTestMethod(callInfo)) continue;
            testBuilder.appendMockTestField(callInfo);
            testBuilder.appendMockField(callInfo);
            testBuilder.appendInitMock(callInfo);
            testBuilder.appendTestMethod(callInfo);
            generatedTests++;
        }
        testBuilder.build();

        return generatedTests;
    }

    public static void main(String args[]) {
        int generatedTests = (new TestGenerator()).generate();
        System.out.println("Generated tests count: " + generatedTests);
    }
}
