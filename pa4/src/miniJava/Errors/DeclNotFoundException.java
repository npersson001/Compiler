package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class DeclNotFoundException extends RuntimeException{
	public DeclNotFoundException(SourcePosition pos){
		super("***declaration not found at lines: " + pos.start + "-" + pos.finish);
	}
}
