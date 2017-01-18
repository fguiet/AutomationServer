package fr.guiet.automationserver.business;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import fr.guiet.automationserver.dto.SMSDto;

public class MqttHelper {
	
	private static Logger _logger = Logger.getLogger(MqttClient.class);
	private String _uri = "tcp://%s:%s";
	private final String CLIENT_ID = "Java Automation Server";
	private SMSGammuService _smsGammuService = null;
		
	public MqttHelper(SMSGammuService gammuService) {
		
		InputStream is = null;
        try {
        	
        	String configPath = System.getProperty("automationserver.config.path");
        	is = new FileInputStream(configPath);
        	
        	Properties prop = new Properties();        	
            prop.load(is);
            
            String host = prop.getProperty("mqtt.host");    
            String port = prop.getProperty("mqtt.port");
            _uri = String.format(_uri, host, port);
            
            _smsGammuService = gammuService;
            
        } catch (FileNotFoundException e) {
        	_logger.error("Cannot find configuration file in classpath_folder/config/automationserver.properties", e);
        } catch (IOException e) {
        	_logger.error("Error in reading configuration file classpath_folder/config/automationserver.properties", e);
        } 	
		
	}
	
	/**
	 * Publishes message to Mqtt broker
	 * 
	 * @param topic
	 * @param message
	 */
	public void Publish(String topic, String message) {
		
		try {
			MqttClient client = new MqttClient(_uri, CLIENT_ID);				
		    client.connect();
		    MqttMessage mqttMessage = new MqttMessage();
		    mqttMessage.setPayload(message.getBytes());
		    client.publish(topic, mqttMessage);
		    client.disconnect();
		}
		catch (MqttException me) {
			_logger.error("Error sending sensor value to mqtt broker", me);
			
			SMSDto sms = new SMSDto();
			sms.setMessage("Error occured in mqtt helper, review error log for more details");
			_smsGammuService.sendMessage(sms, true);
		}		
	}
	
}
