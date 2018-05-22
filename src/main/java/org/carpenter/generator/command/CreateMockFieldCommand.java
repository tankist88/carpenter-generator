package org.carpenter.generator.command;

import org.apache.commons.lang3.StringUtils;
import org.carpenter.core.dto.unit.field.FieldProperties;
import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.generator.dto.unit.field.FieldExtInfo;
import org.carpenter.generator.enums.TestFieldCategory;
import org.carpenter.generator.util.TypeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.TestGenerator.TEST_INST_VAR_NAME;
import static org.object2source.util.GenerationUtil.getClassShort;

public class CreateMockFieldCommand extends AbstractReturnClassInfoCommand<FieldExtInfo> {

    private TestFieldCategory fieldCategory;
    private MethodCallInfo callInfo;

    private List<FieldExtInfo> fieldList;

    public CreateMockFieldCommand(TestFieldCategory fieldCategory, MethodCallInfo callInfo) {
        this.fieldCategory = fieldCategory;
        this.callInfo = callInfo;
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
        if (TestFieldCategory.TEST_CLASS.equals(fieldCategory)) {
            fieldList = Collections.singletonList(mockFieldDeclaration(callInfo.getClassName()));
        } else if (TestFieldCategory.MOCK_FIELD.equals(fieldCategory)) {
            fieldList = new ArrayList<>();
            for(FieldProperties f : callInfo.getServiceFields()) {
                fieldList.add(mockFieldDeclaration(callInfo.getClassName(), f));
            }
        }
    }

    FieldExtInfo mockFieldDeclaration(String ownerClassName) {
        return mockFieldDeclaration(ownerClassName, ownerClassName, TEST_INST_VAR_NAME, true);
    }

    FieldExtInfo mockFieldDeclaration(String ownerClassName, FieldProperties field) {
        StringBuilder typeNameBuilder = new StringBuilder();
        typeNameBuilder.append(field.getClassName());
        if(StringUtils.isNoneBlank(field.getGenericString())) {
            typeNameBuilder.append("<").append(field.getGenericString()).append(">");
        }
        return mockFieldDeclaration(ownerClassName, typeNameBuilder.toString(), field.getUnitName(), false);
    }

    private FieldExtInfo mockFieldDeclaration(String ownerClassName, String typeName, String varName, boolean testClass) {
        FieldExtInfo result = new FieldExtInfo();
        result.setClassName(ownerClassName);
        result.setUnitName(varName);
        StringBuilder fieldSb = new StringBuilder();
        if(testClass) fieldSb.append(TAB + "@Spy\n").append(TAB + "@InjectMocks\n");
        else fieldSb.append(TAB + "@Mock\n");
        fieldSb
                .append(TAB + "private ")
                .append(testClass ? getClassShort(typeName) : TypeHelper.clearClassName(typeName))
                .append(" ")
                .append(result.getUnitName())
                .append(";\n");
        result.setBody(fieldSb.toString());
        return result;
    }
}
