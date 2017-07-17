package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import miniJava.AbstractSyntaxTrees.Declaration;

public class Scope {
	HashMap<String, Declaration> hashMap;
	boolean isStatic;
	boolean isPrivate;
	int level;
	
	public Scope(boolean staticBool, boolean privateBool, int levelVal){
		hashMap = new HashMap<String, Declaration>();
		isStatic = staticBool;
		isPrivate = privateBool;
		level = levelVal;
	}
}
