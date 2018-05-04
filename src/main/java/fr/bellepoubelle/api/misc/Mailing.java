package fr.bellepoubelle.api.misc;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.mail.smtp.SMTPAddressFailedException;
import com.sun.mail.smtp.SMTPAddressSucceededException;
import com.sun.mail.smtp.SMTPSendFailedException;

/**
 * Send the messages.
 * 
 * @author Jürgen Hecht
 *
 */
public class Mailing implements Runnable {

	private static Logger logger = LogManager.getLogger(Mailing.class);
	private static PropertyManager config = PropertyManager.getInstance();

	private String recipient;
	private String subject;
	private String content;

	public Mailing(String recipient, String subject, String content) {
		this.recipient = recipient;
		this.subject = subject;
		this.content = content;
	}

	/**
	 * Sends a predefined E-Mail to the specified recipient using the
	 * SMTP-server in the properties file.
	 * 
	 * @param recipient
	 *            The recipient of the sent mail.
	 * @param subject
	 *            The subject of the sent mail.
	 * @param content
	 *            The content of the sent mail.
	 */
	public static void sendMail(String recipient, String subject, String content) {
		Thread mailThread = new Thread(new Mailing(recipient, subject, content));
		mailThread.start();
	}

	@Override
	public void run() {
		logger.debug("Setup Mail Server Properties");
		Properties props = System.getProperties();
		props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.starttls.required", "true");
		props.put("mail.smtp.reportsuccess", "true");
		logger.debug("Mail Server Properties have been setup successfully");
		logger.debug("Get Mail Session");
		Session session = Session.getDefaultInstance(props, null);
		MimeMessage mail = new MimeMessage(session);
		try {
			mail.setFrom(new InternetAddress(config.getProperty("mail.address")));
			mail.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
			mail.setSubject(subject, "UTF-8");
			mail.setContent(content, "text/html; charset=UTF-8");
		} catch (MessagingException e) {
			logger.error(e.getMessage());
		}
		logger.debug("Mail Session has been created successfully");
		logger.debug("Get Session and send mail");
		try {
			Transport transport = session.getTransport("smtp");
			transport.connect(config.getProperty("mail.smtp.host"), config.getProperty("mail.address"),
					config.getProperty("mail.password"));
			transport.sendMessage(mail, mail.getAllRecipients());
			transport.close();

		} catch (MessagingException e) {

			/*
			 * Handle SMTP-specific exceptions.
			 */
			if (e instanceof SendFailedException) {
				MessagingException sfe = (MessagingException) e;
				if (sfe instanceof SMTPSendFailedException) {
					SMTPSendFailedException ssfe = (SMTPSendFailedException) sfe;
					logger.debug("SMTP SEND NOTIFICATION:");
					logger.debug("  Command: " + ssfe.getCommand());
					logger.debug("  RetCode: " + ssfe.getReturnCode());
					logger.debug("  Response: " + ssfe.getMessage());
				}
				Exception ne;
				while ((ne = sfe.getNextException()) != null && ne instanceof MessagingException) {
					sfe = (MessagingException) ne;
					if (sfe instanceof SMTPAddressFailedException) {
						SMTPAddressFailedException ssfe = (SMTPAddressFailedException) sfe;
						logger.error("ADDRESS FAILED:");
						logger.error("  Address: " + ssfe.getAddress());
						logger.error("  Command: " + ssfe.getCommand());
						logger.error("  RetCode: " + ssfe.getReturnCode());
						logger.error("  Response: " + ssfe.getMessage());
					} else if (sfe instanceof SMTPAddressSucceededException) {
						logger.debug("ADDRESS SUCCEEDED:");
						SMTPAddressSucceededException ssfe = (SMTPAddressSucceededException) sfe;
						logger.debug("  Address: " + ssfe.getAddress());
						logger.debug("  Command: " + ssfe.getCommand());
						logger.debug("  RetCode: " + ssfe.getReturnCode());
						logger.debug("  Response: " + ssfe.getMessage());
					}
				}
			} else {
				logger.error(e.getMessage());
			}
		}
	}
}
