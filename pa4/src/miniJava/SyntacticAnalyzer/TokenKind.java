package miniJava.SyntacticAnalyzer;

public enum TokenKind {
	//class declaration
	CLASS,
		
	//visibility
	PUBLIC, PRIVATE,
		
	//access
	STATIC,
		
	//return type
	VOID, INT, BOOLEAN, IDENTIFIER,
		
	//reference type
	THIS,
		
	//statement keywords
	RETURN, IF, ELSE, WHILE,
		
	//expression 
	TRUE, FALSE, NEW, NULL,
		
	//operators
	MULTIPLICATION, DIVISION, ADDITION, SUBTRACTION,
	AND, OR, NOT, LESS, GREATER, GREATER_EQUAL, 
	LESS_EQUAL, DOUBLE_EQUAL, NOT_EQUAL, EQUAL, 
		
	//special chars
	L_PAREN, R_PAREN, L_CURL, R_CURL, L_BRACK,
	R_BRACK, COMMA, SEMI_COLON, PERIOD,
		
	//literal value
	NUM,
	
	//error
	ERROR, UNSUPPORTED,
	
	//EOT
	EOT
}
