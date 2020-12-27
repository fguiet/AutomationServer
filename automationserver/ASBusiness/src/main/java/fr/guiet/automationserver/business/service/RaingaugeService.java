package fr.guiet.automationserver.business.service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import be.romaincambier.lorawan.FRMPayload;
import be.romaincambier.lorawan.MACPayload;
import be.romaincambier.lorawan.PhyPayload;
import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;

public class RaingaugeService implements Runnable, IMqttable {

	private boolean _isStopped = false; // Service arrete?
	private static Logger _logger = LogManager.getLogger(RaingaugeService.class);
	private Timer _timer = null;
	private DbManager _dbManager = null;
	private SMSGammuService _smsGammuService = null;
	private MqttService _mqttService = null;
	private ArrayList<String> _mqttTopics = new ArrayList<String>();

	// Used to get battery voltage of sensor
	// In OpenHab for instance
	private String _pub_topic = "guiet/outside/sensor/17";

	private final String RAINGAUGE_SENSOR_ID = "17";

	private static String MQTT_TOPIC_LORAWAN = "gateway/dca632fffe365d9c/event/up";

	private Date _lastMessageReceived = new Date();
	
	//private String _lastPayloadReceived = "";
	
	private int _noSensorNewsTimeout = 35; // in minutes

	public RaingaugeService(SMSGammuService smsGammuService, MqttService mqttService) {
		_smsGammuService = smsGammuService;
		_mqttService = mqttService;
		_dbManager = new DbManager();

		// add topics processed by this service
		_mqttTopics.add(MQTT_TOPIC_LORAWAN);
	}

	@Override
	public void run() {

		_logger.info("Starting Raingauge Service...");

		while (!_isStopped) {

			try {

				Thread.sleep(2000);

				Long elapsedTime = DateUtils.minutesBetweenDate(_lastMessageReceived, new Date());

				if (elapsedTime > _noSensorNewsTimeout) {
					String mess = "Aucune nouvelle du pluviomètre au moins " + _noSensorNewsTimeout + " minutes";

					_logger.info(mess);

					SMSDto sms = new SMSDto("1e73328e-12c7-11eb-adc1-0242ac120002");
					sms.setMessage(mess);
					_smsGammuService.sendMessage(sms);

					// Reset
					_lastMessageReceived = new Date();

				}

			} catch (Exception e) {
				_logger.error("Error occured in Raingauge service...", e);

				SMSDto sms = new SMSDto("2f98ac74-12c7-11eb-adc1-0242ac120002");
				sms.setMessage("Error occured in Raingauge service, review error log for more details");
				_smsGammuService.sendMessage(sms);
			}
		}
	}

	// Arret du service LoRaService
	public void StopService() {

		if (_timer != null)
			_timer.cancel();

		_logger.info("Stopping Raingauge service...");

		_isStopped = true;
	}

	@Override
	public boolean ProcessMqttMessage(String topic, String message) {

		boolean messageProcessed = false;

		if (topic.equals(MQTT_TOPIC_LORAWAN)) {

			// _logger.info("LoRaWAN message received : " + message);

			String decryptedPayload = "";

			try {
				JSONObject json = new JSONObject(message);

				String payload = json.getString("phyPayload");
				_logger.info("Payload base64 received : " + payload);
				
				PhyPayload pp;
				// try {
				byte[] decode = Base64.decode(payload);
				// 0x44, 0x4D, 0x47, 0x85, 0xF1, 0xEB, 0x22, 0x4A, 0xE0, 0x25, 0x86, 0xAB, 0x17,
				// 0x56, 0x9C, 0x59
				byte[] nwkSKey = new byte[] { (byte) 0x44, (byte) 0x4D, (byte) 0x47, (byte) 0x85, (byte) 0xF1,
						(byte) 0xEB, (byte) 0x22, (byte) 0x4A, (byte) 0xE0, (byte) 0x25, (byte) 0x86, (byte) 0xAB,
						(byte) 0x17, (byte) 0x56, (byte) 0x9C, (byte) 0x59 };
				// 0x23, 0x71, 0x3C, 0x3C, 0x05, 0x5A, 0x92, 0xD2, 0xBE, 0x23, 0xBE, 0x0E, 0x60,
				// 0x95, 0x80, 0xF1
				byte[] appSKey = new byte[] { (byte) 0x23, (byte) 0x71, (byte) 0x3C, (byte) 0x3C, (byte) 0x05,
						(byte) 0x5A, (byte) 0x92, (byte) 0xD2, (byte) 0xBE, (byte) 0x23, (byte) 0xBE, (byte) 0x0E,
						(byte) 0x60, (byte) 0x95, (byte) 0x80, (byte) 0xF1 };

				pp = PhyPayload.parse(ByteBuffer.wrap(decode));

				MACPayload pl = (MACPayload) pp.getMessage();

				FRMPayload data = pl.getFRMPayload();
				byte[] clearData = data.getClearPayLoad(nwkSKey, appSKey);

				decryptedPayload = new String(clearData);

				_logger.info("Decrypted LoraWAN Message : " + decryptedPayload);

				// Try parse payload

				String[] messageContent = decryptedPayload.split(" ");

				String sensorid = messageContent[0];

				if (sensorid.equals(RAINGAUGE_SENSOR_ID)) {
					
					//2020/12/23 - add this check because something message from gateway is sent two time...
					//In this particular case...message in Application Data, the thing network shows 2 gateways...
					//Don't know why for the moment...may be a problem 
					//with low power mode...
					/*

					{
						  "time": "2020-12-23T17:05:32.079674859Z",
						  "frequency": 868.5,
						  "modulation": "LORA",
						  "data_rate": "SF7BW125",
						  "coding_rate": "4/5",
						  "gateways": [
						    {
						      "gtw_id": "eui-dca632fffe365d9c",
						      "timestamp": 4084734907,
						      "time": "2020-12-23T17:05:31.024132Z",
						      "channel": 2,
						      "rssi": -87,
						      "snr": 6.8,
						      "latitude": 48.09587,
						      "longitude": 1.89417,
						      "altitude": 122
						    }
						  ]
						}
					*/
					/*if (payload.equals(_lastPayloadReceived)) {
						_logger.info("Same message received two times...skipping this one...");
						messageProcessed = true;
						return messageProcessed;
					}*/
					if (DateUtils.secondsBetweenDate(_lastMessageReceived, new Date()).compareTo(new Long(4)) < 0) {
						_logger.info("Less than two second ago I received a message from Raingauge sensor...skipping this one...");
						messageProcessed = true;
						return messageProcessed;
					}
						
					//_lastPayloadReceived = payload;
					_lastMessageReceived = new Date();
															
					
					String firmware = messageContent[1];
					String battery = messageContent[2];
					String flipflop = messageContent[3];

					// TODO : review that!
					JSONObject mess = new JSONObject();
					mess.put("id", RAINGAUGE_SENSOR_ID);
					mess.put("name", "raingauge"); // WM : Water Meter
					mess.put("firmware", firmware);
					mess.put("battery", battery);
					mess.put("flipflop", flipflop);

					_logger.info("JSON Message for OpenHab : " + mess.toString());		
					
					//_lastMessageReceived = new Date();		

					_mqttService.SendMsg(_pub_topic, mess.toString());

					float vcc = 0;
					try {
						vcc = Float.parseFloat(battery);
						_dbManager.SaveRainGaugeInfo(vcc, flipflop);
					} catch (NumberFormatException e) {
						_logger.error("Valeur de la batterie pour le capteur Raingauge incorrecte : " + battery);
					}
				} else {
					throw new Exception("sensorid is " + sensorid + ", mine is " + RAINGAUGE_SENSOR_ID
							+ ", LoRaWAN frame not for me");
				}

				messageProcessed = true;

			} catch (JSONException e) {
				_logger.info("Cannot parse JSON LoRaWAN Message : " + message, e);
			} catch (Exception e) {
				_logger.info("Cannot parse LoRaWAN decrypted message, should not be for me : " + decryptedPayload, e);
			}
		}

		return messageProcessed;
	}

	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}
}
