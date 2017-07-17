package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class SelfDeclException extends RuntimeException{
	public SelfDeclException(SourcePosition pos){
		super("***vardeclstmt declares variable equal to itself at lines: " + pos.start + "-" + pos.finish);
	}
}
