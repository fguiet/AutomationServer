package fr.guiet.automationserver.business.service;

import java.util.ArrayList;
//import java.util.Date;

import org.apache.log4j.Logger;
import com.pi4j.io.gpio.RaspiPin;

import fr.guiet.automationserver.business.helper.GpioHelper;
import fr.guiet.automationserver.dto.SMSDto;

public class AlarmService implements IMqttable {

	private static Logger _logger = Logger.getLogger(AlarmService.class);
	private RollerShutterService _rollerShutterService = null;
	private SMSGammuService _smsGammuService = null;
	private boolean _automaticModeStatus = true; // activated by default
	private ArrayList<String> _mqttTopics = new ArrayList<String>();
	private static String MQTT_TOPIC = "guiet/openhab/alarm";

	public AlarmService(RollerShutterService rollerShutterService, SMSGammuService smsGammuService) {

		_rollerShutterService = rollerShutterService;
		_smsGammuService = smsGammuService;

		_mqttTopics.add(MQTT_TOPIC);

		_logger.info("Starting Alarm service...");

		GpioHelper.provisionGpioPin(RaspiPin.GPIO_06,
				"Provisioning Pin for AlarmService, Pin address : " + RaspiPin.GPIO_06.getAddress(),
				com.pi4j.io.gpio.PinState.LOW, _smsGammuService);

		GpioHelper.provisionGpioPin(RaspiPin.GPIO_25,
				"Provisioning Pin for AlarmService, Pin address : " + RaspiPin.GPIO_25.getAddress(),
				com.pi4j.io.gpio.PinState.LOW, _smsGammuService);

	}

	public String GetAutomaticModeStatus() {
		if (_automaticModeStatus) {
			return "ON";
		} else {
			return "OFF";
		}
	}

	public void SetAutomaticModeOn() {
		_automaticModeStatus = true;
		_logger.info("Setting automatic alarm mode ON");
	}

	public void StopService() {
		// _alarmScheduler.stop();

		_logger.info("Stopping AlarmService service...");

		// _isStopped = true;
	}

	public void SetAutomaticModeOff() {
		_automaticModeStatus = false;
		_logger.info("Setting automatic alarm mode OFF");
	}

	public void SetAutomaticOn() {
		if (!_automaticModeStatus) {
			_logger.info("Alarm was asked to be turned on...but automatic mode is OFF");
			return;
		}

		SetOn();

		SMSDto sms = new SMSDto("f26a0589-402a-4326-8e7a-5a5387068b39");
		sms.setMessage("L'alarme vient d'être automatiquement activée. Bonne et douce journée Maître");
		_smsGammuService.sendMessage(sms);
	}

	public void SetOn() {

		// GPIO_06 (notation Pi4J) = GPIO_25 (notation raspberry) = PIN22
		try {
			String logMessage = "Turning house alarm ON";

			// String pinName, PinState state, String logMessage, SMSGammuService smsService
			GpioHelper.changeGpioPinState(RaspiPin.GPIO_06, fr.guiet.automationserver.business.helper.PinState.HIGH,
					logMessage, _smsGammuService);

			Thread.sleep(1000);

			GpioHelper.changeGpioPinState(RaspiPin.GPIO_06, fr.guiet.automationserver.business.helper.PinState.LOW,
					logMessage, _smsGammuService);

			// Close all rollers shutters if not allready closed
			_rollerShutterService.CloseAllRollerShutters();
		} catch (Exception e) {
			_logger.error("Error occured setting alarm on", e);
		}
	}

	public void SetAutomaticOff() {
		if (!_automaticModeStatus) {
			_logger.info("Alarm was asked to be turned off...but automatic mode is OFF");
			return;
		}

		SetOff();

		SMSDto sms = new SMSDto("7610f1fb-c1f0-40da-b8ee-70b514453ef7");
		sms.setMessage("L'alarme vient d'être automatiquement désactivée.");
		_smsGammuService.sendMessage(sms);
	}

	public void SetOff() {

		// GPIO_25 (notation Pi4J) = GPIO_26 (notation raspberry) = PIN37
		try {
			String logMessage = "Turning house alarm OFF";
			GpioHelper.changeGpioPinState(RaspiPin.GPIO_25, fr.guiet.automationserver.business.helper.PinState.HIGH,
					logMessage, _smsGammuService);

			Thread.sleep(1000);

			GpioHelper.changeGpioPinState(RaspiPin.GPIO_25, fr.guiet.automationserver.business.helper.PinState.LOW,
					logMessage, _smsGammuService);
		} catch (Exception e) {
			_logger.error("Error occured setting alarm off", e);
		}

	}

	@Override
	public boolean ProcessMqttMessage(String topic, String message) {

		boolean messageProcessed = false;

		if (topic.equals(MQTT_TOPIC)) {
			
			//TOODO : Remove this (change to Json)
			String[] messageContent = message.split(";");

			messageProcessed = true;
			
			String mode = messageContent[1];
			if (mode.equals("ON")) {
				SetOn();
				//_alarmService.SetOn();
			} else {
				SetOff();
				//_alarmService.SetOff();
			}
		}

		return messageProcessed;

	}

	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}

}
