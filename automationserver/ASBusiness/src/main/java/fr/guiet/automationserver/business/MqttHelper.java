package fr.guiet.automationserver.business;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;

public class MqttHelper implements MqttCallback {

	private static Logger _logger = Logger.getLogger(MqttClient.class);
	private String _uri = "tcp://%s:%s";
	private final String CLIENT_ID = "Java Automation Server";
	private SMSGammuService _smsGammuService = null;
	private MqttClient _client = null;
	private String[] _topics = null;
	private RoomService _roomService = null;
	private TeleInfoService _teleInfoService = null;
	private WaterHeater _waterHeaterService = null;
	private AlarmService _alarmService = null;
	private RollerShutterService _rollerShutterService = null;
	private DbManager _dbManager = null;
	private Date _lastGotMailMessage = null;
	private final String HOME_INFO_MQTT_TOPIC = "/guiet/home/info";
	private Date _lastBasementMessage = new Date();
	private Date _lastComputeBillCost = null;
	private String _electricityBill = "NA";
	
	public Date GetLastBasementMessage() {
		return _lastBasementMessage;
	}	

	public MqttHelper(SMSGammuService gammuService, RoomService roomService, 
			TeleInfoService teleInfoService, WaterHeater waterHeaterService, AlarmService alarmService,
			RollerShutterService rollerShutterService) {

		InputStream is = null;
		try {

			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);

			String host = prop.getProperty("mqtt.host");
			String port = prop.getProperty("mqtt.port");
			_uri = String.format(_uri, host, port);
			_topics = prop.getProperty("mqtt.topics").split(";");

			_smsGammuService = gammuService;
			_roomService = roomService;
			_teleInfoService = teleInfoService;
			_waterHeaterService = waterHeaterService;
			_alarmService = alarmService;
			_rollerShutterService = rollerShutterService;
			_dbManager = new DbManager();

		} catch (FileNotFoundException e) {
			_logger.error("Cannot find configuration file in classpath_folder/config/automationserver.properties", e);
		} catch (IOException e) {
			_logger.error("Error in reading configuration file classpath_folder/config/automationserver.properties", e);
		}

	}

	public void connectAndSubscribe() {
		try {
			_client = new MqttClient(_uri, CLIENT_ID);
			_client.setCallback(this);
			_client.connect();
			int subQoS = 0;

			for (String topic : _topics) {
				if (!topic.equals("")) {
					_logger.info("Subscribing to topic : " + topic);
					_client.subscribe(topic, subQoS);
				}
			}

		} catch (MqttException me) {
			_logger.error("Error connecting to mqtt broker", me);

			SMSDto sms = new SMSDto();
			sms.setMessage("Error occured in mqtt helper, review error log for more details");
			_smsGammuService.sendMessage(sms, true);
		}
	}

	public void disconnect() {
		try {
			_client.disconnect();
		} catch (MqttException me) {
			_logger.error("Error disconnecting to mqtt broker", me);
		}
	}

	/**
	 * Publishes message to Mqtt broker
	 * 
	 */
	public void PublishInfoToMqttBroker() {

		try {
			MqttMessage mqttMessage = new MqttMessage();

			for (Room room : _roomService.GetRooms()) {
				String message = FormatRoomInfoMessage(room.getRoomId());
				mqttMessage.setPayload(message.getBytes("UTF8"));
				_client.publish(room.getMqttTopic(), mqttMessage);
				// Thread.sleep(1000);
			}
			
			String message = FormatHomeInfoMessage();
			mqttMessage.setPayload(message.getBytes("UTF8"));
			_client.publish(HOME_INFO_MQTT_TOPIC, mqttMessage);
			
			
		} catch (MqttException me) {
			_logger.error("Error sending sensor value to mqtt broker", me);

			SMSDto sms = new SMSDto();
			sms.setMessage("Error occured in mqtt helper, review error log for more details");
			_smsGammuService.sendMessage(sms, true);
			
		} catch (UnsupportedEncodingException e) {
			_logger.error("Error when encoding message in UTF8", e);
		} catch (Exception e) {
			
			_logger.error("Error occured when publishing info to mqtt broker", e);
		}
		
	}

	@Override
	public void connectionLost(Throwable arg0) {
		_logger.warn("Mqtt connection lost...reconnecting...", arg0);
		connectAndSubscribe();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		_logger.info(String.format("Received topic : %s, Message : %s", arg0, new String(arg1.getPayload())));

		ProcessMessageReceived(new String(arg1.getPayload()));

	}

	private void ProcessMessageReceived(String message) {

		String[] messageContent = message.split(";");

		if (messageContent != null && messageContent.length > 0) {
			String action = messageContent[0];

			switch (action) {
			case "SETALARM":
				String mode = messageContent[1];
				if (mode.equals("ON")) {
					_alarmService.SetOn();
				}
				else {
					_alarmService.SetOff();
				}
				break;
			case "SETNEWSERIE":
				String serie = messageContent[1];				
				String mess1 = "Hey! you got a new serie to watch : " +serie;				
				SMSDto sms1 = new SMSDto();
				sms1.setMessage(mess1);
				_smsGammuService.sendMessage(sms1, true);
				break;
				
			case "SETROLLERSHUTTERMGT":
					String automaticManagement = messageContent[1];
										
					if (automaticManagement.equals("ON")) {
						_rollerShutterService.SetAutomaticManagementOn();
					}
					else {
						_rollerShutterService.SetAutomaticManagementOff();
					}
				break;
			
			case "SETAWAYMODE":
				String awayMode = messageContent[1];

				if (awayMode.equals("ON")) {
					_roomService.SetAwayModeOn();
					_waterHeaterService.SetAwayModeOn();
					_rollerShutterService.SetAutomaticManagementOff();					
				} else {
					_roomService.SetAwayModeOff();
					_waterHeaterService.SetAwayModeOff();					
				}

				break;
			case "SETGOTMAIL":
				try {

					long diffMinutes = 0;
					if (_lastGotMailMessage != null) {
						Date currentDate = new Date();
						long diff = currentDate.getTime() - _lastGotMailMessage.getTime();
						diffMinutes = diff / (60 * 1000);
					}

					// Send new SMS only if last message was sent more then one
					// hour before
					// Need to do that so I am not receiving lot of messages
					// during strong wind (it opens my mailbox!!)
					if (_lastGotMailMessage == null || diffMinutes > 60) {

						float vcc = Float.parseFloat(messageContent[1]);

						_dbManager.SaveMailboxSensorInfoInfluxDB(vcc);

						String mess = "Hey! you got mail ! by the way, vcc sensor is " + vcc;
						/*
						 * MailService mailService = new MailService();
						 * mailService.SendMailSSL("Hey! you got mail", mess);
						 */

						SMSDto sms = new SMSDto();
						sms.setMessage(mess);
						_smsGammuService.sendMessage(sms, true);
					}

					_lastGotMailMessage = new Date();

				} catch (Exception e) {
					_logger.error("Could not read or save information received from mailbox", e);
				}
				break;

			case "SETROOMTEMP":
				long roomId = Long.parseLong(messageContent[1]);

				try {
					float wantedTempFloat = Float.parseFloat(messageContent[2]);
					_roomService.SetWantedTemp(roomId, wantedTempFloat);
				} catch (Exception e) {
					_logger.error("Erreur de conversion dans la temp désirée par l'utilisateur", e);
				}
				break;

			case "SETCAVEINFO":
				try {
					
					float temp = Float.parseFloat(messageContent[1]);
					float humi = Float.parseFloat(messageContent[2]);
					String extractorState = messageContent[3];

					_dbManager.SaveCaveInfoToInfluxDb(temp, humi, extractorState);
					
					_lastBasementMessage = new Date();

				} catch (Exception e) {
					_logger.error("Could not read or save information received from basement", e);
				}
				break;
			case "SETOUTSIDEINFO":

				try {
					float garageTemp = Float.parseFloat(messageContent[1]);
					float pressure = Float.parseFloat(messageContent[2]);
					float altitude = Float.parseFloat(messageContent[3]);
					float outsideTemp = Float.parseFloat(messageContent[4]);

					_dbManager.SaveOutsideSensorsInfo(outsideTemp, garageTemp, pressure, altitude);
				} catch (Exception e) {
					_logger.error("Could not read or save information received from outside", e);
				}
				break;
			case "SETINSIDEINFO":
				try {
					long sensorId = Long.parseLong(messageContent[1]);
					float temp = Float.parseFloat(messageContent[2]);
					float humidity = Float.parseFloat(messageContent[3]);

					for (Room room : _roomService.GetRooms()) {
						if (room.getSensor().getIdSendor() == sensorId) {
							room.getSensor().setReceivedValue(temp, humidity);
							// _logger.info("Received WiFi sensor info => id: "
							// + sensorId+ ", temp: "+temp+", hum: "+humidity);
							break;
						}
					}
				} catch (Exception e) {
					_logger.error("Could not process message : " + message, e);
				}
				break;

			default:
				_logger.error("Could not process MQTT message : " + message);
			}
		} else

		{
			_logger.error("Could not process MQTT message : " + message);
		}
	}

	private String FormatHomeInfoMessage() {
		
		String papp = "NA";
		String hchc = "NA";
		String hchp = "NA";	
		long diffMinutes = 0;

		// TODO : Creer un message mqtt /guiet/home/info
		if (_teleInfoService.GetLastTrame() != null) {
			hchc = Integer.toString(_teleInfoService.GetLastTrame().HCHC);
			hchp = Integer.toString(_teleInfoService.GetLastTrame().HCHP);
			papp = Integer.toString(_teleInfoService.GetLastTrame().PAPP);	
		}
		
		if (_lastComputeBillCost != null) { 		
			Date currentDate = new Date();
			long diff = currentDate.getTime() - _lastComputeBillCost.getTime();
			diffMinutes = diff / (60 * 1000);
		}
		else {
			diffMinutes = 61;
		}
		
		//Compute Bill Cost every one hour
		if (diffMinutes >= 60) {			
			_electricityBill = Float.toString(_teleInfoService.GetNextElectricityBillCost());
			_lastComputeBillCost = new Date();
			
		}	
		
		String awayModeStatus = _roomService.GetAwayModeStatus();
		String automaticManagementStatus = _rollerShutterService.GetAutomaticManagementStatus();
		String westRSState = _rollerShutterService.getWestRSState();
		
		String message = hchc + ";" + hchp + ";" + papp + ";" + awayModeStatus + ";" + _electricityBill + ";" + automaticManagementStatus + ";" + westRSState;
		
		return message;
	}
	
	private String FormatRoomInfoMessage(long roomId) {

		String actualTemp = "NA";
		if (_roomService.GetActualTemp(roomId) != null) {
			actualTemp = String.format("%.2f", _roomService.GetActualTemp(roomId));
		}

		String wantedTemp = "NA";
		if (_roomService.GetWantedTemp(roomId) != null) {
			wantedTemp = String.format("%.2f", _roomService.GetWantedTemp(roomId));
		}

		String actualHumidity = "NA";
		if (_roomService.GetActualHumidity(roomId) != null) {
			actualHumidity = String.format("%.2f", _roomService.GetActualHumidity(roomId));
		}

		String nextDefaultTemp = _roomService.NextChangeDefaultTemp(roomId);

		String hasHeaterOn = "HEATEROFF";
		if (_roomService.AtLeastOneHeaterOn(roomId)) {
			hasHeaterOn = "HEATERON";
		}

		String progTemp = "NA";
		Float tempProg = _roomService.GetTempProg(roomId);
		if (tempProg != null) {
			progTemp = String.format("%.2f", tempProg);
		}

		String offForced = "FORCEDHEATEROFF";
		if (_roomService.IsOffForced(roomId)) {
			offForced = "FORCEDHEATERON";
		}

		String sensorKO = "SENSORKO";
		if (_roomService.IsSensorResponding(roomId)) {
			sensorKO = "SENSOROK";
		}

		// Last info received from sensor
		String lastInfoReceveid = _roomService.LastInfoReceived(roomId);

		

		/*String message = actualTemp + ";" + actualHumidity + ";" + progTemp + ";" + nextDefaultTemp + ";" + hasHeaterOn
				+ ";" + offForced + ";" + sensorKO + ";" + wantedTemp + ";" + hchc + ";" + hchp + ";" + papp + ";"
				+ awayModeStatus + ";" + lastInfoReceveid;*/
		
		String message = actualTemp + ";" + actualHumidity + ";" + progTemp + ";" + nextDefaultTemp + ";" + hasHeaterOn
		+ ";" + offForced + ";" + sensorKO + ";" + wantedTemp + ";" + lastInfoReceveid;
		
		return message;
	}

}
