package miniJava.SyntacticAnalyzer;

public class Scanner {
	private SourceFile sourceFile;
	private boolean debug;

	private char currentChar;
	private StringBuffer currentSpelling;
	private boolean currentlyScanningToken;

	//helper methods for the scanner class to check type of char
	private boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	private boolean isDigit(char c) {
		return (c >= '0' && c <= '9');
	}
	
	//constructor method
	public Scanner(SourceFile source) {
		sourceFile = source;
	    currentChar = sourceFile.getSource();
	    debug = false;
	}
	
	//method to turn on debugging, not used for pa1
	public void enableDebugging() {
	    debug = true;
	}
	
	//appends the current char to the current token
	private void takeIt() {
	    if (currentlyScanningToken)
	    currentSpelling.append(currentChar);
	    currentChar = sourceFile.getSource();
	}
	
	private void skipIt() {
		currentChar = sourceFile.getSource();
	}
	
	//skips a single char or a comment
	private TokenKind scanSeparator() {
	    switch (currentChar) {
		    case '/':
		    	skipIt();
		    	switch(currentChar){
		    		case '/':
		    			skipIt();
		    			while(currentChar != '\n' && currentChar != SourceFile.eot && currentChar != '\r'){
			    			skipIt();
		    			}
		    			skipIt();
		    			return null;
		    		case '*':
		    			skipIt();
		    			while(currentChar != SourceFile.eot){
			    			if(currentChar == '*'){
			    				while(currentChar == '*'){
			    					skipIt();
			    				}
			    				if(currentChar == '/'){
			    					skipIt();
			    					return null;
			    				}
			    			}
			    			skipIt();
		    			}
		    		default: 
		    			return TokenKind.DIVISION;
		    	}
		    case ' ': 
	    	case '\n': 
	    	case '\r': 
	    	case '\t':
	    		skipIt();
	    		break;
	    }
	    return null;
	}
	
	private TokenKind scanToken(){
		switch(currentChar){
			case 'a':  case 'b':  case 'c':  case 'd':  case 'e':
			case 'f':  case 'g':  case 'h':  case 'i':  case 'j':
			case 'k':  case 'l':  case 'm':  case 'n':  case 'o':
			case 'p':  case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':  case 'y':
			case 'z':
			case 'A':  case 'B':  case 'C':  case 'D':  case 'E':
			case 'F':  case 'G':  case 'H':  case 'I':  case 'J':
			case 'K':  case 'L':  case 'M':  case 'N':  case 'O':
			case 'P':  case 'Q':  case 'R':  case 'S':  case 'T':
			case 'U':  case 'V':  case 'W':  case 'X':  case 'Y':
			case 'Z':
				takeIt();
				while(isLetter(currentChar) || isDigit(currentChar) || currentChar == '_'){
					takeIt();
				}
				return TokenKind.IDENTIFIER;
			case '0':  case '1':  case '2':  case '3':  case '4':
		    case '5':  case '6':  case '7':  case '8':  case '9':
		    	takeIt();
		    	while (isDigit(currentChar))
		        takeIt();
		    	return TokenKind.NUM;
		    case '*':
		    	takeIt();
		    	return TokenKind.MULTIPLICATION;
		    case '/':
		    	takeIt();
		    	return TokenKind.DIVISION;
		    case '+':
		    	takeIt();
		    	return TokenKind.ADDITION;
		    case '-':
		    	takeIt();
		    	if(currentChar == '-'){
		    		takeIt();
		    		return TokenKind.ERROR;
		    	}
		    	return TokenKind.SUBTRACTION;
		    case ';':
		        takeIt();
		        return TokenKind.SEMI_COLON;
		    case ',':
		        takeIt();
		        return TokenKind.COMMA;
		    case '.':
		        takeIt();
		        return TokenKind.PERIOD;
		    case '!':
		        takeIt();
		        if(currentChar == '='){
		        	takeIt();
		        	return TokenKind.NOT_EQUAL;
		        }
		        return TokenKind.NOT;
		    case '=':
		        takeIt();
		        if(currentChar == '='){
		        	takeIt();
		        	return TokenKind.DOUBLE_EQUAL;
		        }
		        return TokenKind.EQUAL;
		    case '<':
		        takeIt();
		        if(currentChar == '='){
		        	takeIt();
		        	return TokenKind.LESS_EQUAL;
		        }
		        return TokenKind.LESS;
		    case '>':
		        takeIt();
		        if(currentChar == '='){
		        	takeIt();
		        	return TokenKind.GREATER_EQUAL;
		        }
		        return TokenKind.GREATER;
		    case '&':
		    	takeIt();
		    	if(currentChar == '&'){
		    		takeIt();
		    		return TokenKind.AND;
		    	}
		    	return TokenKind.ERROR;
		    case '|':
		    	takeIt();
		    	if(currentChar == '|'){
		    		takeIt();
		    		return TokenKind.OR;
		    	}
		    	return TokenKind.ERROR;
		    case '(':
		        takeIt();
		        return TokenKind.L_PAREN;
		    case ')':
		        takeIt();
		        return TokenKind.R_PAREN;
		    case '[':
		        takeIt();
		        return TokenKind.L_BRACK;
		    case ']':
		        takeIt();
		        return TokenKind.R_BRACK;
		    case '{':
		        takeIt();
		        return TokenKind.L_CURL;
		    case '}':
		        takeIt();
		        return TokenKind.R_CURL;
		    case SourceFile.eot:
		    	takeIt();
		    	return TokenKind.EOT;
		    default:
		    	takeIt();
		    	return TokenKind.ERROR;
		}
	}
	
	public Token scan () {
	    Token token;
	    SourcePosition position;
	    TokenKind kind = null;
	    
	    currentSpelling = new StringBuffer("");
	    position = new SourcePosition();

	    currentlyScanningToken = false;
	    position.start = sourceFile.getCurrentLine();
	    while (currentChar == '/'
	           || currentChar == ' '
	           || currentChar == '\n'
	           || currentChar == '\r'
	           || currentChar == '\t'){
	    	kind = scanSeparator();
	    	if(kind != null){
	    		break;
	    	}
	    }
	    
	    currentlyScanningToken = true;
	    if(kind != null){
	    	position.finish = position.start;
	    	currentSpelling.append("/");
	    	token = new Token(kind, currentSpelling, position);
	    }
	    else {
	    	position.start = sourceFile.getCurrentLine();
		    kind = scanToken();
		    position.finish = sourceFile.getCurrentLine();
		    token = new Token(kind, currentSpelling, position);
	    }
	    
	    return token;
	  }
}
