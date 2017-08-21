package fr.guiet.automationserver.business;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttClientMgt implements MqttCallback {
	
	private static Logger _logger = Logger.getLogger(MqttClientMgt.class);
	private String _uri = "tcp://%s:%s";
	private MqttClient _client = null;
	private final String CLIENT_ID = "Tomcat Mqtt Client";
	
	public MqttClientMgt() {

		InputStream is = null;
		
		try {

			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);

			String host = prop.getProperty("mqtt.host");
			String port = prop.getProperty("mqtt.port");
			_uri = String.format(_uri, host, port);						

		} catch (FileNotFoundException e) {
			_logger.error("Cannot find configuration file in classpath_folder/config/automationserver.properties", e);
		} catch (IOException e) {
			_logger.error("Error in reading configuration file classpath_folder/config/automationserver.properties", e);
		}

	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public void SendMsg(String topic, String message) {
		
		try {
			_client = new MqttClient(_uri, CLIENT_ID);
			_client.setCallback(this);
			_client.connect();
			
			MqttMessage mqttMessage = new MqttMessage();
			mqttMessage.setPayload(message.getBytes("UTF8"));
			_client.publish(topic, mqttMessage);

			
			_client.disconnect();
		} catch (MqttException me) {
			_logger.error("Error sending message to mqtt broker", me);			
		} catch (UnsupportedEncodingException e) {
			_logger.error("Error when encoding message in UTF8", e);
		} catch (Exception e) {		
			_logger.error("Error occured when sending message to mqtt broker", e);		
		}
	}
}