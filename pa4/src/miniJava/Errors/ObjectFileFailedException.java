package miniJava.Errors;


public class ObjectFileFailedException extends RuntimeException{
	public ObjectFileFailedException(){
		super("***object file failed to write");
	}
}
