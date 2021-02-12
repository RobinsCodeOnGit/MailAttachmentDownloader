package tech.rfriedrich.mail.printer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

	private static final Logger log = LogManager.getLogger(Main.class);

	static String host = "imap.web.de";
	static String port = "993";
	static String username = "python39@web.de";
	static String password = "s6ObJzAeAjRpBhmvIPWU";
	static String mailFolderToMove = "Mails Gedruckter Anhänge";
	static String subjectMatch = "Grab";
	static String attachmentSuffix = ".pdf";
	static String foxitReaderExe = "C:\\Program Files (x86)\\Foxit Software\\Foxit Reader\\FoxitReader.exe";
	static File saveDirectory = new File("C:/Attachment");
	static boolean printDownloadedAttachments = false;
	static boolean deleteAttachmentsAfterPrint = false;

	public static void main(String[] args) {
		CliOptions cliOptions = CliOptions.getInstance();
		cliOptions.init();
		CommandLine cmd;
		try {
			cmd = cliOptions.parse(args);

			if (cmd.hasOption("?")) {
				cliOptions.printHelp();
			}
			Properties props = new Properties();
			if (cmd.hasOption("c")) {
				try {
					String pathToConfig = cmd.getOptionValue("c");
					File config = new File(pathToConfig);
					props.load(new InputStreamReader(new FileInputStream(config), Charset.forName("UTF-8")));
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			host = CliOptions.returnValueOfOptionOrDefault(cmd, "imap", props, "host", false, null);
			port = CliOptions.returnValueOfOptionOrDefault(cmd, "p", props, "port", false, "993");
			username = CliOptions.returnValueOfOptionOrDefault(cmd, "u", props, "username", false, null);
			password = CliOptions.returnValueOfOptionOrDefault(cmd, "pw", props, "password", false, null);
			mailFolderToMove = CliOptions.returnValueOfOptionOrDefault(cmd, "mvF", props, "mailFolderToMove", false,
					null);
			saveDirectory = new File(CliOptions.returnValueOfOptionOrDefault(cmd, "dir", props, "saveDirectory", false,
					"C:/Attachments"));
			subjectMatch = CliOptions.returnValueOfOptionOrDefault(cmd, "subject", props, "subjectMatch", true, null);
			attachmentSuffix = CliOptions.returnValueOfOptionOrDefault(cmd, "a", props, "attachmentSuffix", true, null);
			foxitReaderExe = CliOptions.returnValueOfOptionOrDefault(cmd, "fR", props, "foxitReaderExe", true,
					"C:/Program Files (x86)/Foxit Software/Foxit Reader/FoxitReader.exe");
			printDownloadedAttachments = CliOptions.returnBooleanOfOptionOrDefault(cmd, "pA", props,
					"printDownloadedAttachments", false);
			deleteAttachmentsAfterPrint = CliOptions.returnBooleanOfOptionOrDefault(cmd, "dA", props,
					"deleteAttachmentsAfterPrint", false);

		} catch (ParseException e1) {
			log.error("Failed to parse the given arguments.");
			log.error(e1.getMessage(), e1);
			cliOptions.printHelp();
			System.exit(0);
		}

		log.info("Started process.");

		//@formatter:off
		EmailAttachmentReceiver receiver = new EmailAttachmentReceiver();
		receiver.setHost(host)
			.setPort(port)
			.setUsername(username)
			.setPassword(password)
			.setSaveDirectory(saveDirectory.getAbsolutePath())
			.setMailFolderToMoveMailsTo(mailFolderToMove)
			.init();
		//@formatter:on
		try {
			receiver.openSession();
			List<File> downloadedAttachments = receiver.downloadEmailAttachmentsFromINBOX(subjectMatch,
					attachmentSuffix);

			if (printDownloadedAttachments) {
				File foxitReaderExeFile = new File(foxitReaderExe);
				if (!foxitReaderExeFile.exists()) {
					throw new FileNotFoundException("The FoxitReader.exe in the path: "
							+ foxitReaderExeFile.getAbsolutePath() + " was not found.");
				}
				Printer printer = new Printer(foxitReaderExeFile);
				downloadedAttachments.forEach(f -> {
					try {
						printer.print(f);
						if (deleteAttachmentsAfterPrint) {
							f.delete();
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				});
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			receiver.close();
		}
		log.info("Process exits.");
	}

}
