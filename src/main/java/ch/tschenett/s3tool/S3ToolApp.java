package ch.tschenett.s3tool;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class S3ToolApp extends Command {
	public static void main(String[] args) {
		try {
			SpringApplication.run(S3ToolApp.class, args).getBean(S3ToolApp.class).execute();
			
			System.exit(0);
		}
		catch (Exception e) {
			syserr("error: " + e.getMessage());
			
			System.exit(1);
		}
	}

	@Value(value="${command}")
	private String command;
	
	@Autowired
	private ApplicationContext ctx;
	
	@Override
	public void execute() throws IOException {
		((Command) ctx.getBean(this.command + COMMAND_BEAN_NAME_SUFFIX)).execute();
	}
}
