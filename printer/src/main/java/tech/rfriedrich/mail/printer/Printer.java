package tech.rfriedrich.mail.printer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Printer {

	private final static Logger log = LogManager.getLogger(Printer.class);

	private File foxit;

	public Printer(File foxitReaderExecutable) {
		this.foxit = foxitReaderExecutable;
	}

	public void print(File pdfFile) throws IOException, InterruptedException {
		if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
			throw new IllegalArgumentException(
					"The provided file: " + pdfFile.getName() + " was not a pdf and can therefore not be printed.");
		}
		ProcessBuilder pb = new ProcessBuilder("cmd", "/c", foxit.getAbsolutePath(), "/p", "/h", pdfFile.getAbsolutePath());
		String command = pb.command().stream().collect(Collectors.joining(" "));
		pb.redirectErrorStream(true);
		log.debug("Trying to print file: '" + pdfFile.getName() + "'.");
		log.info("Starting process with command: " + command);
		Process process = pb.start();
		BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			log.info("\t" + line);
		}
		process.waitFor();
		in.close();
	}

}
