package fr.guiet.automationserver.business.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.log4j.Logger;

import fr.guiet.automationserver.dto.SMSDto;

/**
 * Helper class to send SMS via Gammu software
 * 
 * @author guiet
 *
 * TODO : faire en sorte que l'on puisse faire une file de SMS dans un thread...
 *
 */
public class SMSGammuService {
	
	private static Logger _logger = Logger.getLogger(SMSGammuService.class);
	private String[] _configRecipientList = null;
	private String _configGammu = null;

	public SMSGammuService() {
		
		InputStream is = null;
        try {
        	
        	String configPath = System.getProperty("automationserver.config.path");
        	is = new FileInputStream(configPath);
        	        	
        	Properties prop = new Properties();            
            prop.load(is);
            
            _configRecipientList = prop.getProperty("sms.recipients").split(",");
            _configGammu = prop.getProperty("gammu.config");
            
            
        } catch (FileNotFoundException e) {
        	_logger.error("Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties", e);
        } catch (IOException e) {
        	_logger.error("Erreur lors de la lecture du fichier de configuration classpath_folder/config/automationserver.properties", e);
        } 		
	}
	
	
	/**
	 *  Send SMS using gammu software
	 *  
	 * @param sms
	 * @param useRecipientInConfig
	 */
	public synchronized void sendMessage(SMSDto sms, boolean useRecipientInConfig) {
	
		if (useRecipientInConfig) {
			for(String recipient : _configRecipientList) {
				sms.setRecipient(recipient);
				SendSMS(sms);
			}
		}
		else {
			//Not useful in my case but...
			SendSMS(sms);
		}
	}
	
	private CommandLine makeCommand(SMSDto sms) {

		//TODO : Add gammu config file
        CommandLine commandLine = new CommandLine("gammu");
        commandLine.addArgument("-c");
        commandLine.addArgument(_configGammu);
        commandLine.addArgument("sendsms") ;
        commandLine.addArgument("TEXT") ;
        commandLine.addArgument(sms.getRecipient()) ;
        commandLine.addArgument("-text") ;
        commandLine.addArgument(sms.getMessage());
        return commandLine;
    }
	
	private synchronized void SendSMS(SMSDto sms) {
		
		MyResultHandler drh = new MyResultHandler(sms);
					
		//String cmd = "gammu -c configfile -TEXT -texts";
		
		CommandLine cmdLine=makeCommand(sms);
		
		//Timeout set to 30s
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
		
		DefaultExecutor de = new DefaultExecutor();
				
		//Define successful exit value
		de.setExitValue(0);
		
		de.setWatchdog(watchdog);
		
		//Start asynchronous thread
		try {
			de.execute(cmdLine, drh);
		} catch (IOException e) {
			_logger.error("Error while sending SMS",e);			
		}
		
	}
	
	private class MyResultHandler extends DefaultExecuteResultHandler {
		
		private SMSDto _sms = null;
		
		public MyResultHandler(SMSDto sms) {
			_sms=sms;
		}
		
	    @Override
	    public void onProcessComplete(final int exitValue) {
	        super.onProcessComplete(exitValue);
	        
	        _logger.info(String.format("SMS sent successfully ! Recipient : %s; Message : %s", _sms.getRecipient(), _sms.getMessage()));
	    }

	    @Override
	    public void onProcessFailed(final ExecuteException e) {
	        super.onProcessFailed(e);
	        
	        String mess = String.format("Error occured while sending SMS ! Recipient : %s; Message : %s", _sms.getRecipient(), _sms.getMessage());
	        _logger.error(mess,e);
	        
	        //Send mail then!!
	        MailService mailService = new MailService();
			mailService.SendMailSSL("AutomationServer - Error sending SMS", mess);
	    }
	}
	
}

