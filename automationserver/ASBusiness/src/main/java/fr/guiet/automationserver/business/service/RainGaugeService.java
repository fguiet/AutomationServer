package fr.guiet.automationserver.business.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;

import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;

public class RainGaugeService implements Runnable {

	private static Logger _logger = LogManager.getLogger(RainGaugeService.class);
	private String _defaultDevice = "";
	private Serial _serial = null;
	private boolean _isStopped = false; // Service arrete?
	private SMSGammuService _smsGammuService = null;
	private DbManager _dbManager = null;
	// private static String _mqttClientId = "rainGaugeCliendId";
	private MqttService _mqttService = null;
	private String _pub_topic = "/guiet/automationserver/raingauge";
	private Date _lastMessageReceived = new Date();

	public RainGaugeService(SMSGammuService smsGammuService, MqttService mqttService) {
		_smsGammuService = smsGammuService;
		_mqttService = mqttService;
		_dbManager = new DbManager();
		
		_serial = SerialFactory.createInstance();
	}

	private void CloseSerialConnection(boolean killSerialFactory) {

		try {
			if (_serial.isOpen()) {
				_logger.info("Fermeture port serie pour la RainGauge");

				_serial.discardInput();
				_serial.close();
			}
			
			if (killSerialFactory)
				SerialFactory.shutdown();
			
		} catch (IOException ioe) {

			_logger.error("Impossible de fermer le port serie pour la RainGauge", ioe);
		}
	}

	private String ReadRainGaugeMessage() {
		try {

			// Check serial connection
			if (!OpenSerialConnection())
				return null;

			// Data available in buffer ?
			if (_serial.available() > 0) {

				byte[] byteArray = _serial.read();

				String serialData = new String(byteArray, "UTF-8");
				_logger.info("Received RainGauge message : " + serialData);

				return serialData;
			}
			
			return null;

		} catch (Exception e) {
			_logger.info("Erreur while reading RainGauge message", e);

			CloseSerialConnection(false);

			return null;
		}
	}

	// Opening Serial Connection
	private boolean OpenSerialConnection() {

		boolean isOk = true;

		try {

			if (_serial.isClosed()) {

				// open the default serial port provided on the GPIO header at 1200
				// bauds
				// serial.open(_defaultDevice, _defaultBaud);
				SerialConfig config = new SerialConfig();
				config.device(_defaultDevice).baud(Baud._9600).dataBits(DataBits._7).parity(Parity.EVEN)
						.stopBits(StopBits._1).flowControl(FlowControl.NONE);

				_serial.setBufferingDataReceived(true);

				_serial.open(config);
				_logger.info("Ouverture du port serie pour la RainGauge effectué avec succès...");

			}

		} catch (IOException e) {

			isOk = false;

			_logger.error("Impossible d'ouvrir le port série pour la RainGauge", e);
		}

		return isOk;
	}

	@Override
	public void run() {

		_logger.info("Starting RainGaugeService...");

		InputStream is = null;
		try {
			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);

			_defaultDevice = prop.getProperty("raingauge.usbdevice");

		} catch (FileNotFoundException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		} catch (IOException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		}

		_logger.info("Using serial device : " + _defaultDevice);

		// CreateSerialInstance();

		while (!_isStopped) {

			try {

				String message = ReadRainGaugeMessage();

				if (message != null) {

					String[] messageContent = message.split(";");

					if (messageContent != null && messageContent.length > 0) {

						String action = messageContent[0];

						switch (action) {
						case "SETRAINGAUGEINFO":
							_lastMessageReceived = new Date();
							
							float vcc = Float.parseFloat(messageContent[1]);
							String flipflop = messageContent[2];
							_mqttService.SendMsg(_pub_topic, message);

							_dbManager.SaveRainGaugeInfo(vcc, flipflop);
						}
					}
				}

				Thread.sleep(2000);
				
				Long elapsedTime = DateUtils.minutesBetweenDate(_lastMessageReceived, new Date());
				
				if (elapsedTime >= 75) {
					String mess = "Aucune nouvelle du pluviomètre depuis 1h15, tentative de relance d'une instance sur le port série";

					_logger.info(mess);

					SMSDto sms = new SMSDto("94997f96-968f-4814-ad25-1e5190d59b13");
					sms.setMessage(mess);
					_smsGammuService.sendMessage(sms);
					
					//Reset
					_lastMessageReceived = new Date();
					
					CloseSerialConnection(false);
				}
				

			} catch (Exception e) {
				_logger.error("Error occured in RainGauge service...", e);

				SMSDto sms = new SMSDto("5ac9056c-599d-4e0f-8a02-3eb9b782d0ba");
				sms.setMessage("Error occured in RainGauge services service, review error log for more details");
				_smsGammuService.sendMessage(sms);
			}
		}
	}

	// Arret du service TeleInfoService
	public void StopService() {

		CloseSerialConnection(true);

		_logger.info("Stopping RainGauge service...");

		_isStopped = true;
	}
}