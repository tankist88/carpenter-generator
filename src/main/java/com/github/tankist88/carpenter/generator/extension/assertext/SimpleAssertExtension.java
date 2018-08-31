package com.github.tankist88.carpenter.generator.extension.assertext;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;
import com.github.tankist88.carpenter.generator.extension.assertext.builder.AssertBuilder;

import java.util.GregorianCalendar;

import static com.github.tankist88.carpenter.generator.util.TypeHelper.simpleType;

public class SimpleAssertExtension implements AssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        return simpleType(returnValue) || isDateObject(returnValue);
    }

    @Override
    public String getAssertBlock(String actual, String expected) {
        return new AssertBuilder(actual, expected)
                .tab().tab().assertEquals().toString();
    }

    private boolean isDateObject(GeneratedArgument returnValue) {
        String className = returnValue.getNearestInstantAbleClass();
        return
                className.equals(java.util.Date.class.getName()) ||
                className.equals(GregorianCalendar.class.getName()) ||
                className.equals(java.sql.Date.class.getName());
    }
}
