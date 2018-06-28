package org.carpenter.generator.extension.assertext;

import org.carpenter.core.dto.argument.GeneratedArgument;

import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.util.TypeHelper.simpleType;

public class SimpleAssertExtension extends AbstractAssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        return simpleType(returnValue);
    }

    @Override
    protected String getExpression(String varPlaceHolder) {
        return TAB + TAB + "assertEquals(result, " + varPlaceHolder + ");\n";
    }
}
