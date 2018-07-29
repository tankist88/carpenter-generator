package com.github.tankist88.carpenter.generator.command;

import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.TestGenerator;
import com.github.tankist88.carpenter.generator.dto.unit.ClassExtInfo;
import com.github.tankist88.carpenter.generator.dto.unit.field.FieldExtInfo;
import com.github.tankist88.carpenter.generator.dto.unit.method.MethodExtInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.github.tankist88.carpenter.generator.TestGenerator.GENERATED_TEST_CLASS_POSTFIX;
import static com.github.tankist88.carpenter.generator.util.GenerateUtil.createAndReturnPathName;
import static com.github.tankist88.carpenter.generator.util.TypeHelper.createImportInfo;
import static com.github.tankist88.carpenter.generator.util.codeformat.ImportsFormatUtil.organizeImports;
import static com.github.tankist88.carpenter.generator.util.codeformat.MethodsFormatUtil.createDataProviders;
import static com.github.tankist88.carpenter.generator.util.codeformat.MethodsFormatUtil.extractMethods;
import static com.github.tankist88.object2source.util.GenerationUtil.getLastClassShort;
import static com.github.tankist88.object2source.util.GenerationUtil.getPackage;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.write;

public class CreateJavaClassesCommand extends AbstractCommand {
    public static final String PROVIDER_POSTFIX = "_Provider";
    public static final String DATA_PROVIDER_ANNOTATION = "@DataProvider";
    public static final String DATA_PROVIDER_PARAMETER = "dataProvider";

    private GenerationProperties props;
    private Map<String, Set<ClassExtInfo>> collectedTests;

    public CreateJavaClassesCommand(Map<String, Set<ClassExtInfo>> collectedTests) {
        this.props = GenerationPropertiesFactory.loadProps();
        this.collectedTests = collectedTests;
    }

    @Override
    public void execute() {
        try {
            saveJavaClassesAndPackages();
        } catch (IOException ioex) {
            throw new IllegalStateException(ioex);
        }
    }

    private void saveJavaClassesAndPackages() throws IOException {
        String pathname = createAndReturnPathName(props);
        String dataProviderClassPattern = props.getDataProviderClassPattern();
        for(String fullClassName : collectedTests.keySet()) {
            if (!allowedPackageForGeneration(fullClassName) || classNotModified(collectedTests.get(fullClassName))) continue;

            String className = getLastClassShort(fullClassName);
            String packageName = getPackage(fullClassName);
            String packageFileStruct = pathname + "/" + packageName.replaceAll("\\.", "/");
            forceMkdir(new File(packageFileStruct));

            Set<ClassExtInfo> units = collectedTests.get(fullClassName);

            addDefaultImports(units, fullClassName, dataProviderClassPattern);

            List<ClassExtInfo> groupList = new ArrayList<>(units);
            Collections.sort(groupList, new Comparator<ClassExtInfo>() {
                @Override
                public int compare(ClassExtInfo o1, ClassExtInfo o2) {
                    return o1.getUnitName().compareTo(o2.getUnitName());
                }
            });

            StringBuilder classBuilder = new StringBuilder();
            classBuilder.append("package ").append(packageName).append(";\n\n");

            for(String unit : organizeImports(groupList)) {
                classBuilder.append(unit);
            }
            classBuilder.append("\n");

            String postfix = fullClassName.startsWith(dataProviderClassPattern) ? "" : GENERATED_TEST_CLASS_POSTFIX;

            classBuilder.append("@Generated(value = \"").append(TestGenerator.class.getName()).append("\")\n");
            classBuilder.append("public class ").append(className).append(postfix).append(" {\n\n");

            for(ClassExtInfo unit : groupList) {
                if(unit instanceof FieldExtInfo) {
                    classBuilder.append(unit.getBody()).append("\n");
                }
            }
            for(MethodExtInfo unit : createDataProviders(extractMethods(groupList))) {
                classBuilder.append(unit.getBody()).append("\n");
            }

            classBuilder.append("}");

            File utClass = new File(packageFileStruct + "/" + className + postfix + ".java");
            write(utClass, classBuilder.toString());
        }
    }

    private void addDefaultImports(Set<ClassExtInfo> units, String fullClassName, String dataProviderClassPattern) {
        if(!fullClassName.startsWith(dataProviderClassPattern)) {
            units.add(createImportInfo("org.testng.annotations.*", fullClassName));
            units.add(createImportInfo("org.mockito.ArgumentMatchers", fullClassName));
            units.add(createImportInfo("org.mockito.InjectMocks", fullClassName));
            units.add(createImportInfo("org.mockito.invocation.InvocationOnMock", fullClassName));
            units.add(createImportInfo("org.mockito.stubbing.Answer", fullClassName));
            units.add(createImportInfo("org.mockito.Spy", fullClassName));
            units.add(createImportInfo("org.mockito.Mock", fullClassName));
            units.add(createImportInfo("org.testng.Assert.*", fullClassName, true));
            units.add(createImportInfo("org.mockito.ArgumentMatchers.*", fullClassName, true));
            units.add(createImportInfo("org.mockito.Mockito.*", fullClassName, true));
            units.add(createImportInfo("org.mockito.MockitoAnnotations.initMocks", fullClassName, true));
        }
        units.add(createImportInfo("javax.annotation.Generated", fullClassName));
    }

    private boolean allowedPackageForGeneration(String className) {
        if(className == null) return false;
        String dataProviderClassPattern = props.getDataProviderClassPattern();
        for (String p : props.getAllowedPackagesForTests()) {
            if(className.startsWith(p) || className.startsWith(dataProviderClassPattern)) return true;
        }
        return false;
    }

    private boolean classNotModified(Set<ClassExtInfo> units) {
        if (units == null) return true;
        for (ClassExtInfo unit : units) {
            if (unit.newUnit()) return false;
        }
        return true;
    }
}
