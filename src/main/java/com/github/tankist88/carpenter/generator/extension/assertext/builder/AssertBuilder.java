package com.github.tankist88.carpenter.generator.extension.assertext.builder;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.TAB;

public class AssertBuilder {
    private String actual;
    private String expected;
    private StringBuilder builder = new StringBuilder();

    public AssertBuilder(String actual, String expected) {
        this.actual = actual;
        this.expected = expected;
    }

    public AssertBuilder tab() {
        builder.append(TAB);
        return this;
    }

    public AssertBuilder assertEqualsList() {
        builder .append(TAB).append(TAB)
                .append(new AssertBuilder(actual, expected).assertEqualsBy("size()"))
                .append(TAB).append(TAB)
                .append("Iterator iterator = ").append(actual).append(".iterator();\n")
                .append(TAB).append(TAB).append("while (iterator.hasNext()) {\n")
                .append(TAB).append(TAB).append(TAB)
                .append(new AssertBuilder(actual, expected).assertTrueForExpect("contains(iterator.next())"))
                .append(TAB).append(TAB).append("}\n");
        return this;
    }

    public AssertBuilder assertEquals() {
        builder .append("assertEquals(")
                .append(actual)
                .append(", ")
                .append(expected)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertEqualsBy(String elementToCheck) {
        builder .append("assertEquals(")
                .append(actual)
                .append(".")
                .append(elementToCheck)
                .append(", ")
                .append(expected)
                .append(".")
                .append(elementToCheck)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertTrue() {
        builder .append("assertTrue(")
                .append(actual)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertTrueFor(String elementToCheck) {
        builder .append("assertTrue(")
                .append(actual)
                .append(".")
                .append(elementToCheck)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertTrueExpect() {
        builder .append("assertTrue(")
                .append(expected)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertTrueForExpect(String elementToCheck) {
        builder .append("assertTrue(")
                .append(expected)
                .append(".")
                .append(elementToCheck)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertFalse() {
        builder .append("assertFalse(")
                .append(actual)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertFalseFor(String elementToCheck) {
        builder .append("assertFalse(")
                .append(actual)
                .append(".")
                .append(elementToCheck)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertNull() {
        builder .append("assertNull(")
                .append(actual)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertNullFor(String elementToCheck) {
        builder .append("assertNull(")
                .append(actual)
                .append(".")
                .append(elementToCheck)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertNotNull() {
        builder .append("assertNotNull(")
                .append(actual)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder assertNotNullFor(String elementToCheck) {
        builder .append("assertNotNull(")
                .append(actual)
                .append(".")
                .append(elementToCheck)
                .append(")")
                .append(";\n");
        return this;
    }

    public AssertBuilder ifNullFor(String elementToCheck) {
        builder .append("if (")
                .append(actual)
                .append(".")
                .append(elementToCheck)
                .append(" == null)");
        return this;
    }

    public AssertBuilder ifNull() {
        builder .append("if (")
                .append(actual)
                .append(" == null)");
        return this;
    }

    public AssertBuilder ifNotNullFor(String elementToCheck) {
        builder .append("if (")
                .append(actual)
                .append(".")
                .append(elementToCheck)
                .append(" != null)");
        return this;
    }

    public AssertBuilder ifNotNull() {
        builder .append("if (")
                .append(actual)
                .append(" != null)");
        return this;
    }

    public AssertBuilder then() {
        builder.append(" {\n");
        return this;
    }

    public AssertBuilder endIf() {
        builder.append("}\n");
        return this;
    }

    protected StringBuilder getBuilder() {
        return builder;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
