package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.AST;
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
import miniJava.Errors.NoReturnStmtException;
import miniJava.Errors.ParameterNumberException;

public class ASTTyper implements Visitor<Object,Object>{
	public Errors errors = new Errors();
	
	public Type checkEquality(Type typeA, Type typeB, AST ast){
		if(typeA.type == TypeKind.UNSUPPORTED || typeB.type == TypeKind.UNSUPPORTED){
			errors.addError("unsupported type encountered", ast.posn);
			return new Type(TypeKind.ERROR, null, null);
		}
		else if(typeA.type == TypeKind.NULL || typeB.type == TypeKind.NULL){
			if(typeA.type == TypeKind.NULL && typeB.type == TypeKind.CLASS){
				return new Type(TypeKind.NULL, null, null);
			}
			else if(typeB.type == TypeKind.NULL && typeA.type == TypeKind.CLASS){
				return new Type(TypeKind.NULL, null, null);
			}
			else{
				return new Type(TypeKind.NULL, null, null);
			}
		}
		else if(typeA.type == typeB.type){
			if(typeA.type == TypeKind.ERROR && typeB.type == TypeKind.ERROR){
				return new Type(TypeKind.ERROR, null, null);
			}
			else if(typeA.type == TypeKind.CLASS){
				if(typeA.decl == typeB.decl){
					return typeA;
				}
				else{
					addErrorToList(typeA, typeB, ast);
					return new Type(TypeKind.ERROR, null, null);
				}
			}
			else if(typeA.type == TypeKind.ARRAY){
				if(typeA.elmType.type == typeB.elmType.type){
					if(typeA.elmType.type == TypeKind.CLASS){
						if(typeA.elmType.decl == typeB.elmType.decl){
							return typeA;
						}
						else{
							addErrorToList(typeA.elmType, typeB.elmType, ast);
							return new Type(TypeKind.ERROR, null, null);
						}
					}
					else{
						return typeA;
					}
				}
				else{
					addErrorToList(typeA.elmType, typeB.elmType, ast);
					return new Type(TypeKind.ERROR, null, null);
				}
			}
			else{
				return typeA;
			}
		}
		else if(typeA.type == TypeKind.ERROR){
			return typeB;
		}
		else if(typeB.type == TypeKind.ERROR){
			return typeA;
		}
		else{
			addErrorToList(typeA, typeB, ast);
			return new Type(TypeKind.ERROR, null, null);
		}
	}
	
	public void addErrorToList(Type typeA, Type typeB, AST ast){
		if(typeA.type == TypeKind.CLASS && typeB.type == TypeKind.CLASS){
			errors.addError(typeA.decl.name + " compared to " + typeB.decl.name, ast.posn);
		}
		else if (typeA.type != TypeKind.CLASS && typeB.type == TypeKind.CLASS){
			errors.addError(typeA.type.toString() + " compared to " + typeB.decl.name, ast.posn);
		}
		else if(typeA.type == TypeKind.CLASS && typeB.type != TypeKind.CLASS){
			errors.addError(typeA.decl.name + " compared to " + typeB.type.toString(), ast.posn);
		}
		else{
			errors.addError(typeA.type.toString() + " compared to " + typeB.type.toString(), ast.posn);
		}
	}
	
	public Errors typeCheck(Package prog){
		prog.visit(this, null);
		return this.errors;
	}
	
	public Type visitPackage(Package prog, Object arg) {
		for(int i = 0; i < prog.classDeclList.size(); i++){
			prog.classDeclList.get(i).visit(this, null);
		}
		return new Type(TypeKind.UNSUPPORTED, null, null);
	}

	public Type visitClassDecl(ClassDecl cd, Object arg) {
		for(int i = 0; i < cd.fieldDeclList.size(); i++){
			cd.fieldDeclList.get(i).visit(this, null);
		}
		for(int i = 0; i < cd.methodDeclList.size(); i++){
			cd.methodDeclList.get(i).visit(this, null);
		}
		return new Type(TypeKind.CLASS, cd, null);
	}

	public Type visitFieldDecl(FieldDecl fd, Object arg) {
		return (Type) fd.type.visit(this, null);
	}

	public Type visitMethodDecl(MethodDecl md, Object arg) {
		for(int i = 0; i < md.parameterDeclList.size(); i++){
			md.parameterDeclList.get(i).visit(this, null);
		}
		Type typeA = null;
		Type typeB = null;
		if(md.type.typeKind == TypeKind.CLASS){
			typeB = new Type(TypeKind.CLASS, ((ClassType)md.type).className.decl, null);
		}
		else if(md.type.typeKind == TypeKind.ARRAY){
			if(((ArrayType)md.type).eltType.typeKind == TypeKind.CLASS){
				typeB = new Type(TypeKind.ARRAY, null, new Type(TypeKind.CLASS, ((ClassType)((ArrayType)md.type).eltType).className.decl, null));
			}
			else{
				typeB = new Type(TypeKind.ARRAY, null, new Type(((ArrayType)md.type).eltType.typeKind, null, null));
			}
		}
		else{
			typeB = new Type(md.type.typeKind, null, null);
		}
		int returnStmtNum = 0;
		for(int i = 0; i < md.statementList.size(); i++){
			Type stmtType = (Type) md.statementList.get(i).visit(this,  null);
			if(md.statementList.get(i) instanceof ReturnStmt){
				typeA = stmtType;
				returnStmtNum = i;
			}
		}
		if(typeA != null){
			checkEquality(typeA, typeB, md.statementList.get(returnStmtNum));
		}
		else{
			if(md.type.typeKind != TypeKind.VOID){
				throw new NoReturnStmtException(md.posn);
			}
			else{
				md.statementList.add(new ReturnStmt(null, null));
			}
		}
		return (Type) md.type.visit(this, null);
	}

	public Type visitParameterDecl(ParameterDecl pd, Object arg) {
		return (Type) pd.type.visit(this, null);
	}

	public Type visitVarDecl(VarDecl decl, Object arg) {
		return (Type) decl.type.visit(this, null);
	}

	public Type visitBaseType(BaseType type, Object arg) {
		return new Type(type.typeKind, null, null);
	}

	public Type visitClassType(ClassType type, Object arg) {
		if(type.className.spelling.equals("String")){
			return new Type(TypeKind.UNSUPPORTED, null, null);
		}
		else{
			return new Type(TypeKind.CLASS, type.className.decl, null);
		}
	}

	public Type visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		if(type.eltType.typeKind == TypeKind.CLASS){
			return new Type(TypeKind.ARRAY, null, new Type(type.eltType.typeKind, ((ClassType)type.eltType).className.decl, null));
		}
		else{
			return new Type(TypeKind.ARRAY, null, new Type(type.eltType.typeKind, null, null));
		}
	}

	public Type visitBlockStmt(BlockStmt stmt, Object arg) {
		for(int i = 0; i < stmt.sl.size(); i++){
			stmt.sl.get(i).visit(this, null);
		}
		return new Type(TypeKind.UNSUPPORTED, null, null);
	}

	public Type visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		Type typeA = (Type) stmt.varDecl.visit(this, null);
		Type typeB = (Type) stmt.initExp.visit(this, null);
		return checkEquality(typeA, typeB, stmt.varDecl);
	}

	public Type visitAssignStmt(AssignStmt stmt, Object arg) {
		Type typeA = (Type) stmt.ref.visit(this, null);
		Type typeB = (Type) stmt.val.visit(this, null);
		return checkEquality(typeA, typeB, stmt.ref);
	}

	public Type visitCallStmt(CallStmt stmt, Object arg) {
		/*
		for(int i = 0; i < stmt.argList.size(); i++){
			stmt.argList.get(i).visit(this, null);
		}
		//return (Type) stmt.methodRef.visit(this, null);
		*/
		if(stmt.argList.size() != ((MethodDecl)stmt.methodRef.decl).parameterDeclList.size()){
			throw new ParameterNumberException(stmt.methodRef.posn);
		}
		for(int i = 0; i < stmt.argList.size(); i++){
			Type typeA = (Type) stmt.argList.get(i).visit(this, null);
			Type typeB = (Type)((MethodDecl)stmt.methodRef.decl).parameterDeclList.get(i).visit(this, null);
			checkEquality(typeA, typeB, stmt.argList.get(i));
		}
		return (Type) stmt.methodRef.visit(this, null);
	}

	public Type visitReturnStmt(ReturnStmt stmt, Object arg) {
		if(stmt.returnExpr != null){
			return (Type) stmt.returnExpr.visit(this, null);
		}
		else{
			return new Type(TypeKind.VOID, null, null);
		}
	}

	public Type visitIfStmt(IfStmt stmt, Object arg) {
		Type typeA = (Type) stmt.cond.visit(this, null);
		Type typeB = new Type(TypeKind.BOOLEAN, null, null);
		checkEquality(typeA, typeB, stmt.cond);
		if(stmt.thenStmt != null){
			stmt.thenStmt.visit(this, null);
		}
		if(stmt.elseStmt != null){
			stmt.elseStmt.visit(this, null);
		}
		return new Type(TypeKind.UNSUPPORTED, null, null);
	}

	public Type visitWhileStmt(WhileStmt stmt, Object arg) {
		Type typeA = (Type) stmt.cond.visit(this, null);
		Type typeB = new Type(TypeKind.BOOLEAN, null, null);
		checkEquality(typeA, typeB, stmt.cond);
		stmt.body.visit(this, null);
		return new Type(TypeKind.UNSUPPORTED, null, null);
	}

	public Type visitUnaryExpr(UnaryExpr expr, Object arg) {
		if(expr.operator.spelling.equals("!")){
			Type typeA = (Type) expr.expr.visit(this, null);
			Type typeB = new Type(TypeKind.BOOLEAN, null, null);
			return checkEquality(typeA, typeB, expr.expr);
		}
		else{
			Type typeA = (Type) expr.expr.visit(this, null);
			Type typeB = new Type(TypeKind.INT, null, null);
			return checkEquality(typeA, typeB, expr.expr);
		}
	}

	public Type visitBinaryExpr(BinaryExpr expr, Object arg) {
		Type typeA = (Type) expr.left.visit(this, null);
		Type typeB = (Type) expr.right.visit(this, null);
		Type typeC = checkEquality(typeA, typeB, expr.left);
		if(expr.operator.spelling.equals("==") || expr.operator.spelling.equals("!=")){
			if(typeC.type == TypeKind.INT){
				return new Type(TypeKind.BOOLEAN, null, null);
			}
			else{
				typeB = new Type(TypeKind.BOOLEAN, null, null);
				return checkEquality(typeC, typeB, expr.left);
			}
		}
		else if(expr.operator.spelling.equals("&&") || expr.operator.spelling.equals("||")){
			typeB = new Type(TypeKind.BOOLEAN, null, null);
			return checkEquality(typeC, typeB, expr.left);
		}
		else if(expr.operator.spelling.equals("<=") || expr.operator.spelling.equals(">=") ||
				expr.operator.spelling.equals("<") || expr.operator.spelling.equals(">")){
			typeB = new Type(TypeKind.INT, null, null);
			if(checkEquality(typeC, typeB, expr.left).type == TypeKind.INT){
				return new Type(TypeKind.BOOLEAN, null, null);
			}
			else{
				//add error to list 
				//*****************************
				addErrorToList(typeC, typeB, expr.left);
				return new Type(TypeKind.ERROR, null, null);
			}
		}
		else{
			typeB = new Type(TypeKind.INT, null, null);
			return checkEquality(typeC, typeB, expr.left);
		}
	}

	public Type visitRefExpr(RefExpr expr, Object arg) {
		return (Type) expr.ref.visit(this, null);
	}

	public Type visitCallExpr(CallExpr expr, Object arg) {
		if(expr.argList.size() != ((MethodDecl)expr.functionRef.decl).parameterDeclList.size()){
			throw new ParameterNumberException(expr.functionRef.posn);
		}
		for(int i = 0; i < expr.argList.size(); i++){
			Type typeA = (Type) expr.argList.get(i).visit(this, null);
			Type typeB = (Type)((MethodDecl)expr.functionRef.decl).parameterDeclList.get(i).visit(this, null);
			checkEquality(typeA, typeB, expr.argList.get(i));
		}
		return (Type) expr.functionRef.visit(this, null);
	}

	public Type visitLiteralExpr(LiteralExpr expr, Object arg) {
		return (Type) expr.lit.visit(this, null);
	}

	public Type visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		return (Type) expr.classtype.visit(this, null);
	}

	public Type visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		Type typeA = (Type) expr.sizeExpr.visit(this, null);
		Type typeB = new Type(TypeKind.INT, null, null);
		checkEquality(typeA, typeB, expr.sizeExpr);
		//expr.eltType.visit(this, null);
		return new Type(TypeKind.ARRAY, null, (Type) expr.eltType.visit(this, null));
		/*
		if(expr.eltType.typeKind == TypeKind.CLASS){
			return new Type(TypeKind.ARRAY, null, new Type(expr.eltType.typeKind, ((ClassType)expr.eltType).className.decl, null));
		}
		else{
			return new Type(TypeKind.ARRAY, null, new Type(expr.eltType.typeKind, null, null));
		}
		*/
	}

	public Type visitThisRef(ThisRef ref, Object arg) {
		return new Type(TypeKind.CLASS, ref.decl, null);
	}

	public Type visitIdRef(IdRef ref, Object arg) {
		if(ref.decl.type.typeKind == TypeKind.CLASS){
			return new Type(ref.decl.type.typeKind, ((ClassType)ref.decl.type).className.decl, null);
		}
		else if(ref.decl.type.typeKind == TypeKind.ARRAY){
			if(((ArrayType)ref.decl.type).eltType.typeKind == TypeKind.CLASS){
				return new Type(TypeKind.ARRAY, null, new Type(TypeKind.CLASS, ((ClassType)((ArrayType)ref.decl.type).eltType).className.decl, null));
			}
			else{
				return new Type(TypeKind.ARRAY, null, new Type(((ArrayType)ref.decl.type).eltType.typeKind, null, null));
			}
		}
		else{
			return new Type(ref.decl.type.typeKind, null, null);
		}
	}

	public Type visitIxIdRef(IxIdRef ref, Object arg) {
		Type typeA = (Type) ref.indexExpr.visit(this, null);
		Type typeB = new Type(TypeKind.INT, null, null);
		checkEquality(typeA, typeB, ref.indexExpr);
		//check if array
		//visit element return type 
		/*
		if(((ArrayType)ref.decl.type).eltType.typeKind == TypeKind.CLASS){
			return new Type(TypeKind.ARRAY, null, new Type(TypeKind.CLASS, ((ClassType)((ArrayType)ref.decl.type).eltType).className.decl, null));
		}
		else{
			return new Type(TypeKind.ARRAY, null, new Type(((ArrayType)ref.decl.type).eltType.typeKind, null, null));
		}
		*/
		//return new Type(((ArrayType)ref.decl.type).eltType.typeKind, null, null);
		
		Type typeC = (Type) ref.decl.type.visit(this, null);
		if(typeC.type == TypeKind.ARRAY){
			return typeC.elmType;
		}
		else{
			
			return typeC;
		}
	}

	public Type visitQRef(QRef ref, Object arg) {
		return (Type) ref.decl.type.visit(this, null);
	}

	public Type visitIxQRef(IxQRef ref, Object arg) {
		Type typeA = (Type) ref.ixExpr.visit(this, null);
		Type typeB = new Type(TypeKind.INT, null, null);
		checkEquality(typeA, typeB, ref.ixExpr);
		
		Type typeC = (Type) ref.decl.type.visit(this, null);
		if(typeC.type == TypeKind.ARRAY){
			return typeC.elmType;
		}
		else{
			return typeC;
		}
	}
	
	public Type visitIdentifier(Identifier id, Object arg) {
		return new Type(id.decl.type.typeKind, id.decl, null);
	}

	public Type visitOperator(Operator op, Object arg) {
		return new Type(TypeKind.UNSUPPORTED, null, null);
	}

	public Type visitIntLiteral(IntLiteral num, Object arg) {
		return new Type(TypeKind.INT, null, null);
	}

	public Type visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return new Type(TypeKind.BOOLEAN, null, null);
	}

	public Type visitNullLiteral(NullLiteral nu, Object arg) {
		return new Type(TypeKind.NULL, null, null);
	}

}
