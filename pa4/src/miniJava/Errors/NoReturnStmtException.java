package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NoReturnStmtException extends RuntimeException{
	public NoReturnStmtException(SourcePosition pos){
		super("***no return statement provided for method at lines: " + pos.start + "-" + pos.finish);
	}
}
