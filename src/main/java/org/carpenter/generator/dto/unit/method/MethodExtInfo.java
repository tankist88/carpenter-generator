package org.carpenter.generator.dto.unit.method;

import org.carpenter.generator.dto.source.MethodLine;
import org.carpenter.generator.dto.source.MethodSource;
import org.carpenter.generator.dto.source.Variable;
import org.carpenter.generator.dto.unit.AbstractUnitExtInfo;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.carpenter.generator.command.CreateTestMethodCommand.HASH_CODE_SEPARATOR;
import static org.carpenter.generator.command.CreateTestMethodCommand.TEST_ANNOTATION;
import static org.carpenter.generator.command.CreateTestMethodCommand.TEST_METHOD_PREFIX;
import static org.carpenter.generator.dto.source.MethodLine.PLACE_HOLDER;
import static org.object2source.util.GenerationUtil.upFirst;

public class MethodExtInfo extends AbstractUnitExtInfo {
    private static final List<String> JAVA_MODIFIERS = Arrays.asList(
        "private", "public", "protected", "static", "final", "volatile", "transient"
    );

    public MethodExtInfo() {
    }
    public MethodExtInfo(String className, String unitName, String body) {
        super(className, validateUnitName(unitName), body);
    }

    public String createCommonMethodName() {
        if (getUnitName().startsWith(TEST_METHOD_PREFIX)) {
            return getUnitName().substring(0, getUnitName().indexOf(HASH_CODE_SEPARATOR));
        } else {
            return "common" + upFirst(getUnitName());
        }
    }

    public boolean isTestMethod() {
        return getUnitName().startsWith(TEST_METHOD_PREFIX) && getBody().contains(TEST_ANNOTATION);
    }

    private static String validateUnitName(String unitName) {
        if(!unitName.contains("(") || !unitName.contains(")")) {
            throw new IllegalArgumentException("Unit name for method will be contains arguments signature!");
        }
        return unitName;
    }

    @Override
    public void setUnitName(String unitName) {
        super.setUnitName(validateUnitName(unitName));
    }

    public MethodSource createMethodSource() {
        try {
            MethodSource methodSource = new MethodSource();

            String providerExp = "get[A-Za-z0-9_]+_[0-9_]+\\(\\)";
            Pattern p = Pattern.compile(providerExp);

            String body = getBody();

            int startIndex = body.indexOf("{") + 2;
            int lastExpEndIndex = body.lastIndexOf(";") + 2;

            String methodDefinition = body.substring(0, startIndex);
            String methodEnd = body.substring(lastExpEndIndex);

            methodSource.setClassName(getClassName());
            methodSource.setUnitName(getUnitName());
            methodSource.setTestMethodDefinition(methodDefinition);
            methodSource.setTestMethodEnd(methodEnd);

            String content = body.substring(startIndex, lastExpEndIndex);

            String[] expressionArray = content.split(";\n");

            for (String line : expressionArray) {
                String expLine = line + ";\n";
                Matcher m = p.matcher(line);
                MethodLine methodLine = new MethodLine();
                int i = 1;
                while (m.find()) {
                    String text = m.group(0);
                    if (line.replace(" ", "").indexOf("=") > 0) {
                        String typeStr = Object.class.getName();
                        for (String e : line.trim().split(" ")) {
                            if (!JAVA_MODIFIERS.contains(e.trim())) {
                                typeStr = e;
                                break;
                            }
                        }
                        String type = typeStr.replace(" ", "");
                        methodLine.getVariables().add(new Variable(i, text, type));
                    } else {
                        methodLine.getVariables().add(new Variable(i, text));
                    }
                    expLine = expLine.replace(text, PLACE_HOLDER.replace("?", String.valueOf(i)));
                    i++;
                }
                methodLine.setExpression(expLine);
                methodSource.getLines().add(methodLine);
            }
            return methodSource;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
