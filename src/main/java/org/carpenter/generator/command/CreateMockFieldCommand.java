package org.carpenter.generator.command;

import org.apache.commons.lang3.StringUtils;
import org.carpenter.core.dto.unit.field.FieldProperties;
import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.dto.unit.field.FieldExtInfo;
import org.carpenter.generator.enums.TestFieldCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.carpenter.core.property.AbstractGenerationProperties.COMMON_UTIL_POSTFIX;
import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.TestGenerator.TEST_INST_VAR_NAME;
import static org.object2source.util.GenerationUtil.getClassShort;
import static org.object2source.util.GenerationUtil.getClearedClassName;

public class CreateMockFieldCommand extends AbstractReturnClassInfoCommand<FieldExtInfo> {

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
        if (TestFieldCategory.TEST_CLASS.equals(fieldCategory)) {
            fieldList = Collections.singletonList(mockFieldDeclaration());
        } else if (TestFieldCategory.MOCK_FIELD.equals(fieldCategory)) {
            fieldList = new ArrayList<>();
            for(FieldProperties f : callInfo.getServiceFields()) {
                fieldList.add(mockFieldDeclaration(f));
            }
        }
    }

    FieldExtInfo mockFieldDeclaration() {
        return mockFieldDeclaration(callInfo.getClassName(), TEST_INST_VAR_NAME, true);
    }

    FieldExtInfo mockFieldDeclaration(FieldProperties field) {
        StringBuilder typeNameBuilder = new StringBuilder();
        typeNameBuilder.append(field.getClassName());
        if(StringUtils.isNoneBlank(field.getGenericString())) {
            typeNameBuilder.append("<").append(field.getGenericString()).append(">");
        }
        return mockFieldDeclaration(typeNameBuilder.toString(), field.getUnitName(), false);
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
            String utilMethod = "createInstance(<type>)".replace("<type>", fieldTypeStr + ".class");
            fieldSb.append(" = ").append(commonUtilClass).append(".").append(utilMethod).append(";\n");
        } else {
            fieldSb.append(";\n");
        }

        result.setBody(fieldSb.toString());
        return result;
    }
}
