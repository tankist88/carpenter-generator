package org.carpenter.generator.extension.assertext;

import org.carpenter.core.dto.argument.GeneratedArgument;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.carpenter.core.property.AbstractGenerationProperties.TAB;

public class CalendarAssertExtension implements AssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        return isCalendarObject(returnValue);
    }

    @Override
    public String getAssertBlock(String dataProviderMethod) {
        return  TAB + TAB + "assertEquals(result.getTimeInMillis(), " + dataProviderMethod + ".getTimeInMillis());\n" +
                TAB + TAB + "if (result.getTimeZone() != null) { \n" +
                TAB + TAB + TAB + "assertEquals(result.getTimeZone().getID(), " + dataProviderMethod + ".getTimeZone().getID());\n" +
                TAB + TAB + "}\n";
    }

    private boolean isCalendarObject(GeneratedArgument returnValue) {
        String className = returnValue.getNearestInstantAbleClass();
        return  className.equals(GregorianCalendar.class.getName()) ||
                className.equals(Calendar.class.getName());
    }
}
