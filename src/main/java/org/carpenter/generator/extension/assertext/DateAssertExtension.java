package org.carpenter.generator.extension.assertext;

import org.carpenter.core.dto.argument.GeneratedArgument;

import static org.carpenter.core.property.AbstractGenerationProperties.TAB;

public class DateAssertExtension extends AbstractAssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        return isDateObject(returnValue);
    }

    @Override
    protected String getExpression(String varPlaceHolder) {
        return TAB + TAB + "assertEquals(result.getTime(), " + varPlaceHolder + ".getTime());\n";
    }

    private boolean isDateObject(GeneratedArgument returnValue) {
        String className = returnValue.getNearestInstantAbleClass();
        return  className.equals(java.util.Date.class.getName()) ||
                className.equals(java.sql.Date.class.getName());
    }
}
