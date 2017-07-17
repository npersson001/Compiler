package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class OverwritingDeclException extends RuntimeException{
	public OverwritingDeclException(SourcePosition pos){
		super("***attempt to overwrite declaration at lines: " + pos.start + "-" + pos.finish);
	}
}
