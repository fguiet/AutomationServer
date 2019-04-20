package fr.guiet.automationserver.business.sensor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;

/**
 * @author guiet
 *
 */
public abstract class Sensor {

	// Logger
	protected static Logger _logger = LogManager.getLogger(Sensor.class);

	// Sensor Id
	private long _id;

	// Mqtt topic or topics served by this sensor
	protected ArrayList<String> _mqttTopics = new ArrayList<String>();

	private Timer _timer = null;
	
	public Timer _saveToDbTaskTimer = null;
	
	// Sensor Name
	private String _name;

	// Last sensor update
	protected Date _lastSensorUpdate = null;

	protected SMSGammuService _smsGammuService = null;
	
	private String _influxDbMeasurement;

	private boolean _alertSent5 = false;
	private boolean _alertSent10 = false;
	private boolean _alertSentMore = false;
	// sensor update timout (true when starting...)
	private boolean _sensorUpdateTimeout = true;

	protected DbManager _dbManager = null;

	public Sensor(long id, String name, String mqttTopic, String influxDbMeasurement, SMSGammuService smsGammuService) {
		_id = id;
		_name = name;
		_mqttTopics.add(mqttTopic);
		_smsGammuService = smsGammuService;
		_influxDbMeasurement = influxDbMeasurement;

		createTimeoutCheckTask();
		
		_dbManager = new DbManager();
		
		createSaveToDBTask();
	}

	public long getId() {
		return _id;
	}
	
	public String getName() {
		return _name;
	}
	
	public String getInfluxDbMeasurement() {
		return _influxDbMeasurement;
	}
	
	public SMSGammuService getSmsGammuService() {
		return _smsGammuService;
	}
	
	public boolean isOperational() {
		return !_sensorUpdateTimeout;
	}
	
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}

	public String getLastSensorUpdate() {

		if (_lastSensorUpdate != null) {
			return DateUtils.getDateToString(_lastSensorUpdate);
		} else {
			return "NA";
		}
	}

	//Stop sensor properly
	public void stop() {
		_timer.cancel();
		_timer = null;
	}

	protected abstract void createSaveToDBTask();
	
	protected abstract boolean sanityCheck(HashMap<String, String> values);
	
	private void createTimeoutCheckTask() {

		_logger.info("Creating timeout check task for sensor with id : " + _id + " and name : " + _name);

		TimerTask timeoutCheckTask = new TimerTask() {
			@Override
			public void run() {

				try {
					// Start of sensor...no info received yet...let say it's a timeout
					if (_lastSensorUpdate == null) {
						_sensorUpdateTimeout = true;
						return;
					}

					Date currentDate = new Date();

					long diff = currentDate.getTime() - _lastSensorUpdate.getTime();
					long diffMinutes = diff / (60 * 1000);

					if (diffMinutes > 5 && diffMinutes < 10) {

						if (!_alertSent5) {

							SMSDto sms = new SMSDto("14b869ef-6233-4830-996f-fe7d136fbcb4-" + _id);
							String message = String.format(
									"No updates received from sensor : %s (id : %s) within least 5 minutes", _name,
									_id);
							sms.setMessage(message);
							_smsGammuService.sendMessage(sms);

							_alertSent5 = true;
						}

						_sensorUpdateTimeout = true;
						return;
					}

					if (diffMinutes >= 10 && diffMinutes < 20) {

						if (!_alertSent10) {

							SMSDto sms = new SMSDto("8aaf28ea-fff8-4d3f-843f-1e4288b1ceb7-" + _id);
							String message = String.format(
									"No updates received from sensor : %s (id : %s) within at least 10 minutes", _name,
									_id);
							sms.setMessage(message);
							_smsGammuService.sendMessage(sms);

							_alertSent10 = true;
						}

						_sensorUpdateTimeout = true;
						return;
					}

					if (diffMinutes >= 20) {

						if (!_alertSentMore) {

							SMSDto sms = new SMSDto("bf3b78e3-1d6d-4435-a750-d400f04ab05b- " + _id);
							String message = String.format(
									"No updates received from sensor : %s (id : %s) within least 20 minutes", _name,
									_id);
							sms.setMessage(message);
							_smsGammuService.sendMessage(sms);

							_alertSentMore = true;
						}

						_sensorUpdateTimeout = true;
						return;
					}

					// No more timeout here, reset values
					_alertSent5 = false;
					_alertSent10 = false;
					_alertSentMore = false;
					_sensorUpdateTimeout = false;
				} catch (Exception e) {
					_logger.error("Error occured in sensor timeout check task", e);
				}
			}
		};

		_timer = new Timer(true);

		// Check every minutes...
		_timer.schedule(timeoutCheckTask, 5000, 60000);

		_logger.info("Timeout check task for sensor with id : " + _id + " and name : " + _name + " created.");
	}
}