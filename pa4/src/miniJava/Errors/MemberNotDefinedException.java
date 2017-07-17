package miniJava.Errors;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class MemberNotDefinedException extends RuntimeException{
	public MemberNotDefinedException(SourcePosition pos){
		super("***member not defined in given class at lines: " + pos.start + "-" + pos.finish);
	}
}
