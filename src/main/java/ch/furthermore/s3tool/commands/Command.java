package ch.furthermore.s3tool.commands;

import java.io.IOException;

public abstract class Command {
	public static final String COMMAND_BEAN_NAME_SUFFIX = "Command";
	
	public static synchronized void sysout(String s) {
		System.out.println(s);
	}
	
	public static synchronized void syserr(String s) {
		System.err.println(s);
	}
	
	public abstract void execute() throws IOException;
}
