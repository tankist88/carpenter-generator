package com.github.tankist88.carpenter.generator.util.codeformat;

import com.github.tankist88.carpenter.generator.dto.PackageTree;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ImportsFormatUtilTest {
    @Test
    public void organizeImportsTest() {
        List<String> imports = new ArrayList<>();

        imports.add("import java.math.BigInteger;");
        imports.add("import java.sql.Timestamp;");
        imports.add("import java.util.ArrayList;");
        imports.add("import java.util.Date;");
        imports.add("import java.util.GregorianCalendar;");
        imports.add("import java.util.HashMap;");
        imports.add("import java.util.Map;");
        imports.add("import java.util.TimeZone;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.yyy.SPDDS.TR;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.yyy.SPDDS.TR;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.vvv.rrr.PC;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.vvv.rrr.SPDP;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.vvv.rrr.TC;");
        imports.add("import org.aaa.bbb.eee.fff.classifier.dto.DCD;");

        List<String> controlList = new ArrayList<>();

        controlList.add("import java.math.BigInteger;");
        controlList.add("import java.sql.Timestamp;");
        controlList.add("import java.util.*;");
        controlList.add("import org.aaa.bbb.ppp.zzz.qqq.yyy.SPDDS.TR;");
        controlList.add("import org.aaa.bbb.ppp.zzz.qqq.vvv.rrr.*;");
        controlList.add("import org.aaa.bbb.eee.fff.classifier.dto.DCD;");

        PackageTree tree = new PackageTree();
        for (String im : imports) {
            tree.addPackage(im);
        }
        List<String> result = tree.cutOne();
        assertEquals(controlList.size(), result.size());
        for (String r : result) {
            assertTrue(controlList.contains(r));
        }
    }

    @Test
    public void sortImportsTest() {
        List<String> imports = new ArrayList<>();

        imports.add("import java.math.BigDecimal;");
        imports.add("import java.util.ArrayList;");
        imports.add("import java.util.GregorianCalendar;");
        imports.add("import org.aaa.bbb.ppp.conv.calc.conv.CR;");
        imports.add("import org.aaa.bbb.ppp.conv.vvv.CI;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.uuu.pl.model.CDD;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.vvv.rrr.CC;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.vvv.rrr.PC;");
        imports.add("import org.aaa.bbb.ppp.zzz.qqq.vvv.request.SOOR;");
        imports.add("import org.aaa.bbb.eee.fff.uuu.util.DDIPC;");
        imports.add("import org.aaa.bbb.eee.fff.uuu.util.DDPPC;");
        imports.add("import org.aaa.bbb.eee.fff.uuu.util.DDWPC;");
        imports.add("import org.aaa.bbb.eee.fff.uuu.vvv.BOAI;");
        imports.add("import org.aaa.bbb.eee.fff.uuu.vvv.DST;");
        imports.add("import org.aaa.bbb.eee.fff.uuu.vvv.SBODRM;");
        imports.add("import org.aaa.bbb.eee.fff.vvv.SODRA;");
        imports.add("import org.aaa.bbb.eee.fff.vvv.SODRP;");
        imports.add("import org.aaa.bbb.eee.fff.vvv.SODRS;");
        imports.add("import org.aaa.bbb.eee.fff.uuu.vvv.TT;");
        imports.add("import org.aaa.www.xxx.mmm.ooo.AE;");

        PackageTree tree = new PackageTree();
        for (String im : imports) {
            tree.addPackage(im);
        }
        List<String> result = tree.toList();
        assertEquals(imports.size(), result.size());
        for (String r : result) {
            assertTrue(imports.contains(r));
        }
    }
}
