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
		if ("genKey".equals(command)) {
			ctx.getBean(GenKeyCommand.class).execute();
		}
		else if ("encode".equals(command)) {
			ctx.getBean(EncodeCommand.class).execute();
		}
		else if ("decode".equals(command)) {
			ctx.getBean(DecodeCommand.class).execute();
		}
		else if ("putObject".equals(command)) {
			ctx.getBean(PutObjectCommand.class).execute();
		}
		else if ("getObject".equals(command)) {
			ctx.getBean(GetObjectCommand.class).execute();
		}
		else if ("download".equals(command)) {
			ctx.getBean(DownloadCommand.class).execute();
		}
		else if ("upSync".equals(command)) {
			ctx.getBean(UpSyncCommand.class).execute();
		}
		else if ("downSync".equals(command)) {
			ctx.getBean(DownSyncCommand.class).execute();
		}
	 	else throw new RuntimeException("unknown command: '"  + command + "'");
	}
}
