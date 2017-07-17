package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class InvalidClassException extends RuntimeException{
	public InvalidClassException(SourcePosition pos){
		super("***qref not a classtype at lines: " + pos.start + "-" + pos.finish);
	}
}
