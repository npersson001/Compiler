package miniJava;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.CodeGenerator.ASTCodeGen;
import miniJava.ContextualAnalyzer.ASTIdentifier;
import miniJava.ContextualAnalyzer.ASTTyper;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourceFile;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;
import miniJava.Errors.ClassNotFoundException;
import miniJava.Errors.InvalidClassException;
import miniJava.Errors.MemberNotDefinedException;
import miniJava.Errors.NonstaticAccessFromStaticException;
import miniJava.Errors.ObjectFileFailedException;
import miniJava.Errors.OverwritingDeclException;
import miniJava.Errors.OverwritingPrevDeclException;
import miniJava.Errors.PrivateAccessException;
import miniJava.Errors.SelfDeclException;
import miniJava.Errors.SingleVarDeclException;
import miniJava.Errors.ThisAssignmentException;

public class Compiler {
	public static void main(String[] args){
		InputStream inputStream = null;
		//InputStream inputStream = new ByteArrayInputStream("class A {void meth(){a.b[3].c = 5;}}".getBytes());
		
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = System.in;
		}
		else {
			try {
				inputStream = new FileInputStream(new File(args[0]));
			} catch (FileNotFoundException e) {
				System.out.println("Input file " + args[0] + " not found");
				System.exit(1);
			}		
		}
		
		//create all required objects
		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(new SourceFile(inputStream));
		Parser parser = new Parser(scanner, reporter);
		ASTDisplay display = new ASTDisplay();
		ASTIdentifier identifier = new ASTIdentifier();
		ASTTyper typeChecker = new ASTTyper();
		ASTCodeGen generator = new ASTCodeGen();
		
		//parse the program
		miniJava.AbstractSyntaxTrees.Package program = parser.parseProgram();
		
		//store mainMethod for code generation phase
		MethodDecl mainMethod = null;
		
		
		if(program != null){
			//do identification and type checking
			try{
				//display.showTree(program);
				mainMethod = identifier.decorate(program);
				typeChecker.typeCheck(program);
				
			}
			catch(Exception e){
				if(e.getMessage() != null){
					System.out.println(e.getMessage());
				}
				else{
					System.out.println(e);
					System.out.println("***unknown error encountered during identification or type checking");
				}
				System.exit(4);
			}
			
			//stop if there are type errors
			if(typeChecker.errors.positions.size() > 0){
				typeChecker.errors.printErrors();
				System.exit(4);
			}
			
			//do code generation 
			try{
				generator.generate(program, mainMethod);
				
				String file = args[0].substring(0, args[0].lastIndexOf('.'));
				
				String objectFileName = file + ".mJAM";
				ObjectFile objectFile = new ObjectFile(objectFileName);
				if(objectFile.write()){
					throw new ObjectFileFailedException();
				}
				//run lines below here for debugging the mJAM code
				
				// *********************************************************************
				/*
				String asmCodeFileName = objectFileName.replace(".mJAM",".asm");
		        System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
		        Disassembler d = new Disassembler(objectFileName);
		        if (d.disassemble()) {
		                System.out.println("FAILED!");
		                return;
		        }
		        else
		                System.out.println("SUCCEEDED");
		        
		        System.out.println("Running code in debugger ... ");
		        Interpreter.debug(objectFileName, asmCodeFileName);

		        System.out.println("*** mJAM execution completed");
				*/
		        // *********************************************************************
				
			}
			catch(Exception e){
				if(e.getMessage() != null){
					System.out.println(e.getMessage());
				}
				else{
					System.out.println(e);
					System.out.println("***unknown error encountered during code generation");
				}
				System.exit(4);
			}
			
			//exit successfully at end
			System.exit(0);
		}
		else{
			System.exit(4);
		}
	}
}
