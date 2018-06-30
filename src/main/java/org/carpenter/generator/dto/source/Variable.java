package org.carpenter.generator.dto.source;

import java.util.Objects;

public class Variable {
    private int num;
    private String value;
    private String type;

    public Variable(int num, String value) {
        this(num, value, Object.class.getName());
    }

    public Variable(int num, String value, String type) {
        this.num = num;
        this.value = value;
        this.type = type;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return num == variable.num &&
                Objects.equals(value, variable.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, value);
    }

    @Override
    public String toString() {
        return value;
    }
}
