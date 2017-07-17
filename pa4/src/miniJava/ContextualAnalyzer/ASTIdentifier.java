package miniJava.ContextualAnalyzer;

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
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxIdRef;
import miniJava.AbstractSyntaxTrees.IxQRef;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
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
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.Errors.InvalidClassException;
import miniJava.Errors.MemberNotDefinedException;
import miniJava.Errors.MethodFieldException;
import miniJava.Errors.MultipleMainMethodFoundException;
import miniJava.Errors.NoMainMethodFoundException;
import miniJava.Errors.NonstaticAccessFromStaticException;
import miniJava.Errors.SingleVarDeclException;
import miniJava.Errors.ThisAssignmentException;
import miniJava.Errors.AssigningArrayLengthException;
import miniJava.Errors.ClassNotFoundException;

public class ASTIdentifier implements Visitor<ScopedIdentificationTable,Object>{
	ScopedIdentificationTable table;
	FieldDecl lengthDecl = new FieldDecl(false, true, new BaseType(TypeKind.INT, null), "length", null);
	
	public MethodDecl decorate(Package prog){
		table = new ScopedIdentificationTable();
		prog.visit(this, table);
		return this.checkMainMethod(prog);
	}
	
	public MethodDecl checkMainMethod(Package prog){
		int numMainMethods = 0;
		MethodDecl mainMethod = null;
		
		for(int i = 0; i < prog.classDeclList.size(); i++){
			for(int j = 0; j < prog.classDeclList.get(i).methodDeclList.size(); j++){
				MethodDecl currentDecl = prog.classDeclList.get(i).methodDeclList.get(j);
				if(currentDecl.isPrivate == false && currentDecl.isStatic == true && currentDecl.type.typeKind == TypeKind.VOID
				&& currentDecl.name.equals("main") && currentDecl.parameterDeclList.size() == 1){
					if(currentDecl.parameterDeclList.get(0).type instanceof ArrayType){
						if(((ArrayType)currentDecl.parameterDeclList.get(0).type).eltType instanceof ClassType){
							if(((ClassType)((ArrayType)currentDecl.parameterDeclList.get(0).type).eltType).className.decl == table.stringClass){
								mainMethod = currentDecl;
								numMainMethods++;
							}
						}
					}
				}
			}
		}

		if(numMainMethods == 0){
			throw new NoMainMethodFoundException();
		}
		else if(numMainMethods > 1){
			throw new MultipleMainMethodFoundException();
		}
		return mainMethod;
	}

	public Object visitPackage(Package prog, ScopedIdentificationTable table) {
		for(int i = 0; i < prog.classDeclList.size(); i++){
			table.addDeclaration(prog.classDeclList.get(i));
			for(int j = 0; j < prog.classDeclList.get(i).fieldDeclList.size(); j++){
				prog.classDeclList.get(i).memberMap.put(prog.classDeclList.get(i).fieldDeclList.get(j).name, prog.classDeclList.get(i).fieldDeclList.get(j));
			}
			for(int j = 0; j < prog.classDeclList.get(i).methodDeclList.size(); j++){
				prog.classDeclList.get(i).memberMap.put(prog.classDeclList.get(i).methodDeclList.get(j).name, prog.classDeclList.get(i).methodDeclList.get(j));
			}
		}
		for(int i = 0; i < prog.classDeclList.size(); i++){
			table.currentClassDecl = prog.classDeclList.get(i);
			prog.classDeclList.get(i).visit(this, table);
		}
		return null;
	}

	public Object visitClassDecl(ClassDecl cd, ScopedIdentificationTable table) {
		table.pushScope(false, false);
		for(int i = 0; i < cd.fieldDeclList.size(); i++){
			table.addDeclaration(cd.fieldDeclList.get(i));
		}
		for(int i = 0; i < cd.methodDeclList.size(); i++){
			table.addDeclaration(cd.methodDeclList.get(i));
		}
		for(int i = 0; i < cd.fieldDeclList.size(); i++){
			cd.fieldDeclList.get(i).visit(this, table);
		}
		for(int i = 0; i < cd.methodDeclList.size(); i++){
			cd.methodDeclList.get(i).visit(this, table);
		}
		table.popScope();
		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, ScopedIdentificationTable table) {
		fd.type.visit(this, table);
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, ScopedIdentificationTable table) {
		table.pushScope(md.isStatic, md.isPrivate);
		md.type.visit(this, table);
		for(int i = 0; i < md.parameterDeclList.size(); i++){
			table.addDeclaration(md.parameterDeclList.get(i));
		}
		for(int i = 0; i < md.parameterDeclList.size(); i++){
			md.parameterDeclList.get(i).visit(this, table);
		}
		for(int i = 0; i < md.statementList.size(); i++){
			md.statementList.get(i).visit(this, table);
		}
		table.popScope();
		return null;
	}

	public Object visitParameterDecl(ParameterDecl pd, ScopedIdentificationTable table) {
		pd.type.visit(this, table);
		return null;
	}

	public Object visitVarDecl(VarDecl decl, ScopedIdentificationTable table) {
		table.addDeclaration(decl);
		decl.type.visit(this, table);
		return null;
	}

	public Object visitBaseType(BaseType type, ScopedIdentificationTable table) {
		return null;
	}

	public Object visitClassType(ClassType type, ScopedIdentificationTable table) {
		if(table.stack.get(1).hashMap.containsKey(type.className.spelling)){
			type.className.decl = table.stack.get(1).hashMap.get(type.className.spelling);
		}
		else if(table.stack.get(0).hashMap.containsKey(type.className.spelling)){
			type.className.decl = table.stack.get(0).hashMap.get(type.className.spelling);
		}
		else{
			throw new ClassNotFoundException(type.posn);
		}
		return null;
	}

	public Object visitArrayType(ArrayType type, ScopedIdentificationTable table) {
		type.eltType.visit(this, table);
		return null;
	}

	public Object visitBlockStmt(BlockStmt stmt, ScopedIdentificationTable table) {
		table.pushScope(table.getScope().isStatic, table.getScope().isPrivate);
		for(int i = 0; i < stmt.sl.size(); i++){
			stmt.sl.get(i).visit(this, table);
		}
		table.popScope();
		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, ScopedIdentificationTable table) {
		stmt.varDecl.visit(this, table);
		table.badVarDecl = stmt.varDecl;
		stmt.initExp.visit(this, table);
		table.badVarDecl = null;
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, ScopedIdentificationTable table) {
		if(stmt.ref instanceof ThisRef){
			throw new ThisAssignmentException(stmt.ref.posn);
		}
		stmt.ref.visit(this, table);
		if(stmt.ref.decl == lengthDecl){
			throw new AssigningArrayLengthException(stmt.posn);
		}
		stmt.val.visit(this, table);
		return null;
	}

	public Object visitCallStmt(CallStmt stmt, ScopedIdentificationTable table) {
		stmt.methodRef.visit(this, table);
		for(int i = 0; i < stmt.argList.size(); i++){
			stmt.argList.get(i).visit(this, table);
		}
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, ScopedIdentificationTable table) {
		if(stmt.returnExpr != null){
			stmt.returnExpr.visit(this, table);
		}
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, ScopedIdentificationTable table) {
		stmt.cond.visit(this, table);
		if(stmt.thenStmt instanceof VarDeclStmt){
			throw new SingleVarDeclException(stmt.thenStmt.posn);
		}
		stmt.thenStmt.visit(this, table);
		if(stmt.elseStmt != null){
			if(stmt.elseStmt instanceof VarDeclStmt){
				throw new SingleVarDeclException(stmt.elseStmt.posn);
			}
			stmt.elseStmt.visit(this, table);
		}
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, ScopedIdentificationTable table) {
		stmt.cond.visit(this, table);
		stmt.body.visit(this, table);
		if(stmt.body instanceof VarDeclStmt){
			throw new SingleVarDeclException(stmt.body.posn);
		}
		return null;
	}

	public Object visitUnaryExpr(UnaryExpr expr, ScopedIdentificationTable table) {
		expr.expr.visit(this, table);
		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, ScopedIdentificationTable table) {
		expr.left.visit(this, table);
		expr.right.visit(this, table);
		return null;
	}

	public Object visitRefExpr(RefExpr expr, ScopedIdentificationTable table) {
		expr.ref.visit(this, table);
		return null;
	}

	public Object visitCallExpr(CallExpr expr, ScopedIdentificationTable table) {
		expr.functionRef.visit(this, table);
		for(int i = 0; i < expr.argList.size(); i++){
			expr.argList.get(i).visit(this, table);
		}
		return null;
	}

	public Object visitLiteralExpr(LiteralExpr expr, ScopedIdentificationTable table) {
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, ScopedIdentificationTable table) {
		expr.classtype.visit(this, table);
		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, ScopedIdentificationTable table) {
		expr.eltType.visit(this, table);
		expr.sizeExpr.visit(this, table);
		return null;
	}

	public Object visitThisRef(ThisRef ref, ScopedIdentificationTable table) {
		if(table.getScope().isStatic){
			throw new NonstaticAccessFromStaticException(ref.posn);
		}
		ref.decl = table.currentClassDecl;
		return null;
	}

	public Object visitIdRef(IdRef ref, ScopedIdentificationTable table) {
		ref.decl = table.getDeclaration(ref.id.spelling, ref);
		if(table.getScope().isStatic){
			if(ref.decl instanceof MemberDecl){
				if(!((MemberDecl)ref.decl).isStatic){
					throw new NonstaticAccessFromStaticException(ref.posn);
				}
			}
		}
		return null;
	}

	public Object visitIxIdRef(IxIdRef ref, ScopedIdentificationTable table) {
		ref.indexExpr.visit(this, table);
		ref.decl = table.getDeclaration(ref.id.spelling, ref);
		return null;
	}

	public Object visitQRef(QRef ref, ScopedIdentificationTable table) {
		ref.ref.visit(this, table);
		
		//if the ref.ref is a methodDecl theres a problem
		if(ref.ref.decl instanceof MethodDecl){
			throw new MethodFieldException(ref.ref.posn);
		}
		
		if(ref.id.spelling.equals("length") && ref.ref.decl.type instanceof ArrayType){
			ref.decl = lengthDecl;
		}
		else if(ref.ref instanceof IxQRef && ref.ref.decl.type instanceof ArrayType){
			if(((ArrayType)ref.ref.decl.type).eltType.typeKind == TypeKind.CLASS){
				ref.decl = table.getDeclInClass(((ClassType)((ArrayType)ref.ref.decl.type).eltType).className.spelling, ref.id.spelling, ref.ref);
			}
			else{
				throw new ClassNotFoundException(ref.ref.posn);
			}
		}
		else if(ref.ref instanceof ThisRef && ref.ref.decl instanceof ClassDecl){
			if(((ClassDecl)ref.ref.decl).memberMap.containsKey(ref.id.spelling)){
				ref.decl = table.getDeclInClass(ref.ref.decl.name, ref.id.spelling, ref.ref);
			}
			else{
				throw new MemberNotDefinedException(ref.posn);
			}
		}
		else if(ref.ref.decl instanceof ClassDecl){
			if(((ClassDecl)ref.ref.decl).memberMap.containsKey(ref.id.spelling)){
				if(((MemberDecl)((ClassDecl)ref.ref.decl).memberMap.get(ref.id.spelling)).isStatic){
					ref.decl = table.getDeclInClass(ref.ref.decl.name, ref.id.spelling, ref.ref);
				}
				else{
					throw new NonstaticAccessFromStaticException(ref.posn);
				}
			}
			else{
				throw new MemberNotDefinedException(ref.posn);
			}
		}
		else if(ref.ref.decl.type instanceof ClassType){
			ref.decl = table.getDeclInClass(((ClassType)ref.ref.decl.type).className.spelling, ref.id.spelling, ref.ref);
		}
		//****************new addition due to pass330
		else if(ref.ref instanceof IxIdRef && ref.ref.decl.type instanceof ArrayType){
			if(((ArrayType)ref.ref.decl.type).eltType.typeKind == TypeKind.CLASS){
				ref.decl = table.getDeclInClass(((ClassType)((ArrayType)ref.ref.decl.type).eltType).className.spelling, ref.id.spelling, ref.ref);
			}
			else{
				throw new ClassNotFoundException(ref.ref.posn);
			}
		}
		else{
			throw new InvalidClassException(ref.ref.posn);
		}
		return null;
	}

	public Object visitIxQRef(IxQRef ref, ScopedIdentificationTable table) {
		ref.ixExpr.visit(this, table);
		ref.ref.visit(this, table);
		if(ref.ref instanceof IxQRef && ref.ref.decl.type instanceof ArrayType){
			if(((ArrayType)ref.ref.decl.type).eltType.typeKind == TypeKind.CLASS){
				ref.decl = table.getDeclInClass(((ClassType)((ArrayType)ref.ref.decl.type).eltType).className.spelling, ref.id.spelling, ref.ref);
			}
			else{
				throw new ClassNotFoundException(ref.ref.posn);
			}
		}
		else if(ref.ref instanceof ThisRef && ref.ref.decl instanceof ClassDecl){
			if(((ClassDecl)ref.ref.decl).memberMap.containsKey(ref.id.spelling)){
				ref.decl = table.getDeclInClass(ref.ref.decl.name, ref.id.spelling, ref.ref);
			}
			else{
				throw new MemberNotDefinedException(ref.posn);
			}
		}
		else if(ref.ref.decl instanceof ClassDecl){
			if(((ClassDecl)ref.ref.decl).memberMap.containsKey(ref.id.spelling)){
				if(((MemberDecl)((ClassDecl)ref.ref.decl).memberMap.get(ref.id.spelling)).isStatic){
					ref.decl = table.getDeclInClass(ref.ref.decl.name, ref.id.spelling, ref.ref);
				}
				else{
					throw new NonstaticAccessFromStaticException(ref.posn);
				}
			}
			else{
				throw new MemberNotDefinedException(ref.posn);
			}
		}
		else if(ref.ref.decl.type instanceof ClassType){
			ref.decl = table.getDeclInClass(((ClassType)ref.ref.decl.type).className.spelling, ref.id.spelling, ref.ref);
		}
		else{
			throw new InvalidClassException(ref.ref.posn);
		}
		return null;
	}

	public Object visitIdentifier(Identifier id, ScopedIdentificationTable table) {
		id.decl = table.getDeclaration(id.spelling, null);
		return null;
	}

	public Object visitOperator(Operator op, ScopedIdentificationTable table) {
		return null;
	}

	public Object visitIntLiteral(IntLiteral num, ScopedIdentificationTable table) {
		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, ScopedIdentificationTable table) {
		return null;
	}

	public Object visitNullLiteral(NullLiteral nu, ScopedIdentificationTable table) {
		return null;
	}

}
