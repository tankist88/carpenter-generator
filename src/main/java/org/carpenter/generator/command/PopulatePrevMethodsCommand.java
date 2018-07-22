package org.carpenter.generator.command;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.field.FieldExtInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;
import org.carpenter.generator.util.ConvertUtil;
import org.object2source.dto.ProviderInfo;
import org.object2source.util.AssigmentUtil;

import java.io.File;
import java.util.*;

import static org.carpenter.core.property.AbstractGenerationProperties.COMMON_UTIL_POSTFIX;
import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.TestGenerator.*;
import static org.carpenter.generator.util.GenerateUtil.getFileList;
import static org.carpenter.generator.util.TypeHelper.createImportInfo;
import static org.object2source.util.GenerationUtil.getClassShort;

public class PopulatePrevMethodsCommand extends AbstractReturnClassInfoCommand<ClassExtInfo> {

    private GenerationProperties props;

    private Map<String, Set<String>> providerSignatureMap;
    private Map<String, Set<ClassExtInfo>> existsMethodsMap;

    public PopulatePrevMethodsCommand(Map<String, Set<String>> providerSignatureMap) {
        this.props = GenerationPropertiesFactory.loadProps();
        this.providerSignatureMap = providerSignatureMap;
    }

    @Override
    public void execute() {
        super.execute();
        populateMethodsMaps();
    }

    @Override
    public List<ClassExtInfo> returnResult() {
        List<ClassExtInfo> result = new ArrayList<>();
        for(Set<ClassExtInfo> set : existsMethodsMap.values()) {
            result.addAll(set);
        }
        return result;
    }

    private void populateMethodsMaps() {
        try {
            existsMethodsMap = new HashMap<>();
            providerSignatureMap = new HashMap<>();
            String dataProviderClassPattern = props.getDataProviderClassPattern();
            String commonClassName = dataProviderClassPattern + COMMON_UTIL_POSTFIX;
            Set<ClassExtInfo> commonMethods = new HashSet<>();
            for (ProviderInfo p : AssigmentUtil.getCommonMethods(TAB)) {
                commonMethods.add(ConvertUtil.toMethodExtInfo(commonClassName, p));
            }
            existsMethodsMap.put(commonClassName, commonMethods);
            for (File f : getFileList(new File(props.getUtGenDir()), "java")) {
                boolean allowedFiles = (f.getName().endsWith(GENERATED_TEST_CLASS_POSTFIX + ".java") ||
                        f.getName().startsWith(getClassShort(dataProviderClassPattern))) &&
                        !f.getName().endsWith(COMMON_UTIL_POSTFIX+ ".java");
                if (!allowedFiles) continue;
                CompilationUnit compilationUnit = JavaParser.parse(f);
                TypeDeclaration type = compilationUnit.getTypes().get(0);
                String fullClassName = compilationUnit.getPackage().getName() + "." + type.getName();
                if (fullClassName.contains(GENERATED_TEST_CLASS_POSTFIX)) {
                    fullClassName = fullClassName.substring(0, fullClassName.indexOf(GENERATED_TEST_CLASS_POSTFIX));
                }
                Set<ClassExtInfo> units = existsMethodsMap.get(fullClassName);
                if (units == null) {
                    units = new HashSet<>();
                    existsMethodsMap.put(fullClassName, units);
                }

                for (ImportDeclaration imp : compilationUnit.getImports()) {
                    String name = imp.getName().toString();
                    String impClass = imp.isAsterisk() ? name + ".*" : name;
                    units.add(createImportInfo(impClass, fullClassName, imp.isStatic()));
                }

                for (Node node : type.getChildrenNodes()) {
                    if (node instanceof FieldDeclaration) {
                        FieldExtInfo field = new FieldExtInfo();
                        field.setClassName(fullClassName);
                        for (Node v : node.getChildrenNodes()) {
                            if (v instanceof VariableDeclarator) {
                                field.setUnitName(v.toString());
                                break;
                            }
                        }
                        field.setBody(TAB + node.toString()
                                .replace("\r\n", "\n")
                                .replace("\n",  "\n" + TAB) + "\n");
                        units.add(field);
                    } else if (node instanceof MethodDeclaration) {
                        MethodExtInfo method = new MethodExtInfo();
                        method.setClassName(fullClassName);
                        StringBuilder paramStrBuilder = new StringBuilder();
                        Iterator<Parameter> iterator = ((MethodDeclaration) node).getParameters().iterator();
                        while (iterator.hasNext()) {
                            paramStrBuilder.append(iterator.next().toString());
                            if (iterator.hasNext()) paramStrBuilder.append(", ");
                        }
                        method.setUnitName(((MethodDeclaration) node).getName() + "(" + paramStrBuilder.toString() + ")");

                        String body = TAB + node.toString()
                                .replace("\r\n", "\n")
                                .replace("\n", "\n" + TAB) + "\n";
                        // TODO Fix format for multi row expressions
                        // START temp code
                        if (method.isArrayProvider()) {
                            body = body.replace("= { ", "= {\n" + TAB + TAB + TAB);
                            body = body.replace("(), ", "(),\n" + TAB + TAB + TAB);
                            body = body.replace("() };", "()\n" + TAB + TAB + "};");
                        } else if (method.isDataProvider()) {
                            body = body.replace("[][] { ", "[][] {\n" + TAB + TAB + TAB);
                            body = body.replace("}, {", "}, \n" + TAB + TAB + TAB + "{");
                            body = body.replace("} };", "}\n" + TAB + TAB + "};");
                        }
                        // END temp code
                        method.setBody(body);
                        units.add(method);
                        if (fullClassName.startsWith(dataProviderClassPattern)) {
                            Set<String> providersSignatures = providerSignatureMap.get(fullClassName);
                            if (providersSignatures == null) {
                                providersSignatures = new HashSet<>();
                                providerSignatureMap.put(fullClassName, providersSignatures);
                            }
                            providersSignatures.add(method.getUnitName());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
