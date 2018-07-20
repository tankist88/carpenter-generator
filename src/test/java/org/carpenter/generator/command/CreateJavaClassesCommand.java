package org.carpenter.generator.command;

import org.carpenter.generator.dto.PackageTree;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CreateJavaClassesCommand {
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
        imports.add("import ru.sbrf.bh.banking.product.deposit.da.SbrfProductDepositDaService.TermRange;");
        imports.add("import ru.sbrf.bh.banking.product.deposit.da.SbrfProductDepositDaService.TermRange;");
        imports.add("import ru.sbrf.bh.banking.product.deposit.vo.product.ProductCondition;");
        imports.add("import ru.sbrf.bh.banking.product.deposit.vo.product.SbrfPersonDepositProduct;");
        imports.add("import ru.sbrf.bh.banking.product.deposit.vo.product.TermCondition;");
        imports.add("import ru.sbrf.bh.sys.deposit.classifier.dto.DepositClassifierDto;");

        List<String> controlList = new ArrayList<>();

        controlList.add("import java.math.BigInteger;");
        controlList.add("import java.sql.Timestamp;");
        controlList.add("import java.util.*;");
        controlList.add("import ru.sbrf.bh.banking.product.deposit.da.SbrfProductDepositDaService.TermRange;");
        controlList.add("import ru.sbrf.bh.banking.product.deposit.vo.product.*;");
        controlList.add("import ru.sbrf.bh.sys.deposit.classifier.dto.DepositClassifierDto;");

        PackageTree tree = new PackageTree();
        for (String im : imports) {
            tree.addPackage(im);
        }
        List<String> result = tree.cutOne();
        assertEquals(controlList.size(), result.size());
        for (String r : result) {
            System.out.println(r);
            assertTrue(controlList.contains(r));
        }
    }

    @Test
    public void sortImportsTest() {
        List<String> imports = new ArrayList<>();

        imports.add("import java.math.BigDecimal;");
        imports.add("import java.util.ArrayList;");
        imports.add("import java.util.GregorianCalendar;");
        imports.add("import ru.sbrf.bh.banking.conversion.calc.conversion.ConversionRequest;");
        imports.add("import ru.sbrf.bh.banking.conversion.vo.ConversionInfo;");
        imports.add("import ru.sbrf.bh.banking.product.deposit.branchdeposit.pl.model.ClosingDepositData;");
        imports.add("import ru.sbrf.bh.banking.product.deposit.vo.product.CurrencyCondition;");
        imports.add("import ru.sbrf.bh.banking.product.deposit.vo.product.ProductCondition;");
        imports.add("import ru.sbrf.bh.banking.product.deposit.vo.request.SbrfOpenOperationRequest;");
        imports.add("import ru.sbrf.bh.sys.deposit.branchdeposit.util.DepositDtoIncomePriorityComparator;");
        imports.add("import ru.sbrf.bh.sys.deposit.branchdeposit.util.DepositDtoProlongationPriorityComparator;");
        imports.add("import ru.sbrf.bh.sys.deposit.branchdeposit.util.DepositDtoWithdrawPriorityComparator;");
        imports.add("import ru.sbrf.bh.sys.deposit.branchdeposit.vo.BranchOperationAddInfo;");
        imports.add("import ru.sbrf.bh.sys.deposit.branchdeposit.vo.DepositSelectedType;");
        imports.add("import ru.sbrf.bh.sys.deposit.branchdeposit.vo.SbrfBranchOperationDepositRequestManager;");
        imports.add("import ru.sbrf.bh.sys.deposit.vo.SbrfOperationDepositRequestAgreement;");
        imports.add("import ru.sbrf.bh.sys.deposit.vo.SbrfOperationDepositRequestPerson;");
        imports.add("import ru.sbrf.bh.sys.deposit.vo.SbrfOperationDepositRequestSession;");
        imports.add("import ru.sbrf.bh.sys.deposit.branchdeposit.vo.TurnoverType;");
        imports.add("import ru.sbrf.ufs.platform.audit.model.AuditEvent;");

        PackageTree tree = new PackageTree();
        for (String im : imports) {
            tree.addPackage(im);
        }
        List<String> result = tree.toList();
        assertEquals(imports.size(), result.size());
        for (String r : result) {
            System.out.println(r);
            assertTrue(imports.contains(r));
        }
    }
}
