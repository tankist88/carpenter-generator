package com.github.tankist88.carpenter.generator.util.codeformat;

import com.github.tankist88.carpenter.generator.dto.PackageTree;
import com.github.tankist88.carpenter.generator.dto.unit.ClassExtInfo;
import com.github.tankist88.carpenter.generator.dto.unit.imports.ImportInfo;

import java.util.ArrayList;
import java.util.List;

public class ImportsFormatUtil {
    public static List<String> organizeImports(List<ClassExtInfo> groupList) {
        PackageTree tree = new PackageTree();
        List<ImportInfo> simpleImports = new ArrayList<>();
        List<ImportInfo> staticImports = new ArrayList<>();
        for (ClassExtInfo unit : groupList) {
            if (unit instanceof ImportInfo && !unit.getBody().contains("import static")) {
                simpleImports.add((ImportInfo) unit);
            } else if (unit instanceof ImportInfo) {
                staticImports.add((ImportInfo) unit);
            }
        }
        for (ImportInfo importInfo : simpleImports) {
            tree.addPackage(importInfo.getBody());
        }
        List<String> result = new ArrayList<>();
        for (String unit : tree.cutOne()) {
            result.add(unit.replace("\n", "").replace("\r", "") + "\n");
        }
        if (simpleImports.size() > 0) {
            result.add("\n");
        }
        for (ImportInfo unit : staticImports) {
            result.add(unit.getBody());
        }
        return result;
    }
}
