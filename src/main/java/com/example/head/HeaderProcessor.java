package com.example.head;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author 19146
 * @since 2024/8/17 20:29
 */
@SupportedAnnotationTypes({"com.example.Header"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class HeaderProcessor extends AbstractProcessor {

    Messager messager;

    JavacTrees trees;

    TreeMaker treeMaker;

    Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(Header.class);
        set.stream()
                .findFirst()
                .ifPresent(e -> addImport(e, "com.example.head", "AnotherHeader"));
        for (Element element : set) {
            JCTree tree = trees.getTree(element);
            tree.accept(new TreeTranslator() {
                @Override
                public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                    if (isValidField(jcVariableDecl)) {
                        JCTree.JCExpression arg = makeArg("value", "anotherHeader test");
                        JCTree.JCAnnotation jcAnnotation = makeAnnotation("com.example.head.AnotherHeader", List.of(arg));
                        jcVariableDecl.mods.annotations = jcVariableDecl.mods.annotations.append(jcAnnotation);
                    }
                    super.visitVarDef(jcVariableDecl);
                }
            });
        }
        return true;
    }

    /**
     * 判断是否是合法的字段
     *
     * @param jcTree 语法树节点
     * @return 是否是合法字段
     */
    private static boolean isValidField(JCTree jcTree) {
        if (jcTree.getKind().equals(Tree.Kind.VARIABLE)) {
            JCTree.JCVariableDecl jcVariable = (JCTree.JCVariableDecl) jcTree;

            Set<Modifier> flagSets = jcVariable.mods.getFlags();
            return (!flagSets.contains(Modifier.STATIC)
                    && !flagSets.contains(Modifier.FINAL));
        }
        return false;
    }

    public JCTree.JCExpression makeArg(String key, String value) {
        return treeMaker.Assign(treeMaker.Ident(names.fromString(key)), treeMaker.Literal(value));
    }

    private JCTree.JCAnnotation makeAnnotation(String annotationName, List<JCTree.JCExpression> args) {
        JCTree.JCExpression expression = chainDots(annotationName.split("\\."));
        messager.printMessage(Diagnostic.Kind.NOTE,"add annotation:" + expression.toString());
        return treeMaker.Annotation(expression, args);
    }

    public JCTree.JCExpression chainDots(String... elems) {
        JCTree.JCExpression e = null;
        for (String elem : elems) {
            e = e == null ? treeMaker.Ident(names.fromString(elem)) : treeMaker.Select(e, names.fromString(elem));
        }
        return e;
    }


    private void addImport(Element element, String packageName, String className) {
        TreePath treePath = trees.getPath(element);
        JCTree.JCCompilationUnit jccu = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();
        java.util.List<JCTree> trees = new ArrayList<>();
        trees.addAll(jccu.defs);
        java.util.List<JCTree> sourceImportList = new ArrayList<>();
        trees.forEach(e -> {
            if (e.getKind().equals(Tree.Kind.IMPORT)) {
                sourceImportList.add(e);
            }
        });
        JCTree.JCImport needImportList = buildImport(packageName, className);
        trees.add(needImportList);
        jccu.defs = List.from(trees);
    }

    private JCTree.JCImport buildImport(String packageName, String className) {
        JCTree.JCIdent ident = treeMaker.Ident(names.fromString(packageName));
        JCTree.JCImport jcImport = treeMaker.Import(treeMaker.Select(ident, names.fromString(className)), false);
        messager.printMessage(Diagnostic.Kind.NOTE,"add Import:" + jcImport.toString());
        return jcImport;
    }
}