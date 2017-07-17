/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.HashMap;

import miniJava.CodeGenerator.ClassRED;
import  miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassDecl extends Declaration {
	public HashMap<String, Declaration> memberMap = new HashMap<String, Declaration>();

  public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  super(cn, null, posn);
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
	  
	  int num = 0;
	  for(FieldDecl decl: fdl){
		  if(!decl.isStatic){
			  num++;
		  }
	  }
	  RED.size = num;
  }
  
  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }
      
  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
  public ClassRED RED = new ClassRED(0);
}
