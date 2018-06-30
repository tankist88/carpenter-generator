package org.carpenter.generator.command;

import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.UnitClassifier;
import org.carpenter.generator.dto.source.MethodLine;
import org.carpenter.generator.dto.source.MethodSource;
import org.carpenter.generator.dto.source.Variable;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.field.FieldExtInfo;
import org.carpenter.generator.dto.unit.imports.ImportInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.write;
import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.TestGenerator.GENERATED_TEST_CLASS_POSTFIX;
import static org.carpenter.generator.util.GenerateUtil.createAndReturnPathName;
import static org.object2source.util.GenerationUtil.*;

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
            forceMkdir(new File(packageFileStruct));
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

            classBuilder.append("import javax.annotation.Generated;\n\n");

            for(ClassExtInfo unit : groupList) {
                if(unit instanceof ImportInfo) {
                    classBuilder.append(unit.getBody());
                }
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
            for(MethodExtInfo unit : groupMethods(extractMethods(groupList))) {
                classBuilder.append(unit.getBody()).append("\n");
            }

            classBuilder.append("}");

            File utClass = new File(packageFileStruct + "/" + className + postfix + ".java");
            write(utClass, classBuilder.toString());
        }
    }

    private Set<MethodExtInfo> extractMethods(List<ClassExtInfo> groupList) {
        Set<MethodExtInfo> methods = new HashSet<>();
        for(ClassExtInfo unit : groupList) {
            if(unit instanceof MethodExtInfo) {
                methods.add((MethodExtInfo) unit);
            }
        }
        return methods;
    }

    private Set<MethodExtInfo> groupMethods(Set<MethodExtInfo> allMethods) {
        Set<MethodSource> commonMethods = new HashSet<>();
        for (MethodExtInfo extInfo : allMethods) {
            if (extInfo.hasMultipleMock()) continue;
            String commonName = "test" + upFirst(extInfo.getUnitName());
            MethodSource methodSource = extInfo.createMethodSource();
            String definition = methodSource.getTestMethodDefinition();
            definition = definition.replace(extInfo.getUnitName(), commonName);

            StringBuilder argDefBuilder = new StringBuilder();
            argDefBuilder.append("(");
            int i = 0;
            for (MethodLine line : methodSource.getLines()) {
                for (Variable var : line.getVariables()) {
                    String argDef = "arg" + i;
                    var.setValue(argDef);
                    argDefBuilder.append(argDef).append(", ");
                    i++;
                }
            }
            argDefBuilder.delete(argDefBuilder.length() - 1, argDefBuilder.length() - 1);
            argDefBuilder.append(")");
            definition = definition.replace("()", argDefBuilder.toString());
            methodSource.setTestMethodDefinition(definition);
            commonMethods.add(methodSource);
        }
        for (MethodExtInfo extInfo : allMethods) {
            MethodSource methodSource = extInfo.createMethodSource();
            if (commonMethods.contains(methodSource)) {
                StringBuilder argBuilder = new StringBuilder();
                argBuilder.append("(");
                for (MethodLine line : methodSource.getLines()) {
                    for (Variable var : line.getVariables()) {
                        argBuilder.append(var.getValue()).append(", ");
                    }
                }
                argBuilder.delete(argBuilder.length() - 1, argBuilder.length() - 1);
                argBuilder.append(")");
                String commonName = "test" + upFirst(extInfo.getUnitName());
                String newContent = TAB + TAB + commonName + argBuilder.toString() + ";\n";
                MethodLine newLine = new MethodLine(newContent);
                methodSource.getLines().clear();
                methodSource.getLines().add(newLine);
                extInfo.setBody(methodSource.toString());
            }
        }
        for (MethodSource methodSource : commonMethods) {
            allMethods.add(methodSource.createMethodExtInfo());
        }
        return allMethods;
    }
}
