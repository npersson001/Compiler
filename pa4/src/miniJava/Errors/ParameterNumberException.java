package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ParameterNumberException extends RuntimeException{
	public ParameterNumberException(SourcePosition pos){
		super("***unequal number of parameters and arguments in method call at lines: " + pos.start + "-" + pos.finish);
	}
}
