package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class OverwritingPrevDeclException extends RuntimeException {
	public OverwritingPrevDeclException(SourcePosition pos){
		super("***attempt to overwrite declaration from level 3 or higher from level 4 or higher at lines: " + pos.start + "-" + pos.finish);
	}
}
