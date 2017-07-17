package miniJava.Errors;

public class MultipleMainMethodFoundException extends RuntimeException{
	public MultipleMainMethodFoundException(){
		super("***multiple main methods found");
	}
}
