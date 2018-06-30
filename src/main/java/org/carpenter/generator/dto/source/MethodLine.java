package org.carpenter.generator.dto.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodLine {
    public static final String PLACE_HOLDER = "<?>";

    private String expression;
    private List<Variable> variables;

    public MethodLine() {
    }

    public MethodLine(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public List<Variable> getVariables() {
        if (variables == null) {
            variables = new ArrayList<>();
        }
        return variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodLine that = (MethodLine) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }

    @Override
    public String toString() {
        String resultExp = expression;
        for (Variable v : getVariables()) {
            resultExp = resultExp.replace(PLACE_HOLDER.replace("?", String.valueOf(v.getNum())), v.getValue());
        }
        return resultExp;
    }
}
