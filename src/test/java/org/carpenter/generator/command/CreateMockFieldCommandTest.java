package org.carpenter.generator.command;

import org.carpenter.core.dto.unit.field.FieldProperties;
import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.generator.dto.unit.field.FieldExtInfo;
import org.testng.annotations.Test;

import java.io.Serializable;

import static org.carpenter.core.util.ConvertUtil.toFieldProperties;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CreateMockFieldCommandTest {

    @Test
    public void testMockFieldDeclaration() {
        MethodCallInfo callInfo1 = new MethodCallInfo();
        callInfo1.setClassName(CreateMockFieldCommandTest.class.getName());
        callInfo1.setClassHasZeroArgConstructor(true);
        CreateMockFieldCommand command1 = new CreateMockFieldCommand(null, callInfo1);

        FieldExtInfo testFieldExtInfo = command1.mockFieldDeclaration();

        assertEquals(testFieldExtInfo.getClassName(), CreateMockFieldCommandTest.class.getName());
        assertEquals(testFieldExtInfo.getUnitName(), "testInstance");
        assertTrue(testFieldExtInfo.getBody().contains("@Spy"));
        assertTrue(testFieldExtInfo.getBody().contains("@InjectMocks"));
        assertTrue(testFieldExtInfo.getBody().contains("private CreateMockFieldCommandTest testInstance;"));

        // --------------------------------

        MethodCallInfo callInfo2 = new MethodCallInfo();
        callInfo2.setClassName(TestMockFieldDeclarationClass.class.getName());
        CreateMockFieldCommand command2 = new CreateMockFieldCommand(null, callInfo2);

        FieldProperties f1 = toFieldProperties(TestMockFieldDeclarationClass.class.getDeclaredFields()[0]);
        FieldExtInfo fieldExtInfo1 = command2.mockFieldDeclaration(f1);

        assertEquals(fieldExtInfo1.getClassName(), TestMockFieldDeclarationClass.class.getName());
        assertEquals(fieldExtInfo1.getUnitName(), "testField");
        assertTrue(fieldExtInfo1.getBody().contains("@Mock"));
        assertTrue(
                fieldExtInfo1.getBody().contains("private org.carpenter.generator.command.CreateMockFieldCommandTest.GenericTestMockFieldDeclarationClass<java.lang.Long, java.lang.Double> testField;")
        );

        // --------------------------------

        MethodCallInfo callInfo3 = new MethodCallInfo();
        callInfo3.setClassName(TestMockFieldDeclarationClass.class.getName());
        CreateMockFieldCommand command3 = new CreateMockFieldCommand(null, callInfo3);

        FieldProperties f2 = toFieldProperties(TestMockFieldDeclarationClass.class.getDeclaredFields()[1]);
        FieldExtInfo fieldExtInfo2 = command3.mockFieldDeclaration(f2);

        assertEquals(fieldExtInfo2.getClassName(), TestMockFieldDeclarationClass.class.getName());
        assertEquals(fieldExtInfo2.getUnitName(), "testFieldNoGen");
        assertTrue(fieldExtInfo1.getBody().contains("@Mock"));
        assertTrue(
                fieldExtInfo2.getBody().contains("private org.carpenter.generator.command.CreateMockFieldCommandTest.WithoutGenericTestMockFieldDeclarationClass testFieldNoGen;")
        );
    }

    @SuppressWarnings("unused")
    private static class GenericTestMockFieldDeclarationClass<T extends Serializable, F extends Serializable> {
    }

    private static class WithoutGenericTestMockFieldDeclarationClass {
    }

    private static class TestMockFieldDeclarationClass {
        @SuppressWarnings("unused")
        private GenericTestMockFieldDeclarationClass<Long, Double> testField;
        @SuppressWarnings("unused")
        private WithoutGenericTestMockFieldDeclarationClass testFieldNoGen;
    }
}
