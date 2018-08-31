package com.github.tankist88.carpenter.generator.extension.assertext.builder;

import org.testng.annotations.Test;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.testng.Assert.assertEquals;

public class AssertBuilderTest {
    @Test
    public void assertEqualsListTest() {
        assertEquals(
            new AssertBuilder("actual", "expected").assertEqualsList().toString(),
            getEqualsListAssertBlock("actual", "expected")
        );
    }

    private String getEqualsListAssertBlock(String actual, String expected) {
        return
                TAB + TAB + "assertEquals(" + actual + ".size(), " + expected + ".size());\n" +
                TAB + TAB + "Iterator iterator = " + actual + ".iterator();\n" +
                TAB + TAB + "while (iterator.hasNext()) {\n" +
                TAB + TAB + TAB + "assertTrue(" + expected +".contains(iterator.next()));\n" +
                TAB + TAB + "}\n";
    }

    @Test
    public void assertEqualsSimpleTest() {
        assertEquals(
                new AssertBuilder("actual", "expected").tab().tab().assertEquals().toString(),
                getSimpleAssertBlock("actual", "expected")
        );
    }

    private String getSimpleAssertBlock(String actual, String expected) {
        return TAB + TAB + "assertEquals(" + actual + ", " + expected + ");\n";
    }

    @Test
    public void assertEqualsDateTest() {
        assertEquals(
                new AssertBuilder("actual", "expected").tab().tab().assertEqualsBy("getTime()").toString(),
                getDateAssertBlock("actual", "expected")
        );
    }

    private String getDateAssertBlock(String actual, String expected) {
        return TAB + TAB + "assertEquals(" + actual + ".getTime(), " + expected + ".getTime());\n";
    }

    @Test
    public void assertEqualsCalendarTest() {
        String actual = new AssertBuilder("actual", "expected")
                .tab().tab().assertEqualsBy("getTimeInMillis()")
                .tab().tab().ifNotNullFor("getTimeZone()").then()
                .tab().tab().tab().assertEqualsBy("getTimeZone().getID()")
                .tab().tab().endIf().toString();
        assertEquals(actual, getCalendarAssertBlock("actual", "expected")
        );
    }

    private String getCalendarAssertBlock(String actual, String expected) {
        return  TAB + TAB + "assertEquals(" + actual + ".getTimeInMillis(), " + expected + ".getTimeInMillis());\n" +
                TAB + TAB + "if (" + actual + ".getTimeZone() != null) {\n" +
                TAB + TAB + TAB + "assertEquals(" + actual + ".getTimeZone().getID(), " + expected + ".getTimeZone().getID());\n" +
                TAB + TAB + "}\n";
    }
}
