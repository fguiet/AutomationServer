package fr.guiet.automationserver.business;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class MailService {
	
	private static Logger _logger = Logger.getLogger(MailService.class);
	
	public void SendMailSSL(String subject, String body) {		

		InputStream is = null;					
        try {
        	
        	String configPath = System.getProperty("automationserver.config.path");
        	is = new FileInputStream(configPath);
        	        	
        	Properties prop = new Properties();            
            prop.load(is);
            
            String recipients = prop.getProperty("mail.recipients");           
            final String userMail = prop.getProperty("mail.user");
            final String password = prop.getProperty("mail.password");
            String smtpServer = prop.getProperty("mail.smtp");
            
            prop = new Properties();

    		prop.put("mail.smtp.host", smtpServer);
    		prop.put("mail.smtp.socketFactory.port", "465");
    		prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    		prop.put("mail.smtp.auth", "true");
    		prop.put("mail.smtp.port", "465");
            
            Session session = Session.getDefaultInstance(prop, new javax.mail.Authenticator() {
    			protected PasswordAuthentication getPasswordAuthentication() {
    				return new PasswordAuthentication(userMail, password);
    			}
    		});
            
            Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("no-reply@automationserver.fr"));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
			message.setSubject(subject);
			message.setText(body);

			_logger.info("Sending mail to "+recipients+"\n\nBODY : " + body);
			Transport.send(message);
            _logger.info("Message sent");
            
        } catch (FileNotFoundException e) {
        	_logger.error("Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties", e);
        } catch (IOException e) {
        	_logger.error("Erreur lors de la lecture du fichier de configuration classpath_folder/config/automationserver.properties", e);
        } 	
        catch (MessagingException e) {
        	_logger.error("Error while sending email",e);
		}
        catch (Exception e) {
        	_logger.error("Error while sending email",e);
		}
	}

}