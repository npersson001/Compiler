package tester;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* Automated regression tester for Checkpoint 1 tests
 * Created by Max Beckman-Harned
 * updated by jfp to accommodate different project organizations
 * Put your tests in "tests/pa1_tests" folder in your Eclipse workspace directory
 */
public class Checkpoint1 {
	
	static ExecutorService threadPool = Executors.newCachedThreadPool();

	public static void main(String[] args) throws IOException, InterruptedException {
		
		// should be project directory for miniJava and tester
		String cwd = System.getProperty("user.dir");  
		
		File testDir = new File(cwd + "/../tests/pa1_tests");
		int failures = 0;
		for (File x : testDir.listFiles()) {
			int returnCode = runTest(x);
			if (x.getName().indexOf("pass") != -1) {
				if (returnCode == 0)
					System.out.println(x.getName() + " passed successfully!");
				else {
					failures++;
					System.err.println(x.getName()
							+ " failed but should have passed!");
				}
			} else {
				if (returnCode == 4)
					System.out.println(x.getName() + " failed successfully!");
				else {
					System.err.println(x.getName() + " did not fail properly!");
					failures++;
				}
			}
		}
		System.out.println(failures + " failures in all.");	
	}
	
	private static int runTest(File x) throws IOException, InterruptedException {
		
		// should be be project directory when source and class files are kept together 
		// should be "bin" subdirectory of project directory when separate src
		// and bin organization is used
		String jcp = System.getProperty("java.class.path");
		
		String testPath = x.getPath();
		ProcessBuilder pb = new ProcessBuilder("java", "miniJava.Compiler", testPath);
		pb.directory(new File(jcp));
		Process p = pb.start();
		threadPool.execute(new ProcessOutputter(p.getInputStream(), false));
		p.waitFor();
		return p.exitValue();
	}
	
	static class ProcessOutputter implements Runnable {
		private Scanner processOutput;
		private boolean output;
		
		public ProcessOutputter(InputStream _processStream, boolean _output) {
			processOutput = new Scanner(_processStream);
			output = _output;
		}
		@Override
		public void run() {
			while(processOutput.hasNextLine()) {
				String line = processOutput.nextLine();
				if (output)
					System.out.println(line);
			}
		}
		
		
	}
}
