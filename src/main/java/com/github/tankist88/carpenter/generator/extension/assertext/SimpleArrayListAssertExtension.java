package com.github.tankist88.carpenter.generator.extension.assertext;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;
import com.github.tankist88.carpenter.generator.extension.assertext.builder.AssertBuilder;
import com.github.tankist88.object2source.dto.ProviderResult;

import java.util.List;

import static com.github.tankist88.object2source.util.GenerationUtil.DATA_PROVIDER_METHOD_START;

public class SimpleArrayListAssertExtension implements AssertExtension {
    @Override
    public boolean isTypeSupported(GeneratedArgument returnValue) {
        String type = returnValue.getNearestInstantAbleClass();
        if (!type.equals(List.class.getName())) return false;
        ProviderResult pr = returnValue.getGenerated();
        return  pr != null &&
                pr.getEndPoint().getMethodBody().contains(".add(") &&
                !pr.getEndPoint().getMethodBody().contains(".add(" + DATA_PROVIDER_METHOD_START);
    }

    @Override
    public String getAssertBlock(String actual, String expected) {
        return new AssertBuilder(actual, expected).assertEqualsList().toString();
    }
}
