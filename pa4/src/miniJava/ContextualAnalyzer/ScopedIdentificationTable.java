package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import java.util.Stack;

import miniJava.Errors.*;
import miniJava.Errors.ClassNotFoundException;
import miniJava.Errors.SelfDeclException;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.TypeKind;

public class ScopedIdentificationTable {
	Stack<Scope> stack = new Stack<Scope>();
	Declaration currentClassDecl;
	Declaration badVarDecl;
	ClassDecl stringClass;
	public static MethodDecl printMethod;
	
	public ScopedIdentificationTable(){
		Scope scope = new Scope(false, false, 0);
		stack.push(scope);
		addPredefined();
		
		scope = new Scope(false, false, 1);
		stack.push(scope);
	}
	
	public void addPredefined(){
		MethodDeclList methodListString = new MethodDeclList();
		FieldDeclList fieldListString = new FieldDeclList();
		ClassDecl stringDecl = new ClassDecl("String", fieldListString, methodListString, null);
		stringClass = stringDecl;
		stack.peek().hashMap.put(stringDecl.name, stringDecl);
		
		MethodDeclList methodListPrint = new MethodDeclList();
		FieldDeclList fieldListPrint = new FieldDeclList();
		StatementList statementListPrint = new StatementList();
		ParameterDeclList parameterListPrint = new ParameterDeclList();
		parameterListPrint.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
		MethodDecl methodPrint = new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null), parameterListPrint, statementListPrint, null);
		printMethod = methodPrint;
		methodListPrint.add(methodPrint);
		ClassDecl printDecl = new ClassDecl("_PrintStream", fieldListPrint, methodListPrint, null);
		printDecl.memberMap.put("println", methodPrint);
		stack.peek().hashMap.put(printDecl.name, printDecl);
		
		MethodDeclList methodListSystem = new MethodDeclList();
		FieldDeclList fieldListSystem = new FieldDeclList();
		Identifier id = new Identifier(new Token(TokenKind.IDENTIFIER, new StringBuffer("_PrintStream"), null));
		FieldDecl fieldSystem = new FieldDecl(false, true, new ClassType(id, null), "out", null);
		fieldListSystem.add(fieldSystem);
		ClassDecl systemDecl = new ClassDecl("System", fieldListSystem, methodListSystem, null);
		systemDecl.memberMap.put("out", fieldSystem);
		stack.peek().hashMap.put(systemDecl.name, systemDecl);
	}
	
	public void pushScope(boolean isStatic, boolean isPrivate){
		Scope scope = new Scope(isStatic, isPrivate, this.getScope().level + 1);
		stack.push(scope);
	}
	
	public void popScope(){
		stack.pop();
	}
	
	public Scope getScope(){
		if(stack.empty()){
			return null;
		}
		else { 
			return stack.peek();
		}
	}
	
	public void addDeclaration(Declaration d){
		if(this.getScope().level > 3){
			for(int i = 0; i <= this.getScope().level - 3; i++){
				if(this.stack.get(this.stack.size()-i-1).hashMap.containsKey(d.name)){
					throw new OverwritingPrevDeclException(d.posn);
				}
			}
			stack.peek().hashMap.put(d.name, d);
		}
		else{
			if(this.stack.peek().hashMap.containsKey(d.name)){
				throw new OverwritingDeclException(d.posn);
			}
			stack.peek().hashMap.put(d.name, d);
		}
	}
	
	public Declaration getDeclaration(String declString, Reference ref){
		Declaration declReturn = null;
		for(int i = 0; i < stack.size(); i++){
			if(stack.get(stack.size()-i-1).hashMap.containsKey(declString)){
				declReturn = stack.get(stack.size()-i-1).hashMap.get(declString);
				break;
			}
		}
		if(declReturn != null){
			if(declReturn.equals(badVarDecl)){
				throw new SelfDeclException(ref.posn);
			}
		}
		else{
			throw new DeclNotFoundException(ref.posn);
		}
		return declReturn;
	}
	
	public Declaration getDeclInClass(String className, String varName, Reference ref){
		if(stack.get(1).hashMap.containsKey(className)){
			if(((ClassDecl)stack.get(1).hashMap.get(className)).memberMap.containsKey(varName)){
				if(((ClassDecl)stack.get(1).hashMap.get(className)).memberMap.get(varName) instanceof MemberDecl){
					// check that if memeber is private check that its in current class
					if(((MemberDecl)((ClassDecl)stack.get(1).hashMap.get(className)).memberMap.get(varName)).isPrivate == false 
							|| stack.get(1).hashMap.get(className) == currentClassDecl){
						return ((ClassDecl)stack.get(1).hashMap.get(className)).memberMap.get(varName);
					}
					else{
						throw new PrivateAccessException(ref.posn);
					}
				}
				else{
					throw new MemberNotDefinedException(ref.posn);
				}
			}
			else{
				throw new MemberNotDefinedException(ref.posn);
			}
		}
		if(stack.get(0).hashMap.containsKey(className)){
			if(((ClassDecl)stack.get(0).hashMap.get(className)).memberMap.containsKey(varName)){
				if(((ClassDecl)stack.get(0).hashMap.get(className)).memberMap.get(varName) instanceof MemberDecl){
					// check that if memeber is private check that its in current class
					if(((MemberDecl)((ClassDecl)stack.get(0).hashMap.get(className)).memberMap.get(varName)).isPrivate == false 
							|| stack.get(0).hashMap.get(className) == currentClassDecl){
						return ((ClassDecl)stack.get(0).hashMap.get(className)).memberMap.get(varName);
					}
					else{
						throw new PrivateAccessException(ref.posn);
					}
				}
				else{
					throw new MemberNotDefinedException(ref.posn);
				}
			}
			else{
				throw new MemberNotDefinedException(ref.posn);
			}
		}
		else{
			throw new ClassNotFoundException(ref.posn);
		}
	}
}
