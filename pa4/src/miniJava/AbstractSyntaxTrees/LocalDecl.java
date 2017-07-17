/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.LocalRED;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class LocalDecl extends Declaration {
	public LocalRED RED = new LocalRED(0);
	
	public LocalDecl(String name, TypeDenoter t, SourcePosition posn){
		super(name,t,posn);
	}

}
