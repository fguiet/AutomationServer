package fr.guiet.automationserver.business.service;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import fr.guiet.automationserver.dataaccess.DbManager;

public class WindvaneService implements IMqttable {

	private static Logger _logger = LogManager.getLogger(WindvaneService.class);
	//private SMSGammuService _smsGammuService = null;
	private static String MQTT_TOPIC = "guiet/outside/sensor/21";
	private ArrayList<String> _mqttTopics = new ArrayList<String>();
	private DbManager _dbManager = null;
	
	public WindvaneService() {
		//_smsGammuService = smsGammuService;		
		_mqttTopics.add(MQTT_TOPIC);
		_dbManager = new DbManager();
		
		_logger.info("Starting Windvane service...");
	}
	
	@Override
	public boolean ProcessMqttMessage(String topic, String message) {
		
		boolean messageProcessed = false;
		
		if (topic.equals(MQTT_TOPIC)) {
			
			JSONObject json = new JSONObject(message);
			String battery = json.getString("battery");			
			String firmware = json.getString("firmware");
			
			String winddirection = json.getString("winddirection");
			String degree = json.getString("degree");
									
			try {
				float vcc = Float.parseFloat(battery);
				float degreeFloat = Float.parseFloat(battery);
				
				//Save info to InfluxDb
				_dbManager.SaveWindvaneSensorInfoInfluxDB(vcc, winddirection, degreeFloat);
			}
			catch (NumberFormatException nfe) {
				_logger.error("Could not convert windvane value, battery : " + battery + ", winddirection : " + winddirection + ", vitesse: " + degree, nfe);
			}
			
			//sms.setMessage(mess);
			//_smsGammuService.sendMessage(sms);
			
			_logger.info("Receveid message from windvane. Battery voltage : " + battery+ "v, Firmware : "+ firmware +", winddirection : " + winddirection + ", degree : " + degree);
			
			messageProcessed = true;
		}
		
		return messageProcessed;
		
	}

	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}

	
}