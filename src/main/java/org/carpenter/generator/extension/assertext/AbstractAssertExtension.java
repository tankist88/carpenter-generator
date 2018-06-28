package org.carpenter.generator.extension.assertext;

import org.carpenter.generator.dto.source.MethodLine;
import org.carpenter.generator.dto.source.Variable;

import static org.carpenter.generator.dto.source.MethodLine.PLACE_HOLDER;

public abstract class AbstractAssertExtension implements AssertExtension {
    @Override
    public MethodLine getAssertBlock(String dataProviderMethod) {
        MethodLine line = new MethodLine();
        line.getVariables().add(new Variable(1, dataProviderMethod));
        line.setExpression(getExpression(PLACE_HOLDER.replace("?", "1")));
        return line;
    }

    protected abstract String getExpression(String varPlaceHolder);
}
