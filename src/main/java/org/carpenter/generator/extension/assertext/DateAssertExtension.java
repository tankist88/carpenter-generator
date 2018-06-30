package org.carpenter.generator.extension.assertext;

import org.carpenter.core.dto.argument.GeneratedArgument;

import static org.carpenter.core.property.AbstractGenerationProperties.TAB;

public class DateAssertExtension implements AssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        return isDateObject(returnValue);
    }

    @Override
    public String getAssertBlock(String dataProviderMethod) {
        return TAB + TAB + "assertEquals(result.getTime(), " + dataProviderMethod + ".getTime());\n";
    }

    private boolean isDateObject(GeneratedArgument returnValue) {
        String className = returnValue.getNearestInstantAbleClass();
        return  className.equals(java.util.Date.class.getName()) ||
                className.equals(java.sql.Date.class.getName());
    }
}
