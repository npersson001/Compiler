package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class MethodFieldException extends RuntimeException{
	public MethodFieldException(SourcePosition pos){
		super("***attempted access of a method field: " + pos.start + "-" + pos.finish);
	}
}
