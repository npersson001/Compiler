package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NonstaticAccessFromStaticException extends RuntimeException{
	public NonstaticAccessFromStaticException(SourcePosition pos){
		super("***access attemt from static scope to nonstatic member at lines: " + pos.start + "-" + pos.finish);
	}
}
