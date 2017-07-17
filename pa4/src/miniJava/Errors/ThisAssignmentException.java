package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ThisAssignmentException extends RuntimeException{
	public ThisAssignmentException(SourcePosition pos){
		super("***this keyword used on left side of an assignment at lines: " + pos.start + "-" + pos.finish);
	}
}
