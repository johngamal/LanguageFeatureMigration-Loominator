package com.langFeautreMigration.projectCode;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


//LoomMigrationEngine is the core of this project... it is what handles the AST Manipulation and code migration.
public class LoomMigrationEngine {
    //Mode enum to keep track of which mode is currently working.
    private enum Mode{
        All,
        FileByFile,
//        EditByEdit
    }
    private static boolean edited = false; //checks if the file was edited
    private static int[] rule = new int[5], curRule = new int[5]; // checks rule usage for statistics
    private static int filescnt = 0; // count the number of edited files.
    private static Mode mode;   // the operating mode.

    //constructor just needs the operating mode. picks correct enum value.
    public LoomMigrationEngine(int p_mode) {
        switch(p_mode){
            case 1:
                mode = Mode.All;
                break;
            case 2:
                mode = Mode.FileByFile;
                break;
//            case 3:
//                mode = Mode.EditByEdit;
//                break;
            default:
                System.out.println("Incorrect mode selected please select a mode between 1 and 2.");
                throw new RuntimeException("Incorrect mode selected");
        }

    }

    public void handle(File file) throws FileNotFoundException {
        try{
            //create an AST from the given file handle
            CompilationUnit cu = StaticJavaParser.parse(file);
            String before = cu.toString(); //used on diff.
            edited = false;

            //starts creating tree visitor objects to apply each of the rules.
            ModifierVisitor<Void> methodNameVisitor = new FirstPattern();
            methodNameVisitor.visit(cu, null);
            methodNameVisitor = new FifthPattern();
            methodNameVisitor.visit(cu, null);
            methodNameVisitor = new SecondPattern();
            methodNameVisitor.visit(cu, null);
            methodNameVisitor = new ThirdPattern();
            methodNameVisitor.visit(cu, null);
            methodNameVisitor = new FourthPattern();
            methodNameVisitor.visit(cu, null);

            //checks if the file is edited and if the user input is needed depending on the mode.
            if(edited
            && (mode != Mode.FileByFile
            || isOk(before, cu.toString()))) {
                filescnt++;
                for(int i=0; i<5; i++)rule[i] += curRule[i];
                PrintWriter out = new PrintWriter(file.getAbsolutePath());
                out.println(cu.toString());
                out.close();
                String after = cu.toString();

                //uses an open source library created by google to find diff and print it to the screen.
                diff_match_patch dmp = new diff_match_patch();
                LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(before, after);
                dmp.diff_cleanupSemantic(diff);
                diff.forEach(x -> {
                    if (x.operation != diff_match_patch.Operation.EQUAL) System.out.println(x);
                });
            }
            else{
                System.out.println("No Edits here!");
            }
        }catch(Exception e){
            //This is sometimes triggered because of a limitation on the javaparser library itself.
            System.out.println(e.toString());
            System.out.println("A problem happened with this file. Skipping it.");
        }

    }

    private static boolean isOk(String a, String b){
        //asks user for input on a certain edit.
        System.out.println("Edits found in file");
        System.out.println("File before edits: ");
        System.out.println(a);
        System.out.println("File after edits: ");
        System.out.println(b);
        System.out.println("diff results: ");


        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(a, b);
        dmp.diff_cleanupSemantic(diff);
        diff.forEach(x -> {
            if (x.operation != diff_match_patch.Operation.EQUAL) System.out.println(x);
        });

        System.out.println("Do you accept these edits? \n(Y) yes(Default)\n(N) no");
        while(true){
            Scanner sc = new Scanner(System.in);
            char read = sc.next().charAt(0);
            if(read == 'Y')
                return true;
            if(read == 'N')
                return false;
            System.out.println("I didn't understand that. try again: ");
        }

    }

    private static class FirstPattern extends ModifierVisitor<Void> {
        @Override
        public Expression visit(ObjectCreationExpr n, Void arg) {
            super.visit(n, arg);

            //checks all object creations of type thread
            if(n.getTypeAsString().equals("Thread")){
                edited = true;
                curRule[0]++;
                NodeList<Expression> arguments = n.getArguments();
                //corner case handling if no executable is specified
                if(arguments.size() == 0){
                    JavaParser javaParser = new JavaParser();
                    Expression newStmt = javaParser.parseExpression("Thread.ofVirtual.unstarted(() -> {})").getResult().get();

                    MethodCallExpr dummy = (MethodCallExpr) newStmt;
                    return dummy;
                }
                //if an executable is specified we just more it
                else if(arguments.size() == 1) {
                    JavaParser javaParser = new JavaParser();
                    Expression newStmt = javaParser.parseExpression("Thread.ofVirtual().unstarted()").getResult().get();

                    MethodCallExpr dummy = (MethodCallExpr) newStmt;
                    dummy.setArguments(arguments);
                    Node parent = n.getParentNode().get();
                    parent.replace(n, newStmt);
                    return dummy;
                }
                //if an executable and a name are specified we also more them accordingly
                else if(arguments.size() == 2){
                    JavaParser javaParser = new JavaParser();
                    Expression newStmtHalf = javaParser.parseExpression("Thread.ofVirtual().name()").getResult().get();

                    NodeList<Expression> exec = new NodeList<>(), name = new NodeList<>();
                    exec.add(arguments.get(0));
                    name.add(arguments.get(1));
                    MethodCallExpr dummy = (MethodCallExpr) newStmtHalf;
                    MethodCallExpr newStmt = new MethodCallExpr();
                    newStmt.setName("unstarted");
                    dummy.setArguments(name);
                    newStmt.setScope(dummy);
                    newStmt.setArguments(exec);
                    Node parent = n.getParentNode().get();
                    parent.replace(n, newStmt);
                    return newStmt;
                }
            }
            return n;
        }
    }

    private static class SecondPattern extends ModifierVisitor<Void> {

        @Override
        public SimpleName visit(SimpleName n, Void arg) {
            super.visit(n, arg);

            //checks for all simple names titled "ofPlatform" and change them to "ofVirtual"
            if(n.getIdentifier().equals("ofPlatform")){
                curRule[1]++;
                edited = true;
                n.setIdentifier("ofVirtual");
                return n;
            }
            return n;
        }
    }

    private static class ThirdPattern extends ModifierVisitor<Void> {

        @Override
        public MethodCallExpr visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);

            int maxParams = -1;
            //first identify all the types of method calls we will be changing for this rule.
            String[] applicableFunctions= {"newThreadPerTaskExecutor", "newSingleThreadExecutor", "newCachedThreadPool", "newSingleThreadScheduledExecutor"};
            String[] applicableFunctionsExtension = {"newFixedThreadPool", "newScheduledThreadPool"};

            //objects in the first list have at most 1 parameter and objects of the second list have at most 2 parameters
            if(Arrays.stream(applicableFunctions).anyMatch(n.getNameAsString()::equals))
                maxParams = 1;
            if(Arrays.stream(applicableFunctionsExtension).anyMatch(n.getNameAsString()::equals))
                maxParams = 2;

            //we check if the object at hand is in one of the lists and it can get a factory added.
            if(maxParams == -1 || (n.getArguments().size() >= maxParams && !n.getNameAsString().equals("newThreadPerTaskExecutor")))
                return n;

            edited = true;
            curRule[2]++;
            final String[] virtualFactoryName = {""};
            Node blockFinder = n.getParentNode().get();
            //the decleration can exist in one of two places either in the middle of the code or as a member variable of a class.
            //we iterate upwards in the AST until we find the parent node of either a block statement or a class delclerarion.
            while(blockFinder.getClass() != BlockStmt.class && blockFinder.getClass() != ClassOrInterfaceDeclaration.class)blockFinder = blockFinder.getParentNode().get();
            if(blockFinder.getClass() == BlockStmt.class){
                //in case it was a block... we iterate over all statements before the current statement to check if a virtual factory has been created.
                BlockStmt block = (BlockStmt) blockFinder;
                NodeList<Statement> codeInBlock = block.getStatements();
                AtomicBoolean beforeCur = new AtomicBoolean(true);
                Node stmtNode = n.getParentNode().get();
                while(stmtNode.getClass() != ExpressionStmt.class)stmtNode = stmtNode.getParentNode().get();
                Statement stmt = (Statement) stmtNode;
                codeInBlock.forEach((x -> {
                    if(!virtualFactoryName[0].equals("") || !beforeCur.get())return;
                    if(x.equals(stmt)){
                        beforeCur.set(false);
                        return;
                    }
                    if(x.getChildNodes().get(0).getClass() == VariableDeclarationExpr.class){
                        VariableDeclarationExpr decl = (VariableDeclarationExpr) x.getChildNodes().get(0);

                        if(decl.getElementType().asString().equals("ThreadFactory")){
                            decl.getVariables().forEach(var -> {
                                if(var.getInitializer().get().toString().equals("Thread.ofVirtual().factory()")){
                                    virtualFactoryName[0] = decl.getVariables().get(0).getName().asString();
                                }
                            });

                        }
                    }
                }));

                JavaParser javaParser = new JavaParser();
                //if we don't find one we create one called "__Dummy_Virtual_Factory__"
                if(virtualFactoryName[0].equals("")){
                    virtualFactoryName[0] = "__Dummy_Virtual_Factory__";
                    codeInBlock.add(0, javaParser.parseStatement("ThreadFactory __Dummy_Virtual_Factory__ = Thread.ofVirtual().factory();").getResult().get());

                    n.tryAddImportToParentCompilationUnit(ThreadFactory.class);
                    block.setStatements(codeInBlock);
                }
                //then we use the factory as a parameter.
                NodeList<Expression> arguments = n.getArguments();
                if(n.getNameAsString().equals("newThreadPerTaskExecutor")){
                    arguments.set(0, javaParser.parseExpression(virtualFactoryName[0]).getResult().get());
                }
                else{
                    arguments.add(javaParser.parseExpression(virtualFactoryName[0]).getResult().get());
                }
                n.setArguments(arguments);
                return n;
            }
            else{
                //if it was an interface we  search for a member variable that is a factory.
                ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) blockFinder;
                decl.getFields().forEach(x -> {
                    if(x.getCommonType().toString().equals("ThreadFactory"))
                        virtualFactoryName[0] = x.getVariable(0).getName().toString();
                });

                //if we find none we create one.
                JavaParser javaParser = new JavaParser();
                if(virtualFactoryName[0] == ""){
                    decl.addField("ThreadFactory", "__Dummy_Virtual_Factory__ = Thread.ofVirtual().factory()", Modifier.Keyword.STATIC);
                    NodeList<BodyDeclaration<?>> fields = decl.getMembers();
                    fields.add(0, fields.get(fields.size() - 1));
                    fields.remove(fields.size() - 1);
                    virtualFactoryName[0] = "__Dummy_Virtual_Factory__";
                }

                //then use it as a parameter.
                NodeList<Expression> arguments = n.getArguments();
                if(n.getNameAsString().equals("newThreadPerTaskExecutor")){
                    arguments.set(0, javaParser.parseExpression(virtualFactoryName[0]).getResult().get());
                }
                else{
                    arguments.add(javaParser.parseExpression(virtualFactoryName[0]).getResult().get());
                }
                n.setArguments(arguments);
                return n;
            }

        }
    }

    private static class FourthPattern extends ModifierVisitor<Void> {

        @Override
        public MethodCallExpr visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);

            //like the last rule we check for the number of parameters
            int maxParams = -1;
            String[] applicableFunctions= {"runAsync", "supplyAsync", "thenApplyAsync", "thenAcceptAsync", "thenRunAsync", "thenComposeAsync", "handleAsync", "exceptionallyAsync", "exceptionallyComposeAsync"};
            String[] applicableFunctionsExtention = {"thenCombineAsync", "thenAcceptBothAsync", "runAfterBothAsync", "applyToEitherAsync", "acceptEitherAsync", "runAfterEitherAsync", "whenCompleteAsync"};


            //functions of list 2 take at most 2 parameters
            if(Arrays.stream(applicableFunctions).anyMatch(n.getNameAsString()::equals))
                maxParams = 2;

            //functions of list 3 take at most 3 parameters
            if(Arrays.stream(applicableFunctionsExtention).anyMatch(n.getNameAsString()::equals))
                maxParams = 3;

            //check if function name is in one of the list and it can have an additional parameter.
            if(maxParams == -1 || n.getArguments().size() >= maxParams)
                return n;

            edited = true;
            curRule[3]++;
            final String[] virtualExecName = {""};
            Node blockFinder = n.getParentNode().get();
            //we iterate upwards in the AST to find the node of type block statement
            while(blockFinder.getClass() != BlockStmt.class)blockFinder = blockFinder.getParentNode().get();
            BlockStmt block = (BlockStmt) blockFinder;
            NodeList<Statement> codeInBlock = block.getStatements();
            AtomicBoolean beforeCur = new AtomicBoolean(true);
            Node stmtNode = n.getParentNode().get();
            while(stmtNode.getClass() != ExpressionStmt.class)stmtNode = stmtNode.getParentNode().get();
            Statement stmt = (Statement) stmtNode;
            //then we iterate over all statement before the current statement to know if a virtual executor exists
            codeInBlock.forEach((x -> {
                if(!virtualExecName[0].equals("") || !beforeCur.get())return;
                if(x.equals(stmt)){
                    beforeCur.set(false);
                    return;
                }
                if(x.getChildNodes().get(0).getClass() == VariableDeclarationExpr.class){
                    VariableDeclarationExpr decl = (VariableDeclarationExpr) x.getChildNodes().get(0);
                    if(decl.getElementType().asString().equals("ExecutorService")){
                        virtualExecName[0] = decl.getVariables().get(0).getName().asString();
                    }
                }
            }));

            JavaParser javaParser = new JavaParser();
            //if there is no virtual executor we create one called "__Dummy_Virtual_Executor__"
            if(virtualExecName[0].equals("")){
                n.tryAddImportToParentCompilationUnit(Executors.class);
                n.tryAddImportToParentCompilationUnit(ExecutorService.class);
                virtualExecName[0] = "__Dummy_Virtual_Executor__";
                codeInBlock.add(0, javaParser.parseStatement("ExecutorService __Dummy_Virtual_Executor__ = Executors.newVirtualThreadPerTaskExecutor();").getResult().get());
                codeInBlock.add(javaParser.parseStatement("__Dummy_Virtual_Executor__.shutdown();").getResult().get());
                codeInBlock.add(javaParser.parseStatement("__Dummy_Virtual_Executor__.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);").getResult().get());
                block.setStatements(codeInBlock);
            }
            //Then we add it as a parameter.
            NodeList<Expression> arguments = n.getArguments();
            arguments.add(javaParser.parseExpression(virtualExecName[0]).getResult().get());
            n.setArguments(arguments);

            return n;
        }
    }

    private static class FifthPattern extends ModifierVisitor<Void> {

        @Override
        public SuperExpr visit(SuperExpr n, Void arg) {
            super.visit(n, arg);
            //we focus here on all super expressions calls.

            Node classNode = n.getParentNode().get();
            //we iterate up to find the class decleration node in the AST.
            while(classNode.getClass() != ClassOrInterfaceDeclaration.class)classNode = classNode.getParentNode().get();
            ClassOrInterfaceDeclaration classDec = (ClassOrInterfaceDeclaration) classNode;
            AtomicBoolean isCompletableFuture = new AtomicBoolean(false);
            //we check if it extends the completable futures class
            classDec.getExtendedTypes().forEach(x -> {
                if(x.getNameAsString().equals("CompletableFuture")) isCompletableFuture.set(true);
            });
            if(!isCompletableFuture.get())
                return n;
            //then we traverse up again and check if the method call is one of the next ones
            int maxParams = -1;
            String[] applicableFunctions= {"runAsync", "supplyAsync", "thenApplyAsync", "thenAcceptAsync", "thenRunAsync", "thenComposeAsync", "handleAsync", "exceptionallyAsync", "exceptionallyComposeAsync"};
            String[] applicableFunctionsExtention = {"thenCombineAsync", "thenAcceptBothAsync", "runAfterBothAsync", "applyToEitherAsync", "acceptEitherAsync", "runAfterEitherAsync", "whenCompleteAsync"};
            Node functionNode = n.getParentNode().get();
            while(functionNode.getClass() != MethodDeclaration.class)functionNode = functionNode.getParentNode().get();
            MethodDeclaration method = (MethodDeclaration) functionNode;
            if(Arrays.stream(applicableFunctions).anyMatch(method.getNameAsString()::equals))
                maxParams = 2;
            if(Arrays.stream(applicableFunctionsExtention).anyMatch(method.getNameAsString()::equals))
                maxParams = 3;

            MethodCallExpr superCall = (MethodCallExpr) n.getParentNode().get();
            if(maxParams == -1 || superCall.getArguments().size() >= maxParams)
                return n;

            //if so then we replace them.
            edited = true;
            curRule[4]++;
            final String[] virtualExecName = {""};
            Node blockFinder = n.getParentNode().get();
            //we iterate up to find block statement node.
            while(blockFinder.getClass() != BlockStmt.class)blockFinder = blockFinder.getParentNode().get();
            BlockStmt block = (BlockStmt) blockFinder;
            NodeList<Statement> codeInBlock = block.getStatements();
            AtomicBoolean beforeCur = new AtomicBoolean(true);
            Node stmtNode = n.getParentNode().get();
            while(stmtNode.getClass() != ExpressionStmt.class)stmtNode = stmtNode.getParentNode().get();
            Statement stmt = (Statement) stmtNode;
            //we iterate up to find if an executor service is already declared.
            codeInBlock.forEach((x -> {
                if(!virtualExecName[0].equals("") || !beforeCur.get())return;
                if(x.equals(stmt)){
                    beforeCur.set(false);
                    return;
                }
                if(x.getChildNodes().get(0).getClass() == VariableDeclarationExpr.class){
                    VariableDeclarationExpr decl = (VariableDeclarationExpr) x.getChildNodes().get(0);
                    if(decl.getElementType().asString().equals("ExecutorService")){
                        virtualExecName[0] = decl.getVariables().get(0).getName().asString();
                    }
                }
            }));
            JavaParser javaParser = new JavaParser();
            //if there is no executor declared we create one called "__Dummy_Virtual_Executor__".
            if(virtualExecName[0].equals("")){
                n.tryAddImportToParentCompilationUnit(ExecutorService.class);
                n.tryAddImportToParentCompilationUnit(Executors.class);
                virtualExecName[0] = "__Dummy_Virtual_Executor__";
                codeInBlock.add(0, javaParser.parseStatement("ExecutorService __Dummy_Virtual_Executor__ = Executors.newVirtualThreadPerTaskExecutor();").getResult().get());
                block.setStatements(codeInBlock);
            }
            //and we ammend the super class call with the virtual executor.
            superCall.addArgument(virtualExecName[0]);
            return n;
        }
    }

    static void printStats(){
        //prints statistics for the number of migrations done and the rules.
        Integer total = Arrays.stream(rule).sum();
        System.out.println("Total files edited: " + filescnt);
        System.out.println("Total migrations done: " + total);
        System.out.println("The first rule was applied: " + rule[0]);
        System.out.println("The second rule was applied: " + rule[1]);
        System.out.println("The third rule was applied: " + rule[2]);
        System.out.println("The fourth rule was applied: " + rule[3]);
        System.out.println("The fifth rule was applied: " + rule[4]);

    }
}
