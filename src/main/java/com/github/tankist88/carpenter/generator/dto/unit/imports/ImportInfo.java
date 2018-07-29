package com.github.tankist88.carpenter.generator.dto.unit.imports;

import com.github.tankist88.carpenter.generator.dto.unit.AbstractUnitExtInfo;

public class ImportInfo extends AbstractUnitExtInfo implements Comparable<ImportInfo> {
    public ImportInfo() {
        super();
    }
    public ImportInfo(String className, String unitName, String body) {
        super(className, unitName, body);
    }

    @Override
    public int compareTo(ImportInfo o) {
        return this.getUnitName().compareTo(o.getUnitName());
    }
}
