package org.carpenter.generator.command;

import org.carpenter.core.dto.unit.field.FieldProperties;
import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.TestGenerator.TEST_INST_VAR_NAME;

public class CreateInitMockMethodCommand extends AbstractReturnClassInfoCommand<MethodExtInfo> {
    static final String INIT_METHOD = "init()";

    private StringBuilder builder;

    private MethodCallInfo callInfo;

    private List<MethodExtInfo> methods;

    public CreateInitMockMethodCommand(MethodCallInfo callInfo) {
        this.callInfo = callInfo;
        this.builder = new StringBuilder();
    }

    @Override
    public void execute() {
        super.execute();
        createInitMockMethod();
    }

    @Override
    public List<MethodExtInfo> returnResult() {
        return methods;
    }

    private void createInitMockMethod() {
        builder.append(TAB + "@BeforeMethod\n")
               .append(TAB + "public void ")
               .append(INIT_METHOD)
               .append(" {\n")
               .append(TAB + TAB + "initMocks(this);\n");

        for (FieldProperties f : callInfo.getServiceFields()) {
            if (f.getDeclaringClass().equals(callInfo.getClassName()) && !Modifier.isPrivate(f.getModifiers())) {
                builder.append(TAB + TAB + TEST_INST_VAR_NAME).append(".")
                       .append(f.getUnitName()).append(" = ").append(f.getUnitName()).append(";\n");
            }
        }

        builder.append(TAB + "}\n");
        methods = Collections.singletonList(new MethodExtInfo(callInfo.getClassName(), INIT_METHOD, builder.toString()));
    }
}
