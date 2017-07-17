package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.List;

import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class Errors {
	public List<MessageAndPosition> positions = new ArrayList<MessageAndPosition>();
	
	public void addError(String message,SourcePosition pos){
		positions.add(new MessageAndPosition(message, pos));
	}
	
	public void printErrors(){
		for(int i = 0; i < positions.size(); i++){
			System.out.println("***type error " + (i+1) + " (" + positions.get(i).message + ") at lines: " + positions.get(i).position.start + "-" + positions.get(i).position.finish);
		}
	}
}
