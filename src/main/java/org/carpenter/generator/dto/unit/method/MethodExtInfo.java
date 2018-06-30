package org.carpenter.generator.dto.unit.method;

import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.dto.source.MethodLine;
import org.carpenter.generator.dto.source.MethodSource;
import org.carpenter.generator.dto.source.Variable;
import org.carpenter.generator.dto.unit.AbstractUnitExtInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.carpenter.generator.dto.source.MethodLine.PLACE_HOLDER;
import static org.object2source.util.GenerationUtil.getClassShort;

public class MethodExtInfo extends AbstractUnitExtInfo {
    public MethodExtInfo() {
    }
    public MethodExtInfo(String className, String methodName, String body) {
        super(className, methodName, body);
    }

    public boolean hasMultipleMock() {
        for (MethodLine l : createMethodSource().getLines()) {
            if(l.getVariables().size() > 1) {
                return true;
            }
        }
        return false;
    }

    public MethodSource createMethodSource() {
        try {
            MethodSource methodSource = new MethodSource();

            GenerationProperties props = GenerationPropertiesFactory.loadProps();
            String dpName = props.getDataProviderClassPattern();

            String regularExp = getClassShort(dpName) + "[0-9]*\\.[A-Za-z0-9_]*\\(\\)";

            Pattern p = Pattern.compile(regularExp);

            String body = getBody();

            int startIndex = body.indexOf("{");
            int lastExpEndIndex = body.lastIndexOf(";");

            String methodDefinition = body.substring(0, startIndex + 1);
            String methodEnd = body.substring(lastExpEndIndex + 1);

            methodSource.setClassName(getClassName());
            methodSource.setUnitName(getUnitName());
            methodSource.setTestMethodDefinition(methodDefinition);
            methodSource.setTestMethodEnd(methodEnd);

            String content = body.substring(startIndex + 1, lastExpEndIndex + 1);

            String[] expressionArray = content.split(";");

            for (String line : expressionArray) {
                String expLine = line + ";";
                Matcher m = p.matcher(line);
                MethodLine methodLine = new MethodLine();
                int i = 1;
                while (m.find()) {
                    String text = m.group(0);
                    methodLine.getVariables().add(new Variable(i, text));
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
