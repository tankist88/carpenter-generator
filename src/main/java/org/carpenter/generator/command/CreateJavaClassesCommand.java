package org.carpenter.generator.command;

import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.field.FieldExtInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.write;
import static org.carpenter.generator.TestGenerator.GENERATED_TEST_CLASS_POSTFIX;
import static org.carpenter.generator.util.GenerateUtil.createAndReturnPathName;
import static org.carpenter.generator.util.codeformat.ImportsFormatUtil.organizeImports;
import static org.carpenter.generator.util.codeformat.MethodsFormatUtil.createDataProviders;
import static org.carpenter.generator.util.codeformat.MethodsFormatUtil.extractMethods;
import static org.object2source.util.GenerationUtil.getLastClassShort;
import static org.object2source.util.GenerationUtil.getPackage;

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
            String className = getLastClassShort(fullClassName);
            String packageName = getPackage(fullClassName);
            String packageFileStruct = pathname + "/" + packageName.replaceAll("\\.", "/");
            forceMkdir(new File(packageFileStruct));
            Set<ClassExtInfo> units = collectedTests.get(fullClassName);
            List<ClassExtInfo> groupList = new ArrayList<>(units);
            Collections.sort(groupList, new Comparator<ClassExtInfo>() {
                @Override
                public int compare(ClassExtInfo o1, ClassExtInfo o2) {
                    return o1.getUnitName().compareTo(o2.getUnitName());
                }
            });
            if(units.size() != groupList.size()) {
                throw new RuntimeException("Error while grouping units!");
            }
            StringBuilder classBuilder = new StringBuilder();
            classBuilder.append("package ").append(packageName).append(";\n\n");

            if(!fullClassName.startsWith(dataProviderClassPattern)) {
                classBuilder.append("import org.testng.annotations.*;\n\n");
                classBuilder.append("import org.mockito.ArgumentMatchers;\n");
                classBuilder.append("import org.mockito.InjectMocks;\n");
                classBuilder.append("import org.mockito.invocation.InvocationOnMock;\n");
                classBuilder.append("import org.mockito.stubbing.Answer;\n");
                classBuilder.append("import org.mockito.Spy;\n");
                classBuilder.append("import org.mockito.Mock;\n\n");
                classBuilder.append("import static org.testng.Assert.*;\n\n");
                classBuilder.append("import static org.mockito.ArgumentMatchers.*;\n");
                classBuilder.append("import static org.mockito.Mockito.*;\n");
                classBuilder.append("import static org.mockito.MockitoAnnotations.initMocks;\n\n");
            }

            classBuilder.append("import javax.annotation.Generated;\n\n");

            for(String unit : organizeImports(groupList)) {
                classBuilder.append(unit);
            }
            classBuilder.append("\n");

            String postfix = fullClassName.startsWith(dataProviderClassPattern) ? "" : GENERATED_TEST_CLASS_POSTFIX;

            classBuilder.append("@Generated(value = \"org.carpenter.generator.TestGenerator\")\n");
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
}
