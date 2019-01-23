package fr.guiet.automationserver.business.sensor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import fr.guiet.automationserver.business.helper.ParseUtils;
import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.dto.SensorDto;

public class DS18B20_Sensor extends EnvironmentalSensor {

	// Constructor
	public DS18B20_Sensor(long id, String name, String mqtt_topic, String influxDbMeasurement,
			SMSGammuService smsGammuService) {
		super(id, name, mqtt_topic, influxDbMeasurement, smsGammuService);
	}
	
	public static DS18B20_Sensor LoadFromDto(SensorDto dto, SMSGammuService gammuService) {

		return new DS18B20_Sensor(dto.sensorId, dto.name, dto.mqtt_topic, dto.influxDbMeasurement, gammuService);
	}

	@Override
	public boolean ProcessMqttMessage(String topic, String message) {

		boolean messageProcessed = false;

		// TODO : Change to JSON please!
		String[] messageContent = message.split(";");

		// At the moment DS18B20 process only one mqtt topic
		if (topic.equals(_mqttTopics.get(0))) {

			try {

				HashMap<String, String> hm = new HashMap<String, String>();

				hm.put("temperature", messageContent[4]);
			
				// return message process, but do not update sensor value!
				if (!sanityCheck(hm))
					return true;

				_temperature = Float.parseFloat(messageContent[4]);

				// Update last sensor update date
				_lastSensorUpdate = new Date();

				messageProcessed = true;

			} catch (Exception e) {
				_logger.error("Sensor : " + _name + " (id : " + _id + ") - Could not process mqtt message : " + message,
						e);
			}
		}

		return messageProcessed;
	}

	@Override
	public boolean sanityCheck(HashMap<String, String> values) {

		boolean isOk = true;
		String mess;

		String temperature = values.get("temperature");
		// Check whether it is a float or not
		Float retVal = ParseUtils.tryFloatParse(temperature);
		if (retVal == null) {
			mess = "Sanity check failed for sensor : " + _name + " (id : " + _id + "), incorrect temperature : "
					+ temperature;

			_logger.info(mess);

			isOk = false;
		}

		return isOk;
	}

	@Override
	protected void createSaveToDBTask() {
		
		_logger.info("Creating save to db sensor info task");

		TimerTask sensorSavingToDbTask = new TimerTask() {
			@Override
			public void run() {

				try {
					
					if (isOperational()) {
						
						_logger.info("Saving sensor info : " + _name + " (id : " + _id + ") to InfluxDB"); 
						
						//No timeout detected and correct values sets to sensor in here...
						_dbManager.saveSensorInfoInfluxDB(_influxDbMeasurement, _temperature);
						
					}
				} catch (Exception e) {
					_logger.error("Error occured in sensorSavingToDbTask", e);
				}
			}
		};

		_saveToDbTaskTimer = new Timer(true);
		_saveToDbTaskTimer.schedule(sensorSavingToDbTask, 5000, 60000);

		_logger.info("Save to db room info task has been created.");
		
	}
}