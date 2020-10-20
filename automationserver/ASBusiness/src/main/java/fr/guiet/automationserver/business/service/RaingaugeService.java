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
	
	//Used to get battery voltage of sensor
	private String _pub_topic = "/guiet/automationserver/raingauge";
	
	private final String RAINGAUGE_SENSOR_ID = "17";
	
	private static String MQTT_TOPIC_LORAWAN="gateway/dca632fffe365d9c/event/up";
	
	private Date _lastMessageReceived = new Date();	
	
	private int _noSensorNewsTimeout = 35; //in minutes
	
	public RaingaugeService(SMSGammuService smsGammuService, MqttService mqttService) {
		_smsGammuService = smsGammuService;
		_mqttService = mqttService;
		_dbManager = new DbManager();	
		
		//add topics processed by this service
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
					String mess = "Aucune nouvelle du pluviomètre au moins " + _noSensorNewsTimeout +  " minutes";

					_logger.info(mess);

					SMSDto sms = new SMSDto("1e73328e-12c7-11eb-adc1-0242ac120002");
					sms.setMessage(mess);
					_smsGammuService.sendMessage(sms);
					
					//Reset
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
			
			//_logger.info("LoRaWAN message received : " + message);
			
			String decryptedPayload = "";
			
			try {
				JSONObject json = new JSONObject(message);
				
				String payload = json.getString("phyPayload");
				
				_logger.info("Payload base64 received : " + payload);
				
							    
				PhyPayload pp;
				//try {
				byte[] decode = Base64.decode(payload);
				byte[] nwkSKey = new byte[] { (byte)0xA2, (byte)0xE9, (byte)0xB3, 0x2D, (byte)0xE6, (byte)0xCE, (byte)0xA0, 0x73, 0x70, (byte)0xA5, 0x21, (byte)0xAB, (byte)0xCF, 0x51, (byte)0x88, 0x2A };
			    byte[] appSKey = new byte[] { (byte)0xC3, 0x0B, (byte)0xEA, (byte)0xEE, (byte)0xE8, (byte)0xBA, 0x50, (byte)0xA8, 0x31, 0x5B, 0x1E, 0x05, (byte)0x9B, (byte)0xAA, (byte)0xE0, (byte)0xB8 };
			    				    
				pp = PhyPayload.parse(ByteBuffer.wrap(decode));
				
				MACPayload pl = (MACPayload) pp.getMessage();
				
				FRMPayload data = pl.getFRMPayload();
				byte[] clearData = data.getClearPayLoad(nwkSKey, appSKey);
				
				decryptedPayload = new String(clearData);
				
				_logger.info("Decrypted LoraWAN Message : " + decryptedPayload);
				
				//Try parse payload
				
				String[] messageContent = decryptedPayload.split(" ");
				
				String sensorid = messageContent[0];
				
				if (sensorid.equals(RAINGAUGE_SENSOR_ID)) {
					
					String firmware  = messageContent[1];	
					String battery  = messageContent[2];
					String flipflop  = messageContent[3];
					
										
					//TODO : review that!
					JSONObject mess = new JSONObject();
					mess.put("id", RAINGAUGE_SENSOR_ID);
					mess.put("name", "raingauge"); //WM : Water Meter
					mess.put("firmware", firmware);
					mess.put("battery", battery);
					mess.put("flipflop", flipflop);					
					
					_logger.info("JSON Message for OpenHab : " + mess.toString());
					
					_lastMessageReceived = new Date();
					
					_mqttService.SendMsg(_pub_topic, mess.toString());

					float vcc = 0;
					try {
						vcc = Float.parseFloat(battery);
						_dbManager.SaveRainGaugeInfo(vcc, flipflop);
					}
					catch(NumberFormatException e) {
						_logger.error("Valeur de la batterie pour le capteur Raingauge incorrecte : " + battery);
					}
				}
				else {
					throw new Exception("sensorid is " + sensorid + ", mine is " + RAINGAUGE_SENSOR_ID + ", LoRaWAN frame not for me");
				}
									
				messageProcessed = true;
				
			}
			catch(JSONException e) {			
				_logger.info("Cannot parse JSON LoRaWAN Message : " + message, e);				
			}
			catch(Exception e) {
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
