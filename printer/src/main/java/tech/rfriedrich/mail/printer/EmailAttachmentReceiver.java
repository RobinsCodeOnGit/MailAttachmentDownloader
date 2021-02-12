package tech.rfriedrich.mail.printer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeUtility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EmailAttachmentReceiver {
	private final static Logger log = LogManager.getLogger(EmailAttachmentReceiver.class);

	private String host, port, username, password, mailFolderToMove;
	private File saveDirectory;
	private Properties properties;
	private Session session;
	private Store store;

	public EmailAttachmentReceiver setHost(String host) {
		this.host = host;
		log.debug("Set host to: " + host);
		if (host == null) {
			throw new IllegalArgumentException("Host cannot be null.");
		}
		return this;
	}

	public EmailAttachmentReceiver setPort(String port) {
		this.port = port;
		log.debug("Set port to: " + port);
		if (port == null) {
			throw new IllegalArgumentException("Port cannot be null.");
		}
		return this;
	}

	public EmailAttachmentReceiver setUsername(String username) {
		this.username = username;
		log.debug("Set username to: " + username);
		if (username == null) {
			throw new IllegalArgumentException("username cannot be null.");
		}
		return this;
	}

	public EmailAttachmentReceiver setPassword(String password) {
		this.password = password;
		log.debug("Set password to: " + hidePassword(password));
		if (password == null) {
			throw new IllegalArgumentException("password cannot be null.");
		}
		return this;
	}
	
	public static String hidePassword(String password) {
		if (password.length() > 3) {
			return multiplyChar('*', (password.length() - 3)) + password.substring(password.length() - 3);
		} else {
			return "***";
		}
	}

	private static String multiplyChar(char c, int multiplier) {
		String s = "";
		for (int i = 0; i < multiplier; i++) {
			s += c;
		}
		return s;
	}

	public EmailAttachmentReceiver setMailFolderToMoveMailsTo(String mailFolderToMove) {
		this.mailFolderToMove = mailFolderToMove;
		log.debug("Set mailFolderToMove to: " + mailFolderToMove);
		return this;
	}

	/**
	 * Sets the directory where attached files will be stored.
	 * 
	 * @param dir absolute path of the directory
	 */
	public EmailAttachmentReceiver setSaveDirectory(String dir) {
		try {
			File directory = new File(dir);
			if (directory.exists() && directory.isDirectory()) {
				this.saveDirectory = directory;
			}
			if (!directory.exists() && !directory.isFile()) {
				directory.mkdirs();
				this.saveDirectory = directory;
			}
			log.debug("Set saveDirectory to: " + dir);
		} catch (Exception e) {
			log.error("Failed to set saveDirectory to: " + dir);
			log.error(e.getMessage(), e);
		}
		return this;
	}

	public EmailAttachmentReceiver init() {
		if (host == null || port == null || username == null || password == null || mailFolderToMove == null) {
			throw new IllegalStateException("There are not initialized members.");
		}
		Properties properties = new Properties();

		// server setting
		properties.put("mail.imap.host", host);
		properties.put("mail.imap.port", port);

		// SSL setting
		properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		properties.setProperty("mail.imap.socketFactory.fallback", "false");
		properties.setProperty("mail.imap.socketFactory.port", String.valueOf(port));

		this.properties = properties;
		log.debug("properties were initialized");
		return this;
	}

	/**
	 * Opens the Session with the Mail Server and sets the Store with IMAP protocol.
	 * 
	 * @return
	 * @throws MessagingException
	 */
	public EmailAttachmentReceiver openSession() throws MessagingException {
		session = Session.getDefaultInstance(properties);
		try {
			// connects to the message store
			store = session.getStore("imap");
			store.connect(username, password);
		} catch (MessagingException e) {
			log.error(e.getMessage(), e);
			throw e;
		}
		return this;
	}

	/**
	 * Downloads new messages and saves attachments to disk for mails which subject
	 * contains the @param subjectMatch if any. And only if the attachment filename
	 * has the given suffix.
	 * 
	 * @return Returns a list of the saved files.s
	 * 
	 * @param subjectMatch
	 * @param attachmentFileNameSuffix
	 * @throws MessagingException, IOException
	 */
	public List<File> downloadEmailAttachmentsFromINBOX(String subjectMatch, String attachmentFileNameSuffix)
			throws MessagingException, IOException {

		List<File> savedAttachments = new ArrayList<File>();
		try {
			// opens the inbox folder
			Folder folderInbox = store.getFolder("INBOX");

			folderInbox.open(Folder.READ_WRITE);

			// fetches new messages from server
			Message[] arrayMessages = folderInbox.getMessages();

			for (int i = 0; i < arrayMessages.length; i++) {
				Message message = arrayMessages[i];
				Address[] fromAddress = message.getFrom();
				String from = fromAddress[0].toString();
				String subject = message.getSubject();
				String sentDate = message.getSentDate().toString();

				String contentType = message.getContentType();
				log.trace("Read Mail:");
				log.trace("\tfrom: " + from);
				log.trace("\tsubject: " + subject);
				log.trace("\tsentDate: " + sentDate);
				log.trace("\tcontentType: " + contentType);

				if (doesSubjectMatch(message, subjectMatch)) {
					log.info("Subject of Message: " + message.getSubject() + " contains '" + subjectMatch + "'");
					if (contentType.contains("multipart")) {
						// content may contain attachments
						Multipart multiPart = (Multipart) message.getContent();
						int numberOfParts = multiPart.getCount();
						for (int partCount = 0; partCount < numberOfParts; partCount++) {
							MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
							// If part is attachment:
							if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
									|| "inline".equalsIgnoreCase(part.getDisposition())) {
								log.info("Mail '" + message.getSubject() + "' has attachements.");
								// this part is attachment
								String fileName = MimeUtility.decodeText(part.getFileName());
								if (doesFilenameSuffixMatch(fileName, attachmentFileNameSuffix)) {
									log.debug("Attachement: " + fileName + " ends with '" + attachmentFileNameSuffix + "'");
									File attachment = new File(this.saveDirectory + File.separator + fileName);
									part.saveFile(attachment);
									log.info("Saved File: " + this.saveDirectory + File.separator + fileName);
									moveMessageToFolder(message, folderInbox, store.getFolder(mailFolderToMove));
									savedAttachments.add(attachment);
								}
							}
						}
					}
				}
			}
			folderInbox.expunge();
			folderInbox.close(false);
		} catch (MessagingException | IOException e) {
			throw e;
		}
		return savedAttachments;
	}

	private boolean doesFilenameSuffixMatch(String fileName, String attachmentFileNameSuffix) {
		if (attachmentFileNameSuffix == null) {
			return true;
		}
		return fileName.toLowerCase().endsWith(attachmentFileNameSuffix);
	}

	private boolean doesSubjectMatch(Message message, String subjectMatch) throws MessagingException {
		if (subjectMatch == null) {
			return true;
		} else {
			return message.getSubject().toLowerCase().contains(subjectMatch.toLowerCase());
		}

	}

	private void moveMessageToFolder(Message message, Folder source, Folder target) {
		Message[] messageArray = new Message[1];
		messageArray[0] = message;

		try {
			log.info("Copying Mail: '" + message.getSubject() + "' to Folder: " + this.mailFolderToMove);
			message.setFlag(Flag.SEEN, false);
			source.copyMessages(messageArray, target);
			log.info("Marking original mail in Inbox: '" + message.getSubject() + "' to be deleted.");
			message.setFlag(Flag.DELETED, true);
		} catch (MessagingException e) {
			log.error("Failed to Copy message to Folder: " + target.getName());
			log.error(e.getMessage(), e);
		}
	}

	public void close() {
		if (store.isConnected()) {
			try {
				log.info("Closing the message store, mails wich are flagged to be deleted, will now be deleted.");
				store.close();
			} catch (MessagingException e) {
				log.error(e.getMessage(), e);
			}
		}
		log.info("Store was closed. You're now disconnected to the server.");
	}
}
