package org.carpenter.generator.command;

import org.apache.commons.io.FileUtils;
import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.field.FieldExtInfo;
import org.carpenter.generator.dto.unit.imports.ImportInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;
import org.carpenter.generator.UnitClassifier;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.carpenter.generator.TestGenerator.GENERATED_TEST_CLASS_POSTFIX;
import static org.carpenter.generator.util.GenerateUtil.createAndReturnPathName;
import static org.object2source.util.GenerationUtil.getLastClassShort;
import static org.object2source.util.GenerationUtil.getPackage;

public class CreateJavaClassesCommand extends AbstractCommand {

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
            ioex.printStackTrace();
        }
    }

    private void saveJavaClassesAndPackages() throws IOException {
        String pathname = createAndReturnPathName(props);
        String dataProviderClassPattern = props.getDataProviderClassPattern();
        for(String fullClassName : collectedTests.keySet()) {
            String className = getLastClassShort(fullClassName);
            String packageName = getPackage(fullClassName);
            String packageFileStruct = pathname + "/" + packageName.replaceAll("\\.", "/");
            FileUtils.forceMkdir(new File(packageFileStruct));
            Set<ClassExtInfo> units = collectedTests.get(fullClassName);
            List<ClassExtInfo> groupList = UnitClassifier.getSimilarClassInfoList(units);
            if(units.size() != groupList.size()) {
                throw new RuntimeException("Error while grouping units!");
            }
            StringBuilder classBuilder = new StringBuilder();
            classBuilder.append("package ").append(packageName).append(";\n\n");

            if(!fullClassName.startsWith(dataProviderClassPattern)) {
                classBuilder.append("import org.testng.annotations.Test;\n");
                classBuilder.append("import org.testng.annotations.BeforeMethod;\n\n");
                classBuilder.append("import org.mockito.ArgumentMatchers;\n");
                classBuilder.append("import org.mockito.InjectMocks;\n");
                classBuilder.append("import org.mockito.invocation.InvocationOnMock;\n");
                classBuilder.append("import org.mockito.stubbing.Answer;\n");
                classBuilder.append("import org.mockito.Spy;\n");
                classBuilder.append("import org.mockito.Mock;\n\n");
                classBuilder.append("import static org.testng.Assert.assertEquals;\n\n");
                classBuilder.append("import static org.mockito.ArgumentMatchers.*;\n");
                classBuilder.append("import static org.mockito.Mockito.*;\n");
                classBuilder.append("import static org.mockito.MockitoAnnotations.initMocks;\n\n");
            }
            for(ClassExtInfo unit : groupList) {
                if(unit instanceof ImportInfo) {
                    classBuilder.append(unit.getBody());
                }
            }
            classBuilder.append("\n");

            String postfix = fullClassName.startsWith(dataProviderClassPattern) ? "" : GENERATED_TEST_CLASS_POSTFIX;

            classBuilder.append("public class ").append(className).append(postfix).append(" {\n\n");

            for(ClassExtInfo unit : groupList) {
                if(unit instanceof FieldExtInfo) {
                    classBuilder.append(unit.getBody()).append("\n");
                }
            }
            for(ClassExtInfo unit : groupList) {
                if(unit instanceof MethodExtInfo) {
                    classBuilder.append(unit.getBody()).append("\n");
                }
            }
            classBuilder.append("}");

            File utClass = new File(packageFileStruct + "/" + className + postfix + ".java");
            FileUtils.write(utClass, classBuilder.toString());
        }
    }
}
