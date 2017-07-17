package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class AssigningArrayLengthException extends RuntimeException{
	public AssigningArrayLengthException(SourcePosition pos){
		super("***attempted assignment of array length at lines: " + pos.start + "-" + pos.finish);
	}
}
