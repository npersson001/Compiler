package miniJava.Errors;


public class NoMainMethodFoundException extends RuntimeException{
	public NoMainMethodFoundException(){
		super("***no main method found");
	}
}