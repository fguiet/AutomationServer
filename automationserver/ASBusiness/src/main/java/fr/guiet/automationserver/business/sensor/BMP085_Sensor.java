package fr.guiet.automationserver.business.sensor;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import fr.guiet.automationserver.business.helper.ParseUtils;
import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.dto.SensorDto;

public class BMP085_Sensor extends EnvironmentalSensor {

	private Float _pressure = null;
	private Float _altitude = null;
	
	public static BMP085_Sensor LoadFromDto(SensorDto dto, SMSGammuService gammuService) {

		return new BMP085_Sensor(dto.sensorId, dto.name, dto.mqtt_topic, dto.influxDbMeasurement, gammuService);
	}

	// Constructor
	public BMP085_Sensor(long id, String name, String mqtt_topic, String influxDbMeasurement,
			SMSGammuService smsGammuService) {
		super(id, name, mqtt_topic, influxDbMeasurement, smsGammuService);
	}

	@Override
	public boolean ProcessMqttMessage(String topic, String message) {

		boolean messageProcessed = false;

		// At the moment BMP085 process only one mqtt topic
		if (topic.equals(_mqttTopics.get(0))) {

			try {

				HashMap<String, String> hm = new HashMap<String, String>();
				
				JSONObject json = new JSONObject(message);
				
				String temperature = json.getString("temperature");
				String altitude = json.getString("altitude");
				String pressure = json.getString("pressure");

				hm.put("temperature", temperature);
				hm.put("pressure", pressure);
				hm.put("altitude", altitude);

				// return message process, but do not update sensor value!
				if (!sanityCheck(hm))
					return true;

				_temperature = Float.parseFloat(temperature);
				_pressure = Float.parseFloat(pressure);
				_altitude = Float.parseFloat(altitude);

				// Update last sensor update date
				_lastSensorUpdate = new Date();

				messageProcessed = true;

			} catch (Exception e) {
				_logger.error("Sensor : " + getName() + " (id : " + getId() + ") - Could not process mqtt message : " + message,
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
			mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect temperature : "
					+ temperature;

			_logger.info(mess);

			isOk = false;
		}

		String pressure = values.get("pressure");
		// Check whether it is a float or not
		retVal = ParseUtils.tryFloatParse(pressure);
		if (retVal == null) {
			mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect pressure : "
					+ temperature;

			_logger.info(mess);

			isOk = false;
		}

		String altitude = values.get("altitude");
		// Check whether it is a float or not
		retVal = ParseUtils.tryFloatParse(altitude);
		if (retVal == null) {
			mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect altitude : "
					+ temperature;

			_logger.info(mess);

			isOk = false;
		}

		return isOk;
	}

	@Override
	protected void createSaveToDBTask() {
		
		_logger.info("Creating save to db sensor info task (Sensor : "+ getName() +")");

		TimerTask sensorSavingToDbTask = new TimerTask() {
			@Override
			public void run() {

				try {
					
					if (isOperational()) {
						
						_logger.info("Saving sensor info : " + getName() + " (id : " + getId() + ") to "+getInfluxDbMeasurement()+" InfluxDB measurement");
						
						//No timeout detected and correct values sets to sensor in here...
						_dbManager.saveSensorInfoInfluxDB2(getInfluxDbMeasurement(), _temperature, _pressure, _altitude);
						
					}
				} catch (Exception e) {
					_logger.error("Error occured in sensorSavingToDbTask", e);
				}
			}
		};

		_saveToDbTaskTimer = new Timer(true);
		_saveToDbTaskTimer.schedule(sensorSavingToDbTask, 5000, 60000);

		_logger.info("Save to db sensor info task has been created.");
		
	}
}