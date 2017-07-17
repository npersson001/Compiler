package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class PrivateAccessException extends RuntimeException{
	public PrivateAccessException(SourcePosition pos){
		super("***attempted access of private member at lines: " + pos.start + "-" + pos.finish);
	}
}
