package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class MessageAndPosition {
	SourcePosition position;
	String message;
	
	public MessageAndPosition(String mes, SourcePosition valPos){
		position = valPos;
		message = mes;
	}
}
