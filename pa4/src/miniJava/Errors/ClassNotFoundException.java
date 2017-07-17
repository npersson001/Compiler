package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassNotFoundException extends RuntimeException{
	public ClassNotFoundException(SourcePosition pos){
		super("***class name not found in class declaration table at lines: " + pos.start + "-" + pos.finish);
	}
}
