package tech.rfriedrich.mail.printer;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CliOptions {
	private static final Logger log = LogManager.getLogger(CliOptions.class);
	private static CliOptions instance;

	Options options;

	private CliOptions() {
	}

	public static CliOptions getInstance() {
		if (instance == null) {
			instance = new CliOptions();
		}
		return instance;
	}

	public void init() {
		options = new Options();
		options.addOption("?", "help", false, "print this message");
		options.addOption("c", "config", true,
				"Path to the config.properties file. The parameters in the config will be regarded as default. All parameters overwrite the parameters given by the config file.");
		options.addOption("imap", "imapServer", true, "Specifies the imap server.");
		options.addOption("p", "port", true, "Specifies the port of the imap server.");
		options.addOption("u", "username", true, "username/mailaddress for the mail server.");
		options.addOption("pw", "password", true, "Specifies the password for the specified mail address.");
		options.addOption("mvF", "mailMoveFolder", true,
				"The name of the folder in the mail server the mails will be moved after saving the attachment.");
		options.addOption("dir", "saveDirectory", true,
				"The folder on the computer (local), where the attachments will be saved to.");
		options.addOption("subject", "subjectMatch", true,
				"Only mails of the Inbox with an subject containing this given string will be analyzed for attachments.");
		options.addOption("a", "attachmentSuffix", true,
				"Only Attachements with a filename matching this suffix will be saved to the 'saveDirectory'.");
		options.addOption("fR", "foxitReaderExe", true,
				"Path to the FfoxitReader.exe, used to print the downloaded attachments");
		options.addOption("pA", "printAttachments", false, "Specifies whether to print the downloaded attachments.");
		options.addOption("dA", "deleteAttachmentsAfterPrint", false,
				"Specifies whether attachments are to be deleted after printing.");

		log.debug("CommandLine Options were initialized");
	}

	public CommandLine parse(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		return parser.parse(options, args);
	}

	public void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("saveMailAttachements", options);
	}

	public static String returnValueOfOptionOrDefault(CommandLine cmd, String opt, Properties props, String propertyKey,
			boolean isNullAllowed, String defaultValue) throws ParseException, IllegalArgumentException {
		String property = props.getProperty(propertyKey);
		String returnValue = null;
		if (cmd.hasOption(opt) && cmd.getOptionValue(opt) != null) {
			returnValue = cmd.getOptionValue(opt);
		} else if (property != null) {
			returnValue = property;
		} else if (isNullAllowed) {
			returnValue = null;
		} else {
			if (defaultValue == null) {
				throw new IllegalArgumentException(propertyKey + " cannot be null.");
			}
			returnValue = defaultValue;
		}
		String logValue = returnValue;
		if (opt.equalsIgnoreCase("pw")) {
			logValue = EmailAttachmentReceiver.hidePassword(returnValue);
		}
		log.debug("Option '" + opt + "' was set to " + (returnValue == null ? "null" : "'" + logValue + "'"));
		return returnValue;
	}

	public static boolean returnBooleanOfOptionOrDefault(CommandLine cmd, String opt, Properties props,
			String propertyKey, Boolean defaultValue) throws ParseException {
		String property = props.getProperty(propertyKey);
		if (cmd.hasOption(opt)) {
			log.debug("Option: '" + opt + "' was set to: true");
			return true;
		} else if (property != null) {
			if (property.trim().equalsIgnoreCase("true")) {
				log.debug("Option: '" + opt + "' was set to: true");
				return true;
			} else if (property.trim().equalsIgnoreCase("false")) {
				log.debug("Option: '" + opt + "' was set to: false");
				return false;
			} else {
				throw new ParseException(
						"Couldn't parse the property " + propertyKey + "'s value: '" + property + "'.");
			}
		} else {
			log.debug("Option: '" + opt + "' was set to: " + defaultValue.toString());
			return defaultValue;
		}

	}
}
