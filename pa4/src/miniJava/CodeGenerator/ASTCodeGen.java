package miniJava.CodeGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxIdRef;
import miniJava.AbstractSyntaxTrees.IxQRef;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.LocalDecl;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.QRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.ContextualAnalyzer.ScopedIdentificationTable;
import miniJava.SyntacticAnalyzer.TokenKind;

public class ASTCodeGen implements Visitor<Object,Object>{
	int localDisplacement;
	HashMap<Integer, MethodRED> methods = new HashMap<Integer, MethodRED>();
	List<FieldDecl> staticClassList = new ArrayList<FieldDecl>();
	
	// 0 = value, 1 = address, 2 = method
	
	/* ************************** Helper Functions ************************** */
	
	public void generate(Package prog, MethodDecl mainDecl){
		prog.visit(this, mainDecl);
	}
	
	public int localSpace(StatementList statementList) {
		int num = 0;

		//find number of variables declared in the local scope so space can be allocated
		for (int i = 0; i < statementList.size(); i++) {
			Statement currentStmt = statementList.get(i);
			if(currentStmt instanceof VarDeclStmt){
				((VarDeclStmt)currentStmt).varDecl.RED.displacement = localDisplacement;
				localDisplacement++;
				num++;
			}
		}

		//allocate the space
		Machine.emit(Op.PUSH, num);
		return num;
	}
	
	public void patchMethodCalls(){
		//go through all methods found and patch them
		for (Integer patchAddr : methods.keySet())
			Machine.patch(patchAddr, methods.get(patchAddr).displacement);
	}
	
	//give fields displacements (static and nonstatic fields)
	public void fieldDisplacement(Package prog){
		int statDis = 0;
		for(int i = 0; i < prog.classDeclList.size(); i++){
			int instDis = 0;
			for(int j = 0; j < prog.classDeclList.get(i).fieldDeclList.size(); j++){
				//if the field is nonstatic give it displacement relative to LB
				if(!prog.classDeclList.get(i).fieldDeclList.get(j).isStatic){
					prog.classDeclList.get(i).fieldDeclList.get(j).RED.displacement = instDis;
					instDis++;
				}
				//if the field is static give it displacement relative to SB
				else{
					prog.classDeclList.get(i).fieldDeclList.get(j).RED.displacement = statDis;
					statDis++;
					//add all static fields that are of class type to an list
					if(prog.classDeclList.get(i).fieldDeclList.get(j).type.typeKind == TypeKind.CLASS){
						staticClassList.add(prog.classDeclList.get(i).fieldDeclList.get(j));
					}
				}
			}
		}
		Machine.emit(Op.PUSH, statDis);
	}
	
	//place static classes on heap
	public void staticClass(){
		for(int i = 0; i < staticClassList.size(); i++){
			Machine.emit(Op.LOADL, -1);
			Machine.emit(Op.LOADL, ((ClassDecl)((ClassType)staticClassList.get(i).type).className.decl).RED.size);
			Machine.emit(Prim.newobj);
			Machine.emit(Op.STORE, Reg.SB, staticClassList.get(i).RED.displacement);
			
			
			/*
			  8         LOADL        -1
			  9         LOADL        2
			 10         CALL         newobj  
			 11         STORE        3[LB]
			 */
		}
	}
	
	/* ************************** Begin Visitors ************************** */

	public Object visitPackage(Package prog, Object arg) {
		//set up code generation environment
		Machine.initCodeGen();
		
		//calculate field displacements and push space on stack
		this.fieldDisplacement(prog);
		
		//place static objects on the heap using the displacements calculated
		//this.staticClass();
		
		//set input array of length 0 on the heap an put address of array in stack
		Machine.emit(Op.LOADL,0); 
		Machine.emit(Prim.newarr);
		
		//setup call to main to patch later
		int patchAddr_Call_main = Machine.nextInstrAddr();
		
		//call main method
		Machine.emit(Op.CALL,Reg.CB,-1);    
		
		//halt program
		Machine.emit(Op.HALT,0,0,0);
		
		//visit the classes
		for(int i = 0; i < prog.classDeclList.size(); i++){
			prog.classDeclList.get(i).visit(this, null);
		}
		
		
		// Patch all method calls that were made
		this.patchMethodCalls();

		//patch the actual main method
		MethodDecl mainMethod = (MethodDecl) arg;
		Machine.patch(patchAddr_Call_main, mainMethod.RED.displacement);

		return null;
		
	}

	public Object visitClassDecl(ClassDecl cd, Object arg) {
		//visit fields and methods
		for(int i = 0; i < cd.fieldDeclList.size(); i++){
			cd.fieldDeclList.get(i).visit(this, null);
		}
		for(int i = 0; i < cd.methodDeclList.size(); i++){
			cd.methodDeclList.get(i).visit(this, null);
		}
		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		//do nothing, these are never instantiated in miniJava
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, Object arg) {
		localDisplacement = 3;
		int displacement = 0;
		
		//set displacement address of method
		md.RED.displacement = Machine.nextInstrAddr();
		
		//set displacement of parameter declarations (must be negative relative to LB)
		displacement = md.parameterDeclList.size() * -1;
		for(int i = 0; i < md.parameterDeclList.size(); i++){
			md.parameterDeclList.get(i).visit(this, null);
			md.parameterDeclList.get(i).RED.displacement = displacement;
			displacement++;
		}
		
		//allocate space on the stack for local variables
		int num = localSpace(md.statementList);
		
		//visit the statements to add them on the stack
		for(int i = 0; i < md.statementList.size(); i++){
			md.statementList.get(i).visit(this, md.parameterDeclList.size());
		}
		
		//reset local displacement
		localDisplacement -= num;
		
		return null;
	}

	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		return null;
	}

	public Object visitVarDecl(VarDecl decl, Object arg) {
		return null;
	}

	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	public Object visitClassType(ClassType type, Object arg) {
		return null;
	}

	public Object visitArrayType(ArrayType type, Object arg) {
		return null;
	}

	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		//allocate space on stack, just like in visitMethod
		int num;
		num = localSpace(stmt.sl);
		
		//loop through and visit the statements
		for(int i = 0; i < stmt.sl.size(); i++){
			stmt.sl.get(i).visit(this, arg);
		}
		
		//reset localDisplacement
		localDisplacement -= num;
		
		//pop the stack because no return is called to do it
		Machine.emit(Op.POP, num);
		
		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		//visit expression 
		stmt.initExp.visit(this, 0);
		
		//emit command to store the value at the appropriate place on the stack
		Machine.emit(Op.STORE, Reg.LB, stmt.varDecl.RED.displacement);
		
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		//check if local or not by checking if the declaration is a local declaration and not an array type
		if(stmt.ref.decl instanceof LocalDecl && stmt.ref.decl.type.typeKind != TypeKind.ARRAY){
			//store value on stack
			stmt.val.visit(this, null);
			Machine.emit(Op.STORE, Reg.LB, ((LocalDecl)stmt.ref.decl).RED.displacement);
		}
		//account for writing to a static field on the stack relative to SB instead of heap
		else if(stmt.ref.decl instanceof FieldDecl){
			if(((FieldDecl)stmt.ref.decl).isStatic){
				if(stmt.ref instanceof IxIdRef || stmt.ref instanceof IxQRef){
					stmt.ref.visit(this, 1);
					stmt.val.visit(this, null);
					Machine.emit(Prim.arrayupd);
				}
				else{
					stmt.val.visit(this, null);
					Machine.emit(Op.STORE, Reg.SB, ((FieldDecl)stmt.ref.decl).RED.displacement);
				}
			}
			//an exact copy of the overall else, couldnt cast ref.decl as a field in the same if statement as instance check
			else{
				//visit ref and val
				stmt.ref.visit(this, 1);
				stmt.val.visit(this, null);
				
				//check if field or array update is called
				//*************************************** ixid or ixqref?
				if(stmt.ref instanceof IxIdRef || stmt.ref instanceof IxQRef){
					Machine.emit(Prim.arrayupd);
				}
				else{
					Machine.emit(Prim.fieldupd);
				}
			}
		}
		else{
			//visit ref and val
			stmt.ref.visit(this, 1);
			stmt.val.visit(this, null);
			
			//check if field or array update is called
			//*************************************** ixid or ixqref?
			if(stmt.ref instanceof IxIdRef || stmt.ref instanceof IxQRef){
				Machine.emit(Prim.arrayupd);
			}
			else{
				Machine.emit(Prim.fieldupd);
			}
		}
		return null;
	}

	// TODO
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		//visit the parameters
		for(int i = 0; i < stmt.argList.size(); i++){
			stmt.argList.get(i).visit(this, null);
		}
		
		//*************************************** method part of own class
		/*
		//if method call is in same class and is nonstatic then load its address
		if (stmt.methodRef.decl instanceof MethodDecl) {
			if(!((MethodDecl) stmt.methodRef.decl).isStatic && ){
				Machine.emit(Op.LOADA, Reg.OB, 0);
			}
		}
		*/
		
		//when println is called
		if(stmt.methodRef.decl == ScopedIdentificationTable.printMethod){
			Machine.emit(Prim.putintnl);
			return null;
		}
		
		//visit the ref
		stmt.methodRef.visit(this, 2);
		
		//delete return value from stack if call stmt
		if(((MethodDecl)stmt.methodRef.decl).type.typeKind != TypeKind.VOID){
			Machine.emit(Prim.dispose);
		}
		
		//*************************************** System.out.println();
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		//determine if there is a return stmt at all
		if(stmt.returnExpr == null){
			Machine.emit(Op.RETURN, 0, 0, (int) arg);
		}
		else{
			stmt.returnExpr.visit(this, 0);
			Machine.emit(Op.RETURN, 1, 0, (int) arg);
		}
		
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, Object arg) {
		//visit the if condition
		stmt.cond.visit(this, null);
		
		//setup else statement to patch later
		int patchAddr_Else = Machine.nextInstrAddr();
		
		//jump to the if statement
		Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);
		
		//visit the then statement
		stmt.thenStmt.visit(this, arg);
		
		//check if there is an else statement 
		if(stmt.elseStmt == null){
			//patch address and do nothing, there is no else
			Machine.patch(patchAddr_Else, Machine.nextInstrAddr());
		}
		else{
			int patchAddr_End = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, 0);
			Machine.patch(patchAddr_Else, Machine.nextInstrAddr());
			stmt.elseStmt.visit(this, arg);
			Machine.patch(patchAddr_End, Machine.nextInstrAddr());
		}
		
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		//setup while statement address to patch later
		int patchAddr_While = Machine.nextInstrAddr();
		
		//emit command to jump to the start of the test (address to be patched later)
		Machine.emit(Op.JUMP, 0, Reg.CB, -1);
		
		//set the start of the loop address
		int loop = Machine.nextInstrAddr();
		
		//visit the body of the loop statement
		stmt.body.visit(this, arg);
		
		//get actual address of the while and visit the conditions of the while loop
		int addr_While = Machine.nextInstrAddr();
		stmt.cond.visit(this, null);
		
		//jump back up to the loop
		Machine.emit(Op.JUMPIF, 1, Reg.CB, loop);
		
		//patch the address of the while
		Machine.patch(patchAddr_While, addr_While);
				
		return null;
	}

	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		//visit expression to put it on the stack
		expr.expr.visit(this, null);
		
		//emit command to make the expr negative or complement
		if(expr.operator.kind == TokenKind.NOT){
			Machine.emit(Prim.not);
		}
		else if(expr.operator.kind == TokenKind.SUBTRACTION){
			Machine.emit(Prim.neg);
		}
		
		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		//visit both of the expr sides
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		
		//emit command based on operator type
		if(expr.operator.kind == TokenKind.GREATER){
			Machine.emit(Prim.gt);
		}
		else if(expr.operator.kind == TokenKind.LESS){
			Machine.emit(Prim.lt);
		}
		else if(expr.operator.kind == TokenKind.GREATER_EQUAL){
			Machine.emit(Prim.ge);
		}
		else if(expr.operator.kind == TokenKind.LESS_EQUAL){
			Machine.emit(Prim.le);
		}
		else if(expr.operator.kind == TokenKind.DOUBLE_EQUAL){
			Machine.emit(Prim.eq);
		}
		else if(expr.operator.kind == TokenKind.NOT_EQUAL){
			Machine.emit(Prim.ne);
		}
		else if(expr.operator.kind == TokenKind.OR){
			Machine.emit(Prim.or);
		}
		else if(expr.operator.kind == TokenKind.AND){
			Machine.emit(Prim.and);
		}
		else if(expr.operator.kind == TokenKind.ADDITION){
			Machine.emit(Prim.add);
		}
		else if(expr.operator.kind == TokenKind.SUBTRACTION){
			Machine.emit(Prim.sub);
		}
		else if(expr.operator.kind == TokenKind.MULTIPLICATION){
			Machine.emit(Prim.mult);
		}
		else if(expr.operator.kind == TokenKind.DIVISION){
			Machine.emit(Prim.div);
		}
		
		return null;
	}

	public Object visitRefExpr(RefExpr expr, Object arg) {
		//visit the reference in the expression
		expr.ref.visit(this, 0);
		return null;
	}

	public Object visitCallExpr(CallExpr expr, Object arg) {
		//put the arguments on the stack
		for(int i = 0; i < expr.argList.size(); i++){
			expr.argList.get(i).visit(this, null);
		}
		
		/*
		//check if the method is in the same class
		if(expr.functionRef.decl instanceof MemberDecl){
			Machine.emit(Op.LOADA, Reg.OB, 0);
		}
		*/
		
		//visit the method reference
		expr.functionRef.visit(this, 2);
		
		return null;
	}

	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		//visit the literal
		expr.lit.visit(this, null);
		
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		//set the literal to -1 for object and then the object size and then the command to put it on the heap
		Machine.emit(Op.LOADL, -1);
		Machine.emit(Op.LOADL, ((ClassDecl)expr.classtype.className.decl).RED.size);
		Machine.emit(Prim.newobj);
		
		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		//visit the array initializing expression 
		expr.sizeExpr.visit(this, null);
		
		//emit the command to create the new array on the heap
		Machine.emit(Prim.newarr);
		
		return null;
	}

	public Object visitThisRef(ThisRef ref, Object arg) {
		//emit command to load address of current object
		Machine.emit(Op.LOADA, Reg.OB, 0);
		
		return null;
	}

	// TODO
	public Object visitIdRef(IdRef ref, Object arg) {
		//determine if the reference is a value, address, or method
		//if the reference is a value
		if((int) arg == 0){
			//if the reference is a local variable and the value is wanted
			if(ref.decl instanceof LocalDecl){
				Machine.emit(Op.LOAD, Reg.LB, ((LocalDecl)ref.decl).RED.displacement);
			}
			//if the reference is the value in a field
			else if(ref.decl instanceof FieldDecl){
				//if field is static access on stack relative to SB
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl) ref.decl).RED.displacement);
				}
				//if field is nonstatic access on heap relative to OB
				else{
					Machine.emit(Op.LOAD, Reg.OB, ((FieldDecl) ref.decl).RED.displacement);
				}
			}
		}
		//if the reference wants an address
		else if((int) arg == 1){
			if(ref.decl instanceof LocalDecl){
				Machine.emit(Op.LOAD,Reg.LB,((LocalDecl)ref.decl).RED.displacement);
			}
			else if(ref.decl instanceof FieldDecl){
				//if field is static access on stack relative to SB
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl) ref.decl).RED.displacement);
				}
				//if field is nonstatic access on heap relative to OB
				else{
					Machine.emit(Op.LOADA, Reg.OB, 0);
					Machine.emit(Op.LOADL, ((FieldDecl)ref.decl).RED.displacement);
				}
			}
		}
		//if the reference wants a method and is only accessed for single method calls ie "method();"
		else if((int) arg == 2){
			if(ref.decl instanceof MethodDecl){
				//if method is static call
				if(((MethodDecl)ref.decl).isStatic){
					int patchAddr_Method = Machine.nextInstrAddr();
					Machine.emit(Op.CALL, Reg.CB, -1);
					methods.put(patchAddr_Method, ((MethodDecl) ref.decl).RED);
				}
				//if method is nonstatic calli
				else{
					Machine.emit(Op.LOADA, Reg.OB, 0);
					int patchAddr_Method = Machine.nextInstrAddr();
					Machine.emit(Op.CALLI, Reg.CB, -1);
					methods.put(patchAddr_Method, ((MethodDecl) ref.decl).RED);
				}
			}
		}
		
		return null;
	}

	@Override
	public Object visitIxIdRef(IxIdRef ref, Object arg) {
		// TODO Auto-generated method stub
		
		//determine if the reference is a value, address, or method
		//if the reference is a value
		if((int) arg == 0){
			//if the reference is a local variable and the value is wanted
			if(ref.decl instanceof LocalDecl){
				Machine.emit(Op.LOAD, Reg.LB, ((LocalDecl)ref.decl).RED.displacement);
				ref.indexExpr.visit(this, null);
				Machine.emit(Prim.arrayref);
			}
			//if the reference is the value in a field
			else if(ref.decl instanceof FieldDecl){
				//if field is static access on stack relative to SB
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl) ref.decl).RED.displacement);
					ref.indexExpr.visit(this, null);
					Machine.emit(Prim.arrayref);
				}
				//if field is nonstatic access on heap relative to OB
				else{
					Machine.emit(Op.LOAD, Reg.OB, ((FieldDecl) ref.decl).RED.displacement);
					ref.indexExpr.visit(this, null);
					Machine.emit(Prim.arrayref);
				}
			}
		}
		//if the reference wants an address
		else if((int) arg == 1){
			if(ref.decl instanceof LocalDecl){
				Machine.emit(Op.LOAD,Reg.LB,((LocalDecl)ref.decl).RED.displacement);
				ref.indexExpr.visit(this, null);
			}
			else if(ref.decl instanceof FieldDecl){
				//if field is static access on stack relative to SB
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl) ref.decl).RED.displacement);
					ref.indexExpr.visit(this, null);
				}
				//if field is nonstatic access on heap relative to OB
				else{
					Machine.emit(Op.LOAD, Reg.OB, ((FieldDecl) ref.decl).RED.displacement);
					ref.indexExpr.visit(this, null);
				}
			}
		}
				
		return null;
	}

	@Override
	public Object visitQRef(QRef ref, Object arg) {
		// TODO Auto-generated method stub
		
		//do the arraylength
		if(ref.decl.name.equals("length") && ref.ref.decl.type.typeKind == TypeKind.ARRAY){
			ref.ref.visit(this, 1);
			if(ref.ref.decl instanceof FieldDecl){
				//in A.x if A is a static field then its address is on the stack so no need for fieldref
				if(!((FieldDecl)ref.ref.decl).isStatic){
					Machine.emit(Prim.fieldref);
				}
			} 
			Machine.emit(Prim.arraylen);
			return null;
		}
		
		//the reference wants an actual value
		if((int) arg == 0){
			if(ref.decl instanceof FieldDecl){
				//if the value were looking for is static just return its value relative to SB
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl)ref.decl).RED.displacement);
					return null;
				}
				//if statement for special case of ixqref address send with arg 3 (this is a workaround im tired)
				boolean usedThree = false;
				if(ref.ref instanceof IxQRef){
					usedThree = true;
					ref.ref.visit(this, 3);
				}
				else{
					ref.ref.visit(this, 1);
				} 
				//unsure if this is necessary
				if(ref.ref.decl instanceof FieldDecl){
					//in A.x if A is a static field then its address is on the stack so no need for fieldref
					if(!((FieldDecl)ref.ref.decl).isStatic){
						//********************may need ixidref
						//if the ref.ref is ixqref then the value needs to be taken from the array
						if(ref.ref instanceof IxQRef){
							//this is a bit of a workaround but if 3 was used as arg then arrayref already called
							if(!usedThree){
								Machine.emit(Prim.arrayref);
							}
						}
						else if(ref.ref instanceof IxIdRef){
							Machine.emit(Prim.arrayref);
						}
						else{
							Machine.emit(Prim.fieldref);
						}
					}
					else{
						if(ref.ref instanceof IxIdRef){
							Machine.emit(Prim.arrayref);
						}
					}
				} 
				Machine.emit(Op.LOADL, ((FieldDecl)ref.decl).RED.displacement);
				Machine.emit(Prim.fieldref);
			}
		}
		//the reference wants an address of some kind
		else if((int) arg == 1){
			//access field of an object field
			if(ref.decl instanceof FieldDecl){
				//if the address requested is of a static object or array
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl)ref.decl).RED.displacement);
					return null;
				}
				//if statement for special case of ixqref address send with arg 3 (this is a workaround im tired)
				boolean usedThree = false;
				if(ref.ref instanceof IxQRef){
					usedThree = true;
					ref.ref.visit(this, 3);
				}
				else{
					ref.ref.visit(this, 1);
				} 
				
				if(ref.ref.decl instanceof FieldDecl){
					//in A.x if A is a static field then its address is on the stack so no need for fieldref
					if(!((FieldDecl)ref.ref.decl).isStatic){
						//if the ref.ref is ixqref then the value needs to be taken from the array
						if(ref.ref instanceof IxIdRef){
							Machine.emit(Prim.arrayref);
						}
						else if(ref.ref instanceof IxQRef){
							//this is a bit of a workaround but if 3 was used as arg then arrayref already called
							if(!usedThree){
								Machine.emit(Prim.arrayref);
							}
						}
						else{
							Machine.emit(Prim.fieldref);
						}
					}
					else{
						if(ref.ref instanceof IxIdRef){
							Machine.emit(Prim.arrayref);
						}
					}
				} 
				Machine.emit(Op.LOADL, ((FieldDecl)ref.decl).RED.displacement);
			}
		}
		//the reference is a method call of some sort
		else if((int) arg == 2){
			if(ref.decl instanceof MethodDecl){
				if(((MethodDecl)ref.decl).isStatic){
					int patchAddr_Method = Machine.nextInstrAddr();
					Machine.emit(Op.CALL, Reg.CB, -1);
					methods.put(patchAddr_Method, ((MethodDecl) ref.decl).RED);
				}
				else{
					ref.ref.visit(this, 1);
					if(ref.ref.decl instanceof FieldDecl){
						//in A.x if A is a static field then its address is on the stack so no need for fieldref
						if(!((FieldDecl)ref.ref.decl).isStatic){
							Machine.emit(Prim.fieldref);
						}
					} 
					int patchAddr_Method = Machine.nextInstrAddr();
					Machine.emit(Op.CALLI, Reg.CB, -1);
					methods.put(patchAddr_Method, ((MethodDecl) ref.decl).RED);
				}
			}
		}
		
		return null;
	}

	@Override
	public Object visitIxQRef(IxQRef ref, Object arg) {
		// TODO Auto-generated method stub
		
		//if a value is wanted from the ixqref
		if((int) arg == 0){
			if(ref.decl instanceof FieldDecl){
				//if static return from the SB 
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl)ref.decl).RED.displacement);
				}
				else{
					ref.ref.visit(this, 1);
					Machine.emit(Op.LOADL, ((FieldDecl)ref.decl).RED.displacement);
					Machine.emit(Prim.fieldref);
				}
				ref.ixExpr.visit(this, null);
				Machine.emit(Prim.arrayref);
			}
			
		}
		//if an address is needed for writing
		else if((int) arg == 1){
			if(ref.decl instanceof FieldDecl){
				//if static return from the SB 
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl)ref.decl).RED.displacement);
				}
				else{
					ref.ref.visit(this, 1);
					Machine.emit(Op.LOADL, ((FieldDecl)ref.decl).RED.displacement);
					Machine.emit(Prim.fieldref);
				}
				ref.ixExpr.visit(this, null);
				//in b.arr[1].x but not in b.arr[2] -- if its an object we need arrayref to get its address
				//if(((ArrayType)ref.decl.type).eltType.typeKind == TypeKind.CLASS){
				//	Machine.emit(Prim.arrayref);
				//}
			}
		}
		else if((int) arg == 3){
			if(ref.decl instanceof FieldDecl){
				//if static return from the SB 
				if(((FieldDecl)ref.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ((FieldDecl)ref.decl).RED.displacement);
				}
				else{
					ref.ref.visit(this, 1);
					Machine.emit(Op.LOADL, ((FieldDecl)ref.decl).RED.displacement);
					Machine.emit(Prim.fieldref);
				}
				ref.ixExpr.visit(this, null);
				//in b.arr[1].x but not in b.arr[2] -- if its an object we need arrayref to get its address
				if(((ArrayType)ref.decl.type).eltType.typeKind == TypeKind.CLASS){
					Machine.emit(Prim.arrayref);
				}
			}
		}
		
		return null;
	}

	public Object visitIdentifier(Identifier id, Object arg) {
		return null;
	}

	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	public Object visitIntLiteral(IntLiteral num, Object arg) {
		Machine.emit(Op.LOADL, Integer.parseInt(num.spelling));
		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if(bool.spelling.equals("false")){
			Machine.emit(Op.LOADL, 0);
		}
		else{
			Machine.emit(Op.LOADL, 1);
		}
		return null;
	}

	public Object visitNullLiteral(NullLiteral nu, Object arg) {
		Machine.emit(Op.LOADL, 0);
		return null;
	}

}
