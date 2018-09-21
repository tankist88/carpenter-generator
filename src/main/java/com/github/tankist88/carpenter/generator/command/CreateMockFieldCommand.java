package com.github.tankist88.carpenter.generator.command;

import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.dto.unit.field.FieldExtInfo;
import com.github.tankist88.carpenter.generator.enums.TestFieldCategory;

import java.util.*;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.COMMON_UTIL_POSTFIX;
import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.TAB;
import static com.github.tankist88.carpenter.generator.TestGenerator.TEST_INST_VAR_NAME;
import static com.github.tankist88.carpenter.generator.enums.TestFieldCategory.MOCK_FIELD;
import static com.github.tankist88.carpenter.generator.enums.TestFieldCategory.TEST_CLASS;
import static com.github.tankist88.carpenter.generator.util.GenerateUtil.*;
import static com.github.tankist88.object2source.util.GenerationUtil.getClassShort;
import static com.github.tankist88.object2source.util.GenerationUtil.getClearedClassName;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

public class CreateMockFieldCommand extends AbstractReturnClassInfoCommand<FieldExtInfo> {
    static final String CREATE_INST_METHOD = "createInstance";
    
    private TestFieldCategory fieldCategory;
    private MethodCallInfo callInfo;
    private List<FieldExtInfo> fieldList;
    private GenerationProperties props;

    public CreateMockFieldCommand(TestFieldCategory fieldCategory, MethodCallInfo callInfo) {
        this.fieldCategory = fieldCategory;
        this.callInfo = callInfo;
        this.props = GenerationPropertiesFactory.loadProps();
    }

    @Override
    public void execute() {
        super.execute();
        createMockField();
    }

    @Override
    public List<FieldExtInfo> returnResult() {
        return fieldList;
    }

    private void createMockField() {
        if (TEST_CLASS.equals(fieldCategory)) {
            fieldList = Collections.singletonList(mockFieldDeclaration());
        } else if (MOCK_FIELD.equals(fieldCategory)) {
            fieldList = new ArrayList<>();
            for (FieldProperties f : callInfo.getServiceFields()) {
                fieldList.add(mockFieldDeclaration(f));
            }

            Set<FieldProperties> testClassHierarchy = createTestClassHierarchy(callInfo);
            Set<FieldProperties> serviceFields = createServiceFields(callInfo);

            Set<FieldExtInfo> innerFieldsSet = new HashSet<>();
            for (MethodCallInfo inner : callInfo.getInnerMethods()) {
                if (skipMock(inner, serviceFields, testClassHierarchy) || forwardMock(inner, testClassHierarchy)) {
                    continue;
                }
                if (inner.isMaybeServiceClass()) {
                    innerFieldsSet.add(mockFieldDeclaration(
                            inner.getNearestInstantAbleClass(),
                            inner.getGenericString(),
                            createVarNameFromMethod(inner.getUnitName())
                    ));
                }
            }
            fieldList.addAll(innerFieldsSet);
        }
    }

    FieldExtInfo mockFieldDeclaration() {
        return mockFieldDeclaration(callInfo.getClassName(), TEST_INST_VAR_NAME, true);
    }

    FieldExtInfo mockFieldDeclaration(FieldProperties field) {
        return mockFieldDeclaration(field.getClassName(), field.getGenericString(), field.getUnitName());
    }

    private FieldExtInfo mockFieldDeclaration(String classname, String genericStr, String varName) {
        StringBuilder typeNameBuilder = new StringBuilder();
        typeNameBuilder.append(classname);
        if (isNoneBlank(genericStr)) {
            typeNameBuilder.append("<").append(genericStr).append(">");
        }
        return mockFieldDeclaration(typeNameBuilder.toString(), varName, false);
    }

    private FieldExtInfo mockFieldDeclaration(String fieldType, String varName, boolean testClass) {
        FieldExtInfo result = new FieldExtInfo();
        result.setClassName(callInfo.getClassName());
        result.setUnitName(varName);
        StringBuilder fieldSb = new StringBuilder();
        if(testClass) fieldSb.append(TAB + "@Spy\n").append(TAB + "@InjectMocks\n");
        else fieldSb.append(TAB + "@Mock\n");

        String fieldTypeStr = testClass ? getClassShort(fieldType) : getClearedClassName(fieldType);
        fieldSb.append(TAB + "private ").append(fieldTypeStr).append(" ").append(result.getUnitName());

        if (testClass && !callInfo.isClassHasZeroArgConstructor()) {
            String commonUtilClass = props.getDataProviderClassPattern() + COMMON_UTIL_POSTFIX;
            String utilMethod = CREATE_INST_METHOD + "(<type>)".replace("<type>", fieldTypeStr + ".class");
            fieldSb.append(" = ").append(commonUtilClass).append(".").append(utilMethod).append(";\n");
        } else {
            fieldSb.append(";\n");
        }

        result.setBody(fieldSb.toString());
        return result;
    }
}
