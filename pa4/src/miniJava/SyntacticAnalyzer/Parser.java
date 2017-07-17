package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {
	private Scanner scanner;
	private ErrorReporter errorReporter;
	private Token currentToken;
	private SourcePosition previousTokenPosition;
	
	public Parser(Scanner scan, ErrorReporter reporter) {
		scanner = scan;
	    errorReporter = reporter;
	    previousTokenPosition = new SourcePosition();
	}
	
	void accept (TokenKind tokenExpected) throws SyntaxError {
		if (currentToken.kind == tokenExpected) {
			previousTokenPosition = currentToken.position;
			currentToken = scanner.scan();
	    } else {
	    	syntacticError("\"%\" expected here", tokenExpected);
	    }
	}

	void acceptIt() {
	    previousTokenPosition = currentToken.position;
	    currentToken = scanner.scan();
	}
	
	void start(SourcePosition position) {
	    position.start = currentToken.position.start;
	}
	
	void finish(SourcePosition position) {
	    position.finish = previousTokenPosition.finish;
	}
	
	void syntacticError(String messageTemplate, TokenKind tokenQuoted) throws SyntaxError {
	    SourcePosition pos = currentToken.position;
	    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
	    throw(new SyntaxError());
	}
	
	public Package parseProgram() {
	    previousTokenPosition.start = 0;
	    previousTokenPosition.finish = 0;
	    currentToken = scanner.scan();
	    Token startToken = currentToken;
	    ClassDeclList classList = new ClassDeclList();
	    try{
	    	while(currentToken.kind == TokenKind.CLASS){
	    		ClassDecl classDec = parseClassDeclaration();
	    		classList.add(classDec);  
	    	}
	    	accept(TokenKind.EOT);
	    }
	    catch(SyntaxError error){
	    	return null;
	    }
	    return new Package(classList, startToken.position);
	}
	
	public ClassDecl parseClassDeclaration() throws SyntaxError {
		Token startToken = currentToken;
		accept(TokenKind.CLASS);
		String classString = currentToken.spelling.toString();
		accept(TokenKind.IDENTIFIER);
		accept(TokenKind.L_CURL);
		MethodDeclList methodList = new MethodDeclList();
		FieldDeclList fieldList = new FieldDeclList();
		ClassDecl classDec = new ClassDecl(classString, fieldList, methodList, startToken.position);
		if(currentToken.kind == TokenKind.R_CURL){
			acceptIt();
		}
		else{
			while(currentToken.kind == TokenKind.PRIVATE || currentToken.kind == TokenKind.PUBLIC || currentToken.kind == TokenKind.STATIC
					|| currentToken.kind == TokenKind.VOID || currentToken.kind == TokenKind.INT 
					|| currentToken.kind == TokenKind.IDENTIFIER || currentToken.kind == TokenKind.BOOLEAN){
				MemberDecl memberDec = parseDeclaration();
				if(memberDec instanceof FieldDecl){
					fieldList.add((FieldDecl)memberDec);
				}
				else if(memberDec instanceof MethodDecl){
					methodList.add((MethodDecl)memberDec);
				}
			}
			accept(TokenKind.R_CURL);
			classDec = new ClassDecl(classString, fieldList, methodList, startToken.position);
		}
		return classDec;
	}
	
	public MemberDecl parseDeclaration() throws SyntaxError{
		boolean privateBool = parseVisibility();
		boolean staticBool = parseAccess();
		MemberDecl dec;
		if(currentToken.kind == TokenKind.VOID){
			dec = parseVoidDeclaration(privateBool, staticBool);
		}
		else{
			dec = parseTypeDeclaration(privateBool, staticBool);
		}
		return dec;
	}
	
	public boolean parseVisibility() throws SyntaxError{
		if(currentToken.kind == TokenKind.PUBLIC){
			acceptIt();
		}
		else if(currentToken.kind == TokenKind.PRIVATE){
			acceptIt();
			return true;
		}
		return false;
	}
	
	public boolean parseAccess() throws SyntaxError{
		if(currentToken.kind == TokenKind.STATIC){
			acceptIt();
			return true;
		}
		return false;
	}
	
	public MemberDecl parseVoidDeclaration(boolean privateBool, boolean staticBool) throws SyntaxError{
		Token startToken = currentToken;
		acceptIt();
		String methodString = currentToken.spelling.toString();
		accept(TokenKind.IDENTIFIER);
		accept(TokenKind.L_PAREN);
		ParameterDeclList parameterList = new ParameterDeclList();
		StatementList statementList = new StatementList();
		if(currentToken.kind == TokenKind.INT || currentToken.kind == TokenKind.BOOLEAN || currentToken.kind == TokenKind.IDENTIFIER){
			parameterList = parseParameterList();
			accept(TokenKind.R_PAREN);
		}
		else{
			accept(TokenKind.R_PAREN);
		}
		accept(TokenKind.L_CURL);
		while(currentToken.kind == TokenKind.L_CURL || currentToken.kind == TokenKind.IF || currentToken.kind == TokenKind.WHILE || 
				currentToken.kind == TokenKind.INT || currentToken.kind == TokenKind.BOOLEAN || currentToken.kind == TokenKind.RETURN || 
				currentToken.kind == TokenKind.THIS || currentToken.kind == TokenKind.IDENTIFIER){
			Statement statement = parseStatement();
			statementList.add(statement);
		}
		accept(TokenKind.R_CURL);
		TypeDenoter type = new BaseType(TypeKind.VOID, startToken.position);
		MemberDecl memberDec = new FieldDecl(privateBool, staticBool, type, methodString, startToken.position);
		return new MethodDecl(memberDec, parameterList, statementList, startToken.position);
	}
	
	public MemberDecl parseTypeDeclaration(boolean privateBool, boolean staticBool) throws SyntaxError{
		Token startToken = currentToken;
		TypeDenoter type = parseType();
		String methodString = currentToken.spelling.toString();
		accept(TokenKind.IDENTIFIER);
		ParameterDeclList parameterList = new ParameterDeclList();
		StatementList statementList = new StatementList();
		if(currentToken.kind == TokenKind.L_PAREN){
			acceptIt();
			if(currentToken.kind == TokenKind.INT || currentToken.kind == TokenKind.BOOLEAN || currentToken.kind == TokenKind.IDENTIFIER){
				parameterList = parseParameterList();
				accept(TokenKind.R_PAREN);
			}
			else{
				accept(TokenKind.R_PAREN);
			}
			accept(TokenKind.L_CURL);
			while(currentToken.kind == TokenKind.L_CURL || currentToken.kind == TokenKind.IF || currentToken.kind == TokenKind.WHILE || 
					currentToken.kind == TokenKind.INT || currentToken.kind == TokenKind.BOOLEAN || currentToken.kind == TokenKind.RETURN || 
					currentToken.kind == TokenKind.THIS || currentToken.kind == TokenKind.IDENTIFIER){
				Statement statement = parseStatement();
				statementList.add(statement);
			}
			accept(TokenKind.R_CURL);
		}
		else{
			accept(TokenKind.SEMI_COLON);
			return new FieldDecl(privateBool, staticBool, type, methodString, startToken.position);
		}
		MemberDecl memberDec = new FieldDecl(privateBool, staticBool, type, methodString, startToken.position);
		return new MethodDecl(memberDec, parameterList, statementList, startToken.position);
	}
	
	public TypeDenoter parseType() throws SyntaxError{
		Token startToken = currentToken;
		TypeDenoter type;
		if(currentToken.kind == TokenKind.INT){
			acceptIt();
			if(currentToken.kind == TokenKind.L_BRACK){
				acceptIt();
				accept(TokenKind.R_BRACK);
				type = new BaseType(TypeKind.INT, currentToken.position);
				return new ArrayType(type, startToken.position);
			}
			return new BaseType(TypeKind.INT, startToken.position);
		}
		else if(currentToken.kind == TokenKind.IDENTIFIER){
			Token idToken = currentToken;
			acceptIt();
			if(currentToken.kind == TokenKind.L_BRACK){
				acceptIt();
				accept(TokenKind.R_BRACK);
				type = new ClassType(new Identifier(idToken), startToken.position);
				return new ArrayType(type, startToken.position);
			}
			return new ClassType(new Identifier(idToken), startToken.position);
		}
		else{
			accept(TokenKind.BOOLEAN);
		}
		return new BaseType(TypeKind.BOOLEAN, startToken.position);
	}
	
	public ParameterDeclList parseParameterList() throws SyntaxError{
		Token startToken = currentToken;
		TypeDenoter type = parseType();
		String parameterString = currentToken.spelling.toString();
		accept(TokenKind.IDENTIFIER);
		ParameterDeclList parameterList = new ParameterDeclList();
		ParameterDecl parameter = new ParameterDecl(type, parameterString, startToken.position);
		parameterList.add(parameter);
		while(currentToken.kind == TokenKind.COMMA){
			acceptIt();
			startToken = currentToken;
			type = parseType();
			parameterString = currentToken.spelling.toString();
			accept(TokenKind.IDENTIFIER);
			parameter = new ParameterDecl(type, parameterString, startToken.position);
			parameterList.add(parameter);
		}
		return parameterList;
	}
	
	public ExprList parseArgumentList() throws SyntaxError{
		Expression expression = parseExpression();
		ExprList list = new ExprList();
		list.add(expression);
		while(currentToken.kind == TokenKind.COMMA){
			acceptIt();
			expression = parseExpression();
			list.add(expression);
		}
		return list;
	}
	
	public QualifiedRef parseReferenceDot(Reference ref1) throws SyntaxError{
		boolean indexed = false;
		accept(TokenKind.PERIOD);
		Token refToken = currentToken;
		accept(TokenKind.IDENTIFIER);
		Expression expression = null;
		if(currentToken.kind == TokenKind.L_BRACK){
			acceptIt();
			expression = parseExpression();
			accept(TokenKind.R_BRACK);
			indexed = true;
		}
		if(indexed == true){
			return new IxQRef(ref1, new Identifier(refToken), expression, refToken.position);
		}
		else{
			return new QRef(ref1, new Identifier(refToken), refToken.position);
		}
	}
	
	public TypeDenoter parseTypeLiteral() throws SyntaxError{
		Token startToken = currentToken;
		if(currentToken.kind == TokenKind.INT){
			BaseType baseType = new BaseType(TypeKind.INT, startToken.position);
			acceptIt();
			if(currentToken.kind == TokenKind.L_BRACK){
				acceptIt();
				accept(TokenKind.R_BRACK);
				return new ArrayType(baseType, startToken.position);
			}
			return baseType;
		}
		else{
			accept(TokenKind.BOOLEAN);
			return new BaseType(TypeKind.BOOLEAN, startToken.position);
		}
	}
	
	public Reference parseReference() throws SyntaxError{
		//boolean loopVal = false;
		Reference ref; 
		Token startToken = currentToken;
		if(currentToken.kind == TokenKind.IDENTIFIER){
			acceptIt();
			//loopVal = true;
			if(currentToken.kind == TokenKind.L_BRACK){
				acceptIt();
				Expression expression = parseExpression();
				accept(TokenKind.R_BRACK);
				ref = new IxIdRef(new Identifier(startToken), expression, startToken.position);
			}
			else{
				ref = new IdRef(new Identifier(startToken), startToken.position);
			}
		}
		else{
			accept(TokenKind.THIS);
			//loopVal = true;
			ref = new ThisRef(startToken.position);
		}
		while(currentToken.kind == TokenKind.PERIOD){
			ref = parseReferenceDot(ref);
		}
		return ref;
		
		//possibly use parseReferenceDot? also what the heck is loopVal even doing??
		/*
		while(currentToken.kind == TokenKind.PERIOD && loopVal == true){
			acceptIt();
			accept(TokenKind.IDENTIFIER);
			if(currentToken.kind == TokenKind.L_BRACK){
				acceptIt();
				parseExpression();
				accept(TokenKind.R_BRACK);
			}
		}
		*/
	}
	
	public Expression parseExpression() throws SyntaxError{
		Expression expression = parseDisjunction();
		return expression;
		/*
		parseSubExpression();
		while(currentToken.kind == TokenKind.LESS || currentToken.kind == TokenKind.LESS_EQUAL || 
				currentToken.kind == TokenKind.GREATER || currentToken.kind == TokenKind.GREATER_EQUAL || 
				currentToken.kind == TokenKind.DOUBLE_EQUAL || currentToken.kind == TokenKind.NOT_EQUAL || 
				currentToken.kind == TokenKind.AND || currentToken.kind == TokenKind.OR || 
				currentToken.kind == TokenKind.SUBTRACTION || currentToken.kind == TokenKind.ADDITION || 
				currentToken.kind == TokenKind.MULTIPLICATION || currentToken.kind == TokenKind.DIVISION){
			acceptIt();
			parseExpression();
		}
		*/
	}
	
	public Expression parseDisjunction() throws SyntaxError{
		Token startToken = currentToken;
		Expression expression1 = parseConjunction();
		while(currentToken.kind == TokenKind.OR){
			Token operator = currentToken;
			acceptIt();
			Expression expression2 = parseConjunction();
			BinaryExpr binExpression = new BinaryExpr(new Operator(operator), expression1, expression2, startToken.position);
			expression1 = binExpression;
		}
		return expression1;
	}
	
	public Expression parseConjunction() throws SyntaxError{
		Token startToken = currentToken;
		Expression expression1 = parseEquality();
		while(currentToken.kind == TokenKind.AND){
			Token operator = currentToken;
			acceptIt();
			Expression expression2 = parseEquality();
			BinaryExpr binExpression = new BinaryExpr(new Operator(operator), expression1, expression2, startToken.position);
			expression1 = binExpression;
		}
		return expression1;
	}
	
	public Expression parseEquality() throws SyntaxError{
		Token startToken = currentToken;
		Expression expression1 = parseRelational();
		while(currentToken.kind == TokenKind.DOUBLE_EQUAL || currentToken.kind == TokenKind.NOT_EQUAL){
			Token operator = currentToken;
			acceptIt();
			Expression expression2 = parseRelational();
			BinaryExpr binExpression = new BinaryExpr(new Operator(operator), expression1, expression2, startToken.position);
			expression1 = binExpression;
		}
		return expression1;
	}
	
	public Expression parseRelational() throws SyntaxError{
		Token startToken = currentToken;
		Expression expression1 = parseAdditive();
		while(currentToken.kind == TokenKind.GREATER || currentToken.kind == TokenKind.GREATER_EQUAL 
				|| currentToken.kind == TokenKind.LESS || currentToken.kind == TokenKind.LESS_EQUAL){
			Token operator = currentToken;
			acceptIt();
			Expression expression2 = parseAdditive();
			BinaryExpr binExpression = new BinaryExpr(new Operator(operator), expression1, expression2, startToken.position);
			expression1 = binExpression;
		}
		return expression1;
	}
	
	public Expression parseAdditive() throws SyntaxError{
		Token startToken = currentToken;
		Expression expression1 = parseMultiplicative();
		while(currentToken.kind == TokenKind.ADDITION || currentToken.kind == TokenKind.SUBTRACTION){
			Token operator = currentToken;
			acceptIt();
			Expression expression2 = parseMultiplicative();
			BinaryExpr binExpression = new BinaryExpr(new Operator(operator), expression1, expression2, startToken.position);
			expression1 = binExpression;
		}
		return expression1;
	}
	
	public Expression parseMultiplicative() throws SyntaxError{
		Token startToken = currentToken;
		Expression expression1 = parseSubExpression();
		while(currentToken.kind == TokenKind.MULTIPLICATION || currentToken.kind == TokenKind.DIVISION){
			Token operator = currentToken;
			acceptIt();
			Expression expression2 = parseSubExpression();
			BinaryExpr binExpression = new BinaryExpr(new Operator(operator), expression1, expression2, startToken.position);
			expression1 = binExpression;
		}
		return expression1;
	}
	
	public ExprList parseCallMethod() throws SyntaxError{
		ExprList list = new ExprList();
		accept(TokenKind.L_PAREN);
		if(currentToken.kind != TokenKind.R_PAREN){
			list = parseArgumentList();
			accept(TokenKind.R_PAREN);
			return list;
		}
		else{
			accept(TokenKind.R_PAREN);
			return list;
		}
	}
	
	public Expression parseEqualsExpression() throws SyntaxError{
		accept(TokenKind.EQUAL);
		Expression expression = parseExpression();
		return expression;
	}
	
	public Expression parseSubExpression() throws SyntaxError{
		Token startToken = currentToken;
		if(currentToken.kind == TokenKind.IDENTIFIER || currentToken.kind == TokenKind.THIS){
			Reference ref = parseReference();
			//come back to this part, null vs empty list
			ExprList list = new ExprList();
			if(currentToken.kind == TokenKind.L_PAREN){
				acceptIt();
				if(currentToken.kind != TokenKind.R_PAREN){
					list = parseArgumentList();
				}
				accept(TokenKind.R_PAREN);
				return new CallExpr(ref, list, startToken.position);
			}
			return new RefExpr(ref, startToken.position);
		}
		else if(currentToken.kind == TokenKind.THIS){
			Reference ref = parseReference();
			return new RefExpr(ref, startToken.position);
		}
		else if(currentToken.kind == TokenKind.NOT || currentToken.kind == TokenKind.SUBTRACTION){
			Operator operator = new Operator(currentToken);
			acceptIt();
			Expression expression = parseSubExpression();
			return new UnaryExpr(operator, expression, startToken.position);
		}
		else if(currentToken.kind == TokenKind.L_PAREN){
			acceptIt();
			Expression expression = parseExpression();
			accept(TokenKind.R_PAREN);
			return expression;
		}
		else if(currentToken.kind == TokenKind.FALSE || currentToken.kind == TokenKind.TRUE){
			BooleanLiteral terminal = new BooleanLiteral(currentToken);
			acceptIt();
			return new LiteralExpr(terminal, startToken.position);
		}
		else if(currentToken.kind == TokenKind.NULL){
			NullLiteral terminal = new NullLiteral(currentToken);
			acceptIt();
			return new LiteralExpr(terminal, startToken.position);
		}
		else if(currentToken.kind == TokenKind.NEW){
			acceptIt();
			if(currentToken.kind == TokenKind.IDENTIFIER){
				Token classId = currentToken;
				acceptIt();
				if(currentToken.kind == TokenKind.L_PAREN){
					ClassType classType = new ClassType(new Identifier(classId), startToken.position);
					acceptIt();
					accept(TokenKind.R_PAREN);
					return new NewObjectExpr(classType, startToken.position);
				}
				else{
					ClassType classType = new ClassType(new Identifier(classId), startToken.position);
					accept(TokenKind.L_BRACK);
					Expression expression = parseExpression();
					accept(TokenKind.R_BRACK);
					return new NewArrayExpr(classType, expression, startToken.position);
				}
			}
			else{
				TypeDenoter type = new BaseType(TypeKind.INT, currentToken.position);
				accept(TokenKind.INT);
				accept(TokenKind.L_BRACK);
				Expression expression = parseExpression();
				accept(TokenKind.R_BRACK);
				return new NewArrayExpr(type, expression, startToken.position);
			}
		}
		else{
			IntLiteral terminal = new IntLiteral(currentToken);
			accept(TokenKind.NUM);
			return new LiteralExpr(terminal, startToken.position);
		}
	}
	
	public Statement parseStatement() throws SyntaxError{
		Token startToken = currentToken;
		if(currentToken.kind == TokenKind.L_CURL){
			StatementList list = new StatementList();
			acceptIt();
			while(currentToken.kind == TokenKind.L_CURL || currentToken.kind == TokenKind.IF || currentToken.kind == TokenKind.WHILE || 
					currentToken.kind == TokenKind.INT || currentToken.kind == TokenKind.BOOLEAN || currentToken.kind == TokenKind.RETURN || 
					currentToken.kind == TokenKind.THIS || currentToken.kind == TokenKind.IDENTIFIER){
				Statement statement = parseStatement();
				list.add(statement);
			}
			accept(TokenKind.R_CURL);
			return new BlockStmt(list, startToken.position);
		}
		else if(currentToken.kind == TokenKind.IF){
			acceptIt();
			accept(TokenKind.L_PAREN);
			Expression expression = parseExpression();
			accept(TokenKind.R_PAREN);
			Statement statement1 = parseStatement();
			if(currentToken.kind == TokenKind.ELSE){
				acceptIt();
				Statement statement2 = parseStatement();
				return new IfStmt(expression, statement1, statement2, startToken.position);
			}
			return new IfStmt(expression, statement1, startToken.position);
		}
		else if(currentToken.kind == TokenKind.WHILE){
			acceptIt();
			accept(TokenKind.L_PAREN);
			Expression expression = parseExpression();
			accept(TokenKind.R_PAREN);
			Statement statement = parseStatement();
			return new WhileStmt(expression, statement, startToken.position);
		}
		else if(currentToken.kind == TokenKind.INT || currentToken.kind == TokenKind.BOOLEAN){
			TypeDenoter type = parseTypeLiteral();
			String varString = currentToken.spelling.toString();
			VarDecl varDec = new VarDecl(type, varString, currentToken.position);
			accept(TokenKind.IDENTIFIER);
			Expression expression = parseEqualsExpression();
			accept(TokenKind.SEMI_COLON);
			return new VarDeclStmt(varDec, expression, startToken.position);
		}
		else if(currentToken.kind == TokenKind.RETURN){
			acceptIt();
			if(currentToken.kind == TokenKind.SEMI_COLON){
				acceptIt();
				return new ReturnStmt(null, startToken.position);
			}
			else{
				Expression expression = parseExpression();
				accept(TokenKind.SEMI_COLON);
				return new ReturnStmt(expression, startToken.position);
			}
		}
		else if(currentToken.kind == TokenKind.THIS){
			Statement statement;
			Reference ref = new ThisRef(startToken.position);
			acceptIt();
			while(currentToken.kind == TokenKind.PERIOD){
				ref = parseReferenceDot(ref);
			}
			if(currentToken.kind == TokenKind.L_PAREN){
				ExprList list = parseCallMethod();
				statement = new CallStmt(ref, list, startToken.position);
			}
			else{
				Expression expression = parseEqualsExpression();
				statement = new AssignStmt(ref, expression, startToken.position);
			}
			accept(TokenKind.SEMI_COLON);
			return statement;
		}
		else{
			Reference ref;
			Statement statement;
			accept(TokenKind.IDENTIFIER);
			if(currentToken.kind == TokenKind.L_BRACK){
				acceptIt();
				if(currentToken.kind == TokenKind.R_BRACK){
					acceptIt();
					ClassType eltType = new ClassType(new Identifier(startToken), startToken.position);
					ArrayType type = new ArrayType(eltType, startToken.position);
					Token varToken = currentToken;
					String varString = currentToken.spelling.toString();
					accept(TokenKind.IDENTIFIER);
					Expression expression = parseEqualsExpression();
					accept(TokenKind.SEMI_COLON);
					VarDecl varDecl = new VarDecl(type, varString, varToken.position);
					return new VarDeclStmt(varDecl, expression, startToken.position);
				}
				else{
					boolean methodCall = false;
					Expression expression1 = parseExpression();
					Expression expression2 = null;
					ExprList list = new ExprList();
					ref = new IxIdRef(new Identifier(startToken), expression1, startToken.position);
					accept(TokenKind.R_BRACK);
					while(currentToken.kind == TokenKind.PERIOD){
						ref = parseReferenceDot(ref);
					}
					if(currentToken.kind == TokenKind.EQUAL){
						expression2 = parseEqualsExpression();
					}
					else{
						list = parseCallMethod();
						methodCall = true;
					}
					accept(TokenKind.SEMI_COLON);
					if(methodCall){
						return new CallStmt(ref, list, startToken.position);
					}
					else{
						return new AssignStmt(ref, expression2, startToken.position);
					}
				}
			}
			else if(currentToken.kind == TokenKind.PERIOD){
				ref = new IdRef(new Identifier(startToken), startToken.position);
				while(currentToken.kind == TokenKind.PERIOD){
					ref = parseReferenceDot(ref);
				}
				if(currentToken.kind == TokenKind.L_PAREN){
					ExprList list = parseCallMethod();
					statement = new CallStmt(ref, list, startToken.position);
				}
				else{
					Expression expression = parseEqualsExpression();
					statement = new AssignStmt(ref, expression, startToken.position);
				}
				accept(TokenKind.SEMI_COLON);
				return statement;
				
			}
			else if (currentToken.kind == TokenKind.IDENTIFIER){
				ClassType classType = new ClassType(new Identifier(startToken), startToken.position);
				Token varToken = currentToken;
				String varString = currentToken.spelling.toString();
				accept(TokenKind.IDENTIFIER);
				Expression expression = parseEqualsExpression();
				accept(TokenKind.SEMI_COLON);
				VarDecl varDecl = new VarDecl(classType, varString, varToken.position);
				return new VarDeclStmt(varDecl, expression, startToken.position);
			}
			else if (currentToken.kind == TokenKind.L_PAREN){
				ref = new IdRef(new Identifier(startToken), startToken.position);
				ExprList list = parseCallMethod();
				accept(TokenKind.SEMI_COLON);
				return new CallStmt(ref, list, startToken.position);
			}
			else{
				ref = new IdRef(new Identifier(startToken), startToken.position);
				Expression expression = parseEqualsExpression();
				accept(TokenKind.SEMI_COLON);
				return new AssignStmt(ref, expression, startToken.position);
			}
		}
	}
}
