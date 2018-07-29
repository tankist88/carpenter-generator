package com.github.tankist88.carpenter.generator.dto.source;

import com.github.tankist88.carpenter.core.dto.unit.method.MethodBaseInfo;
import com.github.tankist88.carpenter.generator.dto.unit.method.MethodExtInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodSource extends MethodBaseInfo {
    private String testMethodDefinition;
    private String testMethodEnd;
    private List<MethodLine> lines;

    public String getTestMethodDefinition() {
        return testMethodDefinition;
    }

    public void setTestMethodDefinition(String testMethodDefinition) {
        this.testMethodDefinition = testMethodDefinition;
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

    public MethodExtInfo createMethodExtInfo() {
        return new MethodExtInfo(getClassName(), getUnitName(), this.toString());
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
