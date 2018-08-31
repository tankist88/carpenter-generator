package com.github.tankist88.carpenter.generator.extension.assertext;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;
import com.github.tankist88.carpenter.generator.extension.assertext.builder.AssertBuilder;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class CalendarAssertExtension implements AssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        return isCalendarObject(returnValue);
    }

    @Override
    public String getAssertBlock(String actual, String expected) {
        return new AssertBuilder(actual, expected)
                .tab().tab().assertEqualsBy("getTimeInMillis()")
                .tab().tab().ifNotNullFor("getTimeZone()").then()
                .tab().tab().tab().assertEqualsBy("getTimeZone().getID()")
                .tab().tab().endIf().toString();
    }

    private boolean isCalendarObject(GeneratedArgument returnValue) {
        String className = returnValue.getNearestInstantAbleClass();
        return  className.equals(GregorianCalendar.class.getName()) ||
                className.equals(Calendar.class.getName());
    }
}
