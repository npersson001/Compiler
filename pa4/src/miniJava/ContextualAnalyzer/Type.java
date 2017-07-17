package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.TypeKind;

public class Type {
	TypeKind type;
	Declaration decl;
	Type elmType;
	
	public Type(TypeKind typeA, Declaration declA, Type typeB){
		type = typeA;
		decl = declA;
		elmType = typeB;
	}
}
