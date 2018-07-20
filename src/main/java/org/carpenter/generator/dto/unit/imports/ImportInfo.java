package org.carpenter.generator.dto.unit.imports;

import org.carpenter.generator.dto.unit.AbstractUnitExtInfo;

public class ImportInfo extends AbstractUnitExtInfo implements Comparable<ImportInfo> {
    public ImportInfo() {
    }
    public ImportInfo(String className, String unitName, String body) {
        super(className, unitName, body);
    }

    @Override
    public int compareTo(ImportInfo o) {
        return this.getUnitName().compareTo(o.getUnitName());
    }
}
