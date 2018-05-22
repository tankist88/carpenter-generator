package org.carpenter.generator;

import org.carpenter.core.dto.unit.ClassInfo;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.imports.ImportInfo;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.carpenter.generator.UnitClassifier.getSimilarClassInfoList;
import static org.testng.Assert.assertEquals;

public class UnitClassifierTest {
    @Test
    public void getSimilarClassInfoListTest() {
        ClassExtInfo[] importArr = {
            new ImportInfo("", "org.example.util.CommonDataProvider_2", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_6", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_10", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_1", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_5", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_9", ""),
            new ImportInfo("", "java.lang.Integer", ""),
            new ImportInfo("", "java.lang.Boolean", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_11", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_4", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_8", ""),
            new ImportInfo("", "java.util.List", ""),
            new ImportInfo("", "java.util.Date", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_3", ""),
            new ImportInfo("", "org.example.util.CommonDataProvider_7", ""),
            new ImportInfo("", "java.lang.String", ""),
            new ImportInfo("", "java.util.ArrayList", ""),
            new ImportInfo("", "java.io.Serializable", "")
        };

        List<ClassExtInfo> result = getSimilarClassInfoList(Arrays.asList(importArr));
        for(ClassInfo c : result) {
            System.out.println(c.getUnitName());
        }
        assertEquals(result.size(), importArr.length);
    }
}
