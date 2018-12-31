package fr.guiet.automationserver.business;

//import com.rapplogic.xbee.api.XBeeAddress64;
import org.apache.log4j.Logger;

import java.util.Date;
import fr.guiet.automationserver.dto.*;

//public class Sensor implements IXBeeListener {
public class Sensor {

	private static Logger _logger = Logger.getLogger(Sensor.class);

	private long _idSensor;
	private float _tempshift; //Correct sensor value (maybe incorrect due to different room factor (place, not well calibrated, etc)
	private String _name;
	private Date _lastInfoReceived = null; // Date de derniere reception d'une
											// info du capteur
	private float _actualTemp;
	private float _actualHumidity;
	private float _battery; //Battery voltage
	private float _rssi;
	private Room _room = null;
	private SMSGammuService _smsGammuService = null;
	private boolean _alertSent5 = false;
	private boolean _alertSent10 = false;
	private boolean _alertSentMore = false;


	public float getActualTemp() {
		return _actualTemp;
	}
	
	public String lastInfoReceived() {
		
		if (_lastInfoReceived != null) {
			return DateUtils.getDateToString(_lastInfoReceived);
		}
		else {
			return "NA";
		}
		
	}

	public void setReceivedValue(float actualTemp, float actualHumidity, float battery, float rssi) {
		_actualHumidity = actualHumidity;		
		_actualTemp = actualTemp;
		_battery = battery;
		_rssi = rssi;
		
		//DHT22 is inside a box and temp reading is incorrect (warn from ESP8266)
		_actualTemp = _actualTemp + _tempshift;
				
		_lastInfoReceived = new Date();
	}

	public float getBattery() {
		return _battery;
	}
	
	public float getRssi() {
		return _rssi;
	}
	
	public float getActualHumidity() {
		return _actualHumidity;
	}

	/*private static XBeeService XBeeInstance() {
		synchronized (_lockObj) {
			if (_XBeeInstance == null) {
				_XBeeInstance = new XBeeService();
			}
		}

		return _XBeeInstance;
	}*/

	// TODO : Timeout must be handled by sensor itself (via timer, not by
	// method)
	public boolean HasTimeoutOccured() {

		// Si pas encore recu d'info, on considere que c'est un timeout
		if (_lastInfoReceived == null)
			return true;

		Date currentDate = new Date();

		long diff = currentDate.getTime() - _lastInfoReceived.getTime();
		long diffMinutes = diff / (60 * 1000);

		if (diffMinutes > 5 && diffMinutes < 10) {

			if (!_alertSent5) {

				SMSDto sms = new SMSDto();
				String message = String.format("Sensor %s does not send messages anymore (5 minutes alert)", _name);
				sms.setMessage(message);
				_smsGammuService.sendMessage(sms, true);

				_alertSent5 = true;
			}

			return true;
		}

		if (diffMinutes >= 10 && diffMinutes < 20) {

			if (!_alertSent10) {

				SMSDto sms = new SMSDto();
				String message = String.format("Sensor %s does not send messages anymore (10 minutes alert)", _name);
				sms.setMessage(message);
				_smsGammuService.sendMessage(sms, true);

				_alertSent10 = true;
			}

			return true;
		}

		if (diffMinutes >= 20) {

			if (!_alertSentMore) {

				SMSDto sms = new SMSDto();
				String message = String.format(
						"Sensor %s does not send messages anymore (20 minutes alert)...Time to do something", _name);
				sms.setMessage(message);
				_smsGammuService.sendMessage(sms, true);

				_alertSentMore = true;
			}

			return true;
		}

		_alertSent5 = false; // Réinitialisation
		_alertSent10 = false; // Réinitialisation
		_alertSentMore = false; // Réinitialisation

		return false;

	}

	// Reception d'un message
	/*public void processResponse(String message) {

		try {

			// _logger.info(_room.getName()+ " - Reception message : "+message);

			String[] messageArray = message.split(";");

			// _logger.info(_room.getName()+ " - " + messageArray[0]);

			if (messageArray[0].equals("SETSENSORINFO")) {
				_lastInfoReceived = new Date();

				// _logger.info("ouhou");

				String actualTemp = messageArray[1];
				// wantedTemp = messageArray[2];

				String actualHumidity;
				// Pour la gestion des deux versions
				if (messageArray.length == 4)
					// Ancienne version du message recu des Xbee
					actualHumidity = messageArray[3];
				else {
					// Nouvelle version du message recu des Xbee
					actualHumidity = messageArray[2];
				}

				_actualTemp = Float.parseFloat(actualTemp);
				_actualHumidity = Float.parseFloat(actualHumidity);

				/*
				 * DbManager dbManager = new DbManager();
				 * dbManager.SaveSensorInfo(_idSensor, _actualTemp,
				 * _room.getWantedTemp(), _actualHumidity); _logger.
				 * info("Sauvegardee en base de donnees des infos du capteur pour la piece : "
				 * + _room.getName() + ", Temp actuelle : " + _actualTemp +
				 * ", Temp désirée : " + _room.getWantedTemp() + ", Humidité : "
				 * + _actualHumidity);
				 * 
				 * dbManager.SaveSensorInfoInfluxDB(_influxdbMeasurement,
				 * _actualTemp, _room.getWantedTemp(), _actualHumidity);
				 */
		//	}
		//} catch (Exception e) {
	//		_logger.error("Erreur lors du traitement du message reçu pour la piece : " + _room.getName()
	//				+ ", message recu : " + message, e);
	//		_lastInfoReceived = null;
	//	}

	//}

	/*public String sensorAddress() {
		return _sensorAddress.toString();
	}*/

	public void StopService() {

		/*if (_timer != null)
			_timer.cancel();*/

		/*if (!XBeeInstance().isStopped()) {
			XBeeInstance().StopService();
		}*/

		_logger.info("Arrêt du capteur de la pièce : " + _room.getName());

	}

	// Creation de la tache de recuperation des infos des capteurs
	/*private void CreateGetSensorInfoTask() {

		TimerTask getSensorInfoTask = new TimerTask() {
			@Override
			public void run() {
				GetSensorInfo();
			}
		};

		_timer = new Timer(true);
		// Toutes les minutes on enregistre une trame
		_timer.schedule(getSensorInfoTask, 5000, 30000);
	}*/

	// Envoi d'un message pour recuperer les infos du capteur
	/*private void GetSensorInfo() {

		// XBeeService xbee = new XBeeService(_sensorAddress);
		XBeeInstance().SendAsynchronousMessage(_sensorAddress, "GETSENSORINFO");
	}*/

	private Sensor(SensorDto dto, Room room, SMSGammuService gammuService) {

		_idSensor = dto.sensorId;
		_room = room;
		_name = dto.name;
		_smsGammuService = gammuService;
		_tempshift = dto.tempshift;
		// _mqttHelper = new MqttHelper(gammuService);

		//TODO : soon each sensor will wifi sensor no more xbee
		/*if (dto.sensorAddress != null) {

			String[] address = dto.sensorAddress.split(" ");

			int i1 = Integer.parseInt(address[0], 16);
			int i2 = Integer.parseInt(address[1], 16);
			int i3 = Integer.parseInt(address[2], 16);
			int i4 = Integer.parseInt(address[3], 16);
			int i5 = Integer.parseInt(address[4], 16);
			int i6 = Integer.parseInt(address[5], 16);
			int i7 = Integer.parseInt(address[6], 16);
			int i8 = Integer.parseInt(address[7], 16);

			_sensorAddress = new XBeeAddress64(i1, i2, i3, i4, i5, i6, i7, i8);

			//CreateGetSensorInfoTask();
			//XBeeInstance().addXBeeListener(this);
		}*/
	}

	public static Sensor LoadFromDto(SensorDto dto, Room room, SMSGammuService gammuService) {

		return new Sensor(dto, room, gammuService);
	}

	/*public XBeeAddress64 getSensorAddress() {
		return _sensorAddress;
	}*/

	public long getIdSendor() {
		return _idSensor;
	}
}
