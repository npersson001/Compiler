package miniJava.SyntacticAnalyzer;

import java.io.InputStream;

public class SourceFile {
	static final char eol = '\n';
	static final char eot = '\u0000';

	java.io.InputStream source;
	int currentLine;

	public SourceFile(InputStream stream) {
		source = stream;
		currentLine = 1;
	}

	char getSource() {
	    try {
	    	int c = source.read();

	    	if (c == -1) {
	        c = eot;
	    	} else if (c == eol) {
	    		currentLine++;
	    }
	      return (char) c;
	    }
	    catch (java.io.IOException s) {
	    	return eot;
	    }
	}

	int getCurrentLine() {
	    return currentLine;
	}
}
