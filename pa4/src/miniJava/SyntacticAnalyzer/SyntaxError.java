package miniJava.SyntacticAnalyzer;

public class SyntaxError extends Exception{
	SyntaxError() {
	    super();
	};

	SyntaxError (String s) {
	    super(s);
	}
}
