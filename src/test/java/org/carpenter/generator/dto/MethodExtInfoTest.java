package org.carpenter.generator.dto;

import org.carpenter.generator.dto.source.MethodSource;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class MethodExtInfoTest {
    private static final String TAB = " " + " " + " " + " ";

    @Test
    public void testHashCode() {

        StringBuilder initMockMethodBuilder1 = new StringBuilder();
        String initMockMethodName1 = "init()";
        initMockMethodBuilder1
                .append(TAB + TAB + "@BeforeMethod\n")
                .append(TAB + TAB + "public void ")
                .append(initMockMethodName1)
                .append(" {\n")
                .append(TAB + TAB + "initMocks(this);\n");

        initMockMethodBuilder1.append(TAB + "}\n\n");

        MethodExtInfo initMockMethod1 = new MethodExtInfo();
        initMockMethod1.setClassName("org.carpenter.collector.dto.Test");
        initMockMethod1.setUnitName(initMockMethodName1);
        initMockMethod1.setBody(initMockMethodBuilder1.toString());

        StringBuilder initMockMethodBuilder2 = new StringBuilder();
        String initMockMethodName2 = "init()";
        initMockMethodBuilder2
                .append(TAB + "@BeforeMethod\n")
                .append(TAB + "public void ")
                .append(initMockMethodName2)
                .append(" {\n")
                .append(TAB + TAB + "initMocks(this);\n");

        initMockMethodBuilder2.append(TAB + "}\n\n");

        MethodExtInfo initMockMethod2 = new MethodExtInfo();
        initMockMethod2.setClassName("org.carpenter.collector.dto.Test");
        initMockMethod2.setUnitName(initMockMethodName2);
        initMockMethod2.setBody(initMockMethodBuilder2.toString());

        Set<ClassExtInfo> set = new HashSet<>();

        set.add(initMockMethod2);
        set.add(initMockMethod1);

        assertEquals(initMockMethod2.hashCode(), initMockMethod1.hashCode());
        assertEquals(set.size(), 1);
    }

    @Test
    public void testCreateMethodSource() throws Exception {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("method.txt");

        StringBuilder bodyBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            bodyBuilder.append(line).append("\n");
        }
        String body = bodyBuilder.toString();

        MethodExtInfo methodExtInfo = new MethodExtInfo();
        methodExtInfo.setClassName("Test");
        methodExtInfo.setUnitName("testGetSortedTermConditions__2080488256()");
        methodExtInfo.setBody(body);

        MethodSource methodSource = methodExtInfo.createMethodSource();

        System.out.println(methodSource.toString());

        assertEquals(methodSource.toString(), body);
    }
}
