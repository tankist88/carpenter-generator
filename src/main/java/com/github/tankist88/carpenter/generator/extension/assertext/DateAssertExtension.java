package com.github.tankist88.carpenter.generator.extension.assertext;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;
import com.github.tankist88.carpenter.generator.extension.assertext.builder.AssertBuilder;

public class DateAssertExtension implements AssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        return isDateObject(returnValue);
    }

    @Override
    public String getAssertBlock(String actual, String expected) {
        return new AssertBuilder(actual, expected)
                .tab().tab().assertEqualsBy("getTime()").toString();
    }

    private boolean isDateObject(GeneratedArgument returnValue) {
        String className = returnValue.getNearestInstantAbleClass();
        return  className.equals(java.util.Date.class.getName()) ||
                className.equals(java.sql.Date.class.getName());
    }
}
