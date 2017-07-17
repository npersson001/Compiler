package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class SingleVarDeclException extends RuntimeException{
	public SingleVarDeclException(SourcePosition pos) {
		super("***single variable declaration within if/else statement at lines: " + pos.start + "-" + pos.finish);
	}
}
