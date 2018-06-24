package org.carpenter.generator.extension.assertext;

import org.carpenter.core.dto.argument.GeneratedArgument;

import java.util.GregorianCalendar;

import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.util.TypeHelper.simpleType;

public class SimpleAssertExtension implements AssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        return simpleType(returnValue) || isDateObject(returnValue);
    }

    @Override
    public String getAssertBlock(String dataProviderMethod) {
        return TAB + TAB + "assertEquals(result, " + dataProviderMethod + ");\n";
    }

    private boolean isDateObject(GeneratedArgument returnValue) {
        String className = returnValue.getNearestInstantAbleClass();
        return
                className.equals(java.util.Date.class.getName()) ||
                className.equals(GregorianCalendar.class.getName()) ||
                className.equals(java.sql.Date.class.getName());
    }
}
