package org.carpenter.generator.dto;

import org.carpenter.core.dto.argument.GeneratedArgument;
import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.object2source.SourceGenerator;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.AssertJUnit.assertEquals;

public class MethodCallInfoHashCodeTest {
    @Test
    public void hashCodeTest() {
        MethodCallInfo utd = new MethodCallInfo();
        utd.setMethodModifiers(1);
        utd.setVoidMethod(false);
        utd.setArguments(new ArrayList<GeneratedArgument>());
        utd.setReturnArg(new GeneratedArgument(
                Integer.class.getName(),
                (new SourceGenerator()).createDataProviderMethod(433474))
        );
        utd.setClassName("org.example.Object");
        utd.setUnitName("hashCode");

        MethodCallInfo utd2 = new MethodCallInfo();
        utd2.setMethodModifiers(1);
        utd2.setVoidMethod(false);
        utd2.setArguments(new ArrayList<GeneratedArgument>());
        utd2.setClassName("org.example.Object");
        utd2.setUnitName("hashCode");

        assertEquals(utd.hashCode(), utd2.hashCode());
    }
}
