package com.github.tankist88.carpenter.generator.extension.assertext;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;
import com.github.tankist88.object2source.dto.ProviderResult;

import java.util.List;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.TAB;

public class SimpleArrayListAssertExtension implements AssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        String type = returnValue.getNearestInstantAbleClass();
        if (!type.equals(List.class.getName())) return false;
        ProviderResult pr = returnValue.getGenerated();
        return pr != null && pr.getEndPoint().getMethodBody().contains(".add(") && !pr.getEndPoint().getMethodBody().contains(".add(get");
    }

    @Override
    public String getAssertBlock(String dataProviderMethod) {
        return
                TAB + TAB + "assertEquals(result.size(), " + dataProviderMethod + ".size());\n" +
                TAB + TAB + "Iterator iterator = result.iterator();\n" +
                TAB + TAB + "while (iterator.hasNext()) {\n" +
                TAB + TAB + TAB + "assertTrue(" + dataProviderMethod +".contains(iterator.next()));\n" +
                TAB + TAB + "}\n";
    }
}
