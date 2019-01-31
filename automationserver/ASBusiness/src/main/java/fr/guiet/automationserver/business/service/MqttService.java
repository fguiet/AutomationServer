package fr.guiet.automationserver.business.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import fr.guiet.automationserver.business.RollerShutterState;

import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;

public class MqttService implements MqttCallbackExtended {

	private static Logger _logger = Logger.getLogger(MqttClient.class);
	private String _uri = "tcp://%s:%s";
	private final String CLIENT_ID = "Java Automation Server";
	private SMSGammuService _smsGammuService = null;
	private MqttClient _client = null;
	private String[] _topics = null;
	private RoomService _roomService = null;
	private TeleInfoService _teleInfoService = null;
	private WaterHeaterService _waterHeaterService = null;
	// private Print3DService _print3DService = null;
	private AlarmService _alarmService = null;
	// private ScenariiManager _scenariiManager = null;
	private RollerShutterService _rollerShutterService = null;
	// private BLEHubService _BLEHubService = null;
	private DbManager _dbManager = null;
	private Date _lastGotMailMessage = null;
	private final String HOME_INFO_MQTT_TOPIC = "/guiet/home/info";
	// private Date _lastBasementMessage = new Date();
	private Date _lastComputeBillCost = null;
	private String _electricityBill = "NA";

	private ArrayList<IMqttable> _mqttClients = new ArrayList<IMqttable>();

	public void addClient(IMqttable client) {
		_mqttClients.add(client);
	}

	public void addClients(ArrayList<IMqttable> clients) {
		_mqttClients.addAll(clients);
	}

	// TODO : when RoomService will be IMqttable...please remove that horror
	public void setRoomService(RoomService roomService) {
		_roomService = roomService;
	}

	// TODO : when teleInfoService will be IMqttable...please remove that horror
	public void setTeleInfoService(TeleInfoService teleInfoService) {
		_teleInfoService = teleInfoService;
	}

	// TODO : when waterHeaterService will be IMqttable...please remove that horror
	public void setWaterHeaterService(WaterHeaterService waterHeaterService) {
		_waterHeaterService = waterHeaterService;
	}

	// TODO : when rollerShutterService will be IMqttable...please remove that horror
	public void setRollerShutterService(RollerShutterService rollerShutterService) {
		_rollerShutterService = rollerShutterService;
	}

	public MqttService(SMSGammuService gammuService) {

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

			//_waterHeaterService = waterHeaterService;
			//_alarmService = alarmService;
			//_rollerShutterService = rollerShutterService;
			// _scenariiManager = scenariiManager;
			// _print3DService = print3DService;
			// _BLEHubService = BLEHubService;
			_dbManager = new DbManager();

		} catch (FileNotFoundException e) {
			_logger.error("Cannot find configuration file in classpath_folder/config/automationserver.properties", e);
		} catch (IOException e) {
			_logger.error("Error in reading configuration file classpath_folder/config/automationserver.properties", e);
		}

	}

	@Override
	public void connectComplete(boolean arg0, String arg1) {
		// arg0 true if automatic reconnect occured
		if (arg0) {
			_logger.info("Automatic Mqtt reconnection occured !");
			subscribe();
		}

	}

	private void subscribe() {

		int subQoS = 0;

		try {

			// TODO : 2019/01/19 will be removed
			for (String topic : _topics) {
				if (!topic.equals("")) {
					_logger.info("Subscribing to topic : " + topic);
					_client.subscribe(topic, subQoS);
				}
			}

			for (IMqttable c : _mqttClients) {
				for (String t : c.getTopics()) {
					_logger.info("Subscribing to topic : " + t);
					_client.subscribe(t, subQoS);
				}
			}

		} catch (Exception e) {
			_logger.error("Error while subscribing to mqtt topic", e);

			SMSDto sms = new SMSDto("b7e40b31-abf3-4a3f-bca6-934cbbda4dbf");
			sms.setMessage("Error while subscribing to mqtt topic, review error log for more details");
			_smsGammuService.sendMessage(sms, true);
		}
	}

	public void connectAndSubscribe() {
		try {
			_client = new MqttClient(_uri, CLIENT_ID);

			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			options.setKeepAliveInterval(30);
			options.setAutomaticReconnect(true);

			_client.setCallback(this);
			_client.connect();

			subscribe();

			_logger.info("Client : " + CLIENT_ID + " has connected to mqtt broker !");

		} catch (MqttException me) {
			_logger.error("Error while connecting to mqtt broker", me);

			SMSDto sms = new SMSDto("f2d0fb97-4f95-4eb8-a5de-18ab8bdd0b4d");
			sms.setMessage("Error occured in mqtt helper (MqttException), review error log for more details");
			_smsGammuService.sendMessage(sms, true);
		} catch (Exception e) {
			_logger.error("Error while connecting to mqtt broker", e);

			SMSDto sms = new SMSDto("a8730a13-b209-430e-a517-9d5aff75dc4c");
			sms.setMessage("Error occured in mqtt helper (Exception), review error log for more details");
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

	public boolean IsMqttServerAvailable() {

		boolean isMqttStarted = false;
		try {
			MqttClient client = new MqttClient(_uri, CLIENT_ID);

			client.connect();

			if (client.isConnected()) {
				isMqttStarted = true;
				// Issue :
				// https://github.com/eclipse/paho.mqtt.java/issues/402
				// client.disconnect();
				client.disconnectForcibly();
				client.close(true);
				client = null;
			}
		} catch (Exception e) {
			_logger.error("Mqtt instance not ready...", e);
		}

		return isMqttStarted;
	}

	public void SendMsg(String topic, String message) {

		try {
			MqttMessage mqttMessage = new MqttMessage();
			mqttMessage.setPayload(message.getBytes("UTF8"));
			_client.publish(topic, mqttMessage);

		} catch (MqttException me) {
			_logger.error("Error sending message to mqtt broker", me);
		} catch (UnsupportedEncodingException e) {
			_logger.error("Error when encoding message in UTF8", e);
		} catch (Exception e) {
			_logger.error("Error occured when sending message to mqtt broker", e);
		}
	}

	/**
	 * Publishes message to Mqtt broker
	 * 
	 */
	public void PublishInfoToMqttBroker() {

		try {
			MqttMessage mqttMessage = new MqttMessage();

			String message = FormatHomeInfoMessage();
			mqttMessage.setPayload(message.getBytes("UTF8"));

			_client.publish(HOME_INFO_MQTT_TOPIC, mqttMessage);

		} catch (MqttException me) {
			_logger.error("Error sending sensor value to mqtt broker", me);

			SMSDto sms = new SMSDto("3b6f870b-97f3-496f-8f47-b4dc17a642f9");
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
		_logger.warn("Mqtt connection lost...automatic reconnection is going to occur...", arg0);
		// connectAndSubscribe();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		_logger.info(String.format("Received topic : %s, Message : %s", arg0, new String(arg1.getPayload())));

		ProcessMessageReceived(arg0, new String(arg1.getPayload()));
	}

	private void ProcessMessageReceived(String topic, String message) {

		boolean messageProcessed = false;

		// Process Mqtt message!
		for (IMqttable c : _mqttClients) {
			messageProcessed = c.ProcessMqttMessage(topic, message);

			// TODO : Remove this horror
			// Change outsidemonitoring to send two topics for the two sensors
			if (messageProcessed && !topic.equals("/guiet/outside/sensorsinfo")) {
				return;
			}
		}

		// TODO : Will be removed!
		String[] messageContent = message.split(";");

		if (messageContent != null && messageContent.length > 0) {
			String action = messageContent[0];

			switch (action) {
			/*case "SETALARM":
				String mode = messageContent[1];
				if (mode.equals("ON")) {
					_alarmService.SetOn();
				} else {
					_alarmService.SetOff();
				}
				break;*/
			case "SETNEWSERIE":
				String serie = messageContent[1];
				String mess1 = "Hey! you got a new serie to watch : " + serie;
				SMSDto sms1 = new SMSDto("238588b4-767f-484b-8cb6-d867e267bdd6");
				sms1.setMessage(mess1);
				_smsGammuService.sendMessage(sms1, true);
				break;

			case "SETROLLERSHUTTERMGT":
				String automaticManagement = messageContent[1];

				if (automaticManagement.equals("ON")) {
					_rollerShutterService.SetAutomaticManagementOn();
				} else {
					_rollerShutterService.SetAutomaticManagementOff();
				}
				break;

			case "SETALARMMGT":
				String automaticAlarmManagement = messageContent[1];

				if (automaticAlarmManagement.equals("ON")) {
					_alarmService.SetAutomaticModeOn();
				} else {
					_alarmService.SetAutomaticModeOff();
				}
				break;

			/*
			 * case "SETHOMEMODE": String homeMode = messageContent[1];
			 * //_scenariiManager.SetHomeModeState(homeMode); break;
			 */
			case "SETAWAYMODE":
				String awayMode = messageContent[1];

				if (awayMode.equals("ON")) {
					_roomService.SetAwayModeOn();
					_waterHeaterService.SetAwayModeOn();
					_rollerShutterService.SetAutomaticManagementOff();
					_alarmService.SetAutomaticModeOff();
					// _scenariiManager.SetHomeModeState("NOTACTIVATED");
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

						SMSDto sms = new SMSDto("fdaed23f-92d6-4334-8aec-bf39be62f3aa");
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

			/*
			 * case "SETCAVEINFO": try {
			 * 
			 * float temp = Float.parseFloat(messageContent[1]); float humi =
			 * Float.parseFloat(messageContent[2]); String extractorState =
			 * messageContent[3];
			 * 
			 * _dbManager.SaveCaveInfoToInfluxDb(temp, humi, extractorState);
			 * 
			 * _lastBasementMessage = new Date();
			 * 
			 * } catch (Exception e) {
			 * _logger.error("Could not read or save information received from basement",
			 * e); } break;
			 */
			/*
			 * case "SETOUTSIDEINFO":
			 * 
			 * try { float garageTemp = Float.parseFloat(messageContent[1]); float pressure
			 * = Float.parseFloat(messageContent[2]); float altitude =
			 * Float.parseFloat(messageContent[3]); float outsideTemp =
			 * Float.parseFloat(messageContent[4]);
			 * 
			 * _dbManager.SaveOutsideSensorsInfo(outsideTemp, garageTemp, pressure,
			 * altitude);
			 * 
			 * } catch (Exception e) {
			 * _logger.error("Could not read or save information received from outside", e);
			 * } break;
			 */
			case "SETRSSTATE":
				long rsId = Long.parseLong(messageContent[1]);
				String state = messageContent[2];

				switch (state) {
				case "UNDETERMINED":
					_rollerShutterService.setState(rsId, RollerShutterState.UNDETERMINED);
					break;
				case "OPENED":
					_rollerShutterService.setState(rsId, RollerShutterState.OPENED);
					break;
				case "CLOSED":
					_rollerShutterService.setState(rsId, RollerShutterState.CLOSED);
					break;
				case "ERROR":
					_rollerShutterService.setState(rsId, RollerShutterState.ERROR);
					break;
				}

				break;
			case "SETBOILERINFO":
				try {
					// long sensorId = Long.parseLong(messageContent[1]);
					float temp = Float.parseFloat(messageContent[2]);

					_waterHeaterService.SaveTemp(temp);

				} catch (Exception e) {
					_logger.error("Could not process message : " + message, e);
				}
				break;
			/*
			 * case "SETINSIDEINFO": try { long sensorId =
			 * Long.parseLong(messageContent[1]); float temp =
			 * Float.parseFloat(messageContent[2]); float humidity =
			 * Float.parseFloat(messageContent[3]);
			 * 
			 * //100% per default if power operated float battery = 100;
			 * 
			 * if (messageContent[4] != null) battery = Float.parseFloat(messageContent[4]);
			 * 
			 * //Rssi 0 per default if power operated float rssi = 0;
			 * 
			 * if (messageContent.length >= 6) { rssi = Float.parseFloat(messageContent[5]);
			 * }
			 * 
			 * for (Room room : _roomService.GetRooms()) { if
			 * (room.getSensor().getIdSendor() == sensorId) {
			 * room.getSensor().setReceivedValue(temp, humidity, battery, rssi); break; } }
			 * } catch (Exception e) { _logger.error("Could not process message : " +
			 * message, e); } break;
			 */

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
		} else {
			diffMinutes = 61;
		}

		// Compute Bill Cost every one hour
		if (diffMinutes >= 60) {
			_electricityBill = Float.toString(_teleInfoService.GetNextElectricityBillCost());
			_lastComputeBillCost = new Date();
		}

		String awayModeStatus = _roomService.GetAwayModeStatus();
		String automaticManagementStatus = _rollerShutterService.GetAutomaticManagementStatus();
		String westRSState = _rollerShutterService.getWestRSState();
		String northRSState = _rollerShutterService.getNorthRSState();
		String alarmAutomaticManagementStatus = _alarmService.GetAutomaticModeStatus();
		// String homeModeStatus = _scenariiManager.GetHomeModeStatus();

		// Boiler Ino
		// Last info received from sensor
		String lastBoilerInfoReceveid = _waterHeaterService.LastInfoReceived();
		String lastBoilerOnDuration = _waterHeaterService.LastOnDuration();
		String boilerState = _waterHeaterService.getState();

		String actualBoilerTemp = "NA";
		if (_waterHeaterService.getActualTemp() != null) {
			actualBoilerTemp = String.format("%.2f", _waterHeaterService.getActualTemp());
		}

		String message = hchc + ";" + hchp + ";" + papp + ";" + awayModeStatus + ";" + _electricityBill + ";"
				+ automaticManagementStatus + ";" + westRSState + ";" + alarmAutomaticManagementStatus;
		message = message + ";" + "NA" + ";" + "NA";
		message = message + ";" + northRSState + ";" + "NA" + ";" + lastBoilerInfoReceveid + ";" + lastBoilerOnDuration
				+ ";" + boilerState + ";" + actualBoilerTemp;
		// message = message + ";" + _scenariiManager.getNextRSOpenDate() + ";" +
		// _scenariiManager.getNextRSCloseDate();
		// message = message + ";" + northRSState + ";" + homeModeStatus + ";" +
		// lastBoilerInfoReceveid + ";" + lastBoilerOnDuration + ";" + boilerState + ";"
		// + actualBoilerTemp;

		return message;
	}

	/*
	 * private String FormatRoomInfoMessage(long roomId) {
	 * 
	 * String actualTemp = "NA"; if (_roomService.GetActualTemp(roomId) != null) {
	 * actualTemp = String.format("%.2f", _roomService.GetActualTemp(roomId)); }
	 * 
	 * String wantedTemp = "NA"; if (_roomService.GetWantedTemp(roomId) != null) {
	 * wantedTemp = String.format("%.2f", _roomService.GetWantedTemp(roomId)); }
	 * 
	 * String actualHumidity = "NA"; if (_roomService.GetActualHumidity(roomId) !=
	 * null) { actualHumidity = String.format("%.2f",
	 * _roomService.GetActualHumidity(roomId)); }
	 * 
	 * String nextDefaultTemp = _roomService.NextChangeDefaultTemp(roomId);
	 * 
	 * String hasHeaterOn = "HEATEROFF"; if
	 * (_roomService.AtLeastOneHeaterOn(roomId)) { hasHeaterOn = "HEATERON"; }
	 * 
	 * String progTemp = "NA"; Float tempProg = _roomService.GetTempProg(roomId); if
	 * (tempProg != null) { progTemp = String.format("%.2f", tempProg); }
	 * 
	 * String offForced = "FORCEDHEATEROFF"; if (_roomService.IsOffForced(roomId)) {
	 * offForced = "FORCEDHEATERON"; }
	 * 
	 * String sensorKO = "SENSORKO"; if (_roomService.isSensorOperational(roomId)) {
	 * sensorKO = "SENSOROK"; }
	 * 
	 * String lastSensorUpdate = _roomService.getLastSensorUpdate(roomId);
	 * 
	 * // Battery String battery = "NA"; if (_roomService.GetBattery(roomId) !=
	 * null) { battery = String.format("%.2f", _roomService.GetBattery(roomId)); }
	 * 
	 * // Rssi String rssi = "NA"; if (_roomService.GetBattery(roomId) != null) {
	 * rssi = String.format("%.2f", _roomService.GetRssi(roomId)); }
	 * 
	 * String message = actualTemp + ";" + actualHumidity + ";" + progTemp + ";" +
	 * nextDefaultTemp + ";" + hasHeaterOn + ";" + offForced + ";" + sensorKO + ";"
	 * + wantedTemp + ";" + lastSensorUpdate + ";" + battery + ";" + rssi;
	 * 
	 * return message; }
	 */
}
