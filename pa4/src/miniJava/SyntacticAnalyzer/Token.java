package miniJava.SyntacticAnalyzer;

public class Token {
	public TokenKind kind;
	public StringBuffer spelling;
	public SourcePosition position;

	public Token(TokenKind kind, StringBuffer spelling, SourcePosition pos) {
		this.kind = kind;
		this.spelling = spelling;
		this.position = pos;
		
		if(kind == TokenKind.IDENTIFIER){
			switch(spelling.toString()){
				case "class":
					this.kind = TokenKind.CLASS;
					break;
				case "void":
					this.kind = TokenKind.VOID;
					break;
				case "public":
					this.kind = TokenKind.PUBLIC;
					break;
				case "private":
					this.kind = TokenKind.PRIVATE;
					break;
				case "static":
					this.kind = TokenKind.STATIC;
					break;
				case "int":
					this.kind = TokenKind.INT;
					break;
				case "boolean":
					this.kind = TokenKind.BOOLEAN;
					break;
				case "this":
					this.kind = TokenKind.THIS;
					break;
				case "if":
					this.kind = TokenKind.IF;
					break;
				case "else":
					this.kind = TokenKind.ELSE;
					break;
				case "while":
					this.kind = TokenKind.WHILE;
					break;
				case "true":
					this.kind = TokenKind.TRUE;
					break;
				case "false":
					this.kind = TokenKind.FALSE;
					break;
				case "new":
					this.kind = TokenKind.NEW;
					break;
				case "return":
					this.kind = TokenKind.RETURN;
					break;
				case "null":
					this.kind = TokenKind.NULL;
					break;
			}
		}
	}
}
