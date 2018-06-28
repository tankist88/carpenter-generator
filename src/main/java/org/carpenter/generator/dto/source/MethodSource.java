package org.carpenter.generator.dto.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodSource {
    private String unitName;
    private String testMethodName;
    private String testMethodDefinition;
    private String testMethodEnd;
    private List<MethodLine> lines;

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public void setTestMethodName(String testMethodName) {
        this.testMethodName = testMethodName;
    }

    public String getTestMethodDefinition() {
        return testMethodDefinition;
    }

    public void setTestMethodDefinition(String testMethodDefinition) {
        this.testMethodDefinition = testMethodDefinition;
    }

    public String getTestMethodEnd() {
        return testMethodEnd;
    }

    public void setTestMethodEnd(String testMethodEnd) {
        this.testMethodEnd = testMethodEnd;
    }

    public List<MethodLine> getLines() {
        if (lines == null) {
            lines = new ArrayList<>();
        }
        return lines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodSource that = (MethodSource) o;
        return Objects.equals(lines, that.lines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lines);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(testMethodDefinition);
        for (MethodLine l : getLines()) {
            sb.append(l);
        }
        sb.append(testMethodEnd);
        return sb.toString();
    }
}
