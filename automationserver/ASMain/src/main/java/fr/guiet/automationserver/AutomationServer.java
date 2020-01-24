package fr.guiet.automationserver;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.guiet.automationserver.business.helper.GpioHelper;
import fr.guiet.automationserver.business.service.*;
import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;

/**
 * Main class : in charge of home IoT management
 * 
 * Creation date : 201602
 * 
 * @author guiet
 *
 */
public class AutomationServer implements Daemon {

	private Thread _mainThread = null; // Thread principal
	private AlarmService _alarmService = null; // Alarm service

	private WaterMeterService _waterMeterService = null;
	private TeleInfoService _teleInfoService = null; // service de teleinfo
	private LoRaService _loRaService = null; // service raingauge
	private RoomService _roomService = null; // service de room service
	private WaterHeaterService _waterHeater = null; // service de gestion du chauffe-eau
	private RollerShutterService _rollerShutterService = null;
	private BLEHubService _BLEHubService = null;
	private boolean _isStopped = false;
	private Thread _waterMeterServiceThread = null;
	private Thread _roomServiceThread = null;
	private Thread _teleInfoServiceThread = null;
	private Thread _loRaServiceThread = null;
	private Thread _waterHeaterServiceThread = null;
	private Thread _rollerShutterServiceThread = null;
	private Thread _BLEHubServiceThread = null;
	private Thread _smsGammuServiceThread = null;
	private Print3DService _print3DService = null;
	private MailboxService _mailboxService = null;
	private OutsideEnvironmentalService _outsideEnvService = null;
	private SMSGammuService _smsGammuService = null;
	private MqttService _mqttService = null;
	
	//private ArrayList<Thread> _automationServiceThreadList = new ArrayList<Thread>();
	//private Queue<AbstractAutomationService> _automationServiceQueue = new LinkedList<AbstractAutomationService>();

	// Logger
	private static Logger _logger = LogManager.getLogger(AutomationServer.class);

	// Init deamon
	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
		/*
		 * Construct objects and initialize variables here. You can access the command
		 * line arguments that would normally be passed to your main() method as
		 * follows:
		 */
		// String[] args = daemonContext.getArguments();

		// Initialisation du Thread principal
		_mainThread = new Thread() {

			// Méthode de démarrage du Thread principale
			@Override
			public synchronized void start() {
				AutomationServer.this._isStopped = false;
				super.start(); // Démarrage du Thread principal (Appel de la
								// méthode run)
			}

			// Méthode principale du Thread principal (celle qui boucle)
			@Override
			public void run() {

				try {

					_logger.info("Starting automation server...");

					// Set local to en_GB
					Locale.setDefault(new Locale("en", "GB"));

					// SMS Service
					_smsGammuService = new SMSGammuService();
					_smsGammuServiceThread = new Thread(_smsGammuService);
					_smsGammuServiceThread.start();

					// MqttService
					_mqttService = new MqttService(_smsGammuService);

					// Starts system sanity checks
					DoSystemSanityChecks();
					
					_waterMeterService = new WaterMeterService(_smsGammuService, _mqttService);
					_waterHeaterServiceThread = new Thread(_waterMeterService);
					_waterHeaterServiceThread.start();

					// Starting rain gauge service
					_loRaService = new LoRaService(_smsGammuService, _mqttService);
					_loRaServiceThread = new Thread(_loRaService);
					_loRaServiceThread.start();

					// Starting teleinfo service
					_teleInfoService = new TeleInfoService(_smsGammuService, _mqttService);
					_teleInfoServiceThread = new Thread(_teleInfoService);
					_teleInfoServiceThread.start();

					// Starting water heater
					_waterHeater = new WaterHeaterService(_teleInfoService, _smsGammuService);
					_waterHeaterServiceThread = new Thread(_waterHeater);
					_waterHeaterServiceThread.start();

					// Start Rollershutter service
					_rollerShutterService = new RollerShutterService(_smsGammuService, _mqttService);
					_rollerShutterServiceThread = new Thread(_rollerShutterService);
					_rollerShutterServiceThread.start();

					// Start BLE Hub Service
					_BLEHubService = new BLEHubService(_smsGammuService);
					_BLEHubServiceThread = new Thread(_BLEHubService);
					_BLEHubServiceThread.start();

					// Start alarm service
					_alarmService = new AlarmService(_rollerShutterService, _smsGammuService);

					// Start Mailbox service
					_mailboxService = new MailboxService(_smsGammuService);

					// Start 3D Print service
					_print3DService = new Print3DService(_smsGammuService, _mqttService);

					// Start Outside Environmental service
					_outsideEnvService = new OutsideEnvironmentalService("Outside Environmental", _smsGammuService);
					
					// Starting room service
					_roomService = new RoomService(_teleInfoService, _smsGammuService, _mqttService);
					_roomServiceThread = new Thread(_roomService);
					_roomServiceThread.start();

					while (!_roomService.isRoomListLoaded()) {
						_logger.info("Waiting for room list to be fully loaded !");
						Thread.sleep(1000);
					}

					// TODO : Remove that horror when service will be IMqttable
					_mqttService.setRoomService(_roomService);
					// _mqttService.setTeleInfoService(_teleInfoService);
					_mqttService.setWaterHeaterService(_waterHeater);
					_mqttService.setRollerShutterService(_rollerShutterService);

					// _mqttService.setAlarmService(_alarmService);

					_mqttService.addClient(_mailboxService);
					_mqttService.addClient(_alarmService);
					_mqttService.addClient(_BLEHubService);
					_mqttService.addClient(_print3DService);
					_mqttService.addClients(_roomService.getMqttableClients());
					_mqttService.addClients(_outsideEnvService.getMqttableClients());
					_mqttService.addClient(_smsGammuService);

					_mqttService.connectAndSubscribe();

					SMSDto sms = new SMSDto("ba92bca9-a67f-4945-a6ee-51bf44bfaed5");
					sms.setMessage("Automation server has started...");
					_smsGammuService.sendMessage(sms);

					while (!_isStopped) {

						// Publication des données toutes les 10s
						Thread.sleep(10000);

						// TODO : please remove that!!!
						_mqttService.PublishInfoToMqttBroker();

					}
				} catch (Exception e) {
					_logger.error("Error occured in automation server...", e);

					SMSDto sms = new SMSDto("6b81daa5-eca5-49c4-bc58-53fa9cd192a6");
					sms.setMessage("Error occured in main loop !");
					_smsGammuService.sendMessage(sms);

				}
			}
		};
	}

	private void DoSystemSanityChecks() {

		DbManager dbManager = new DbManager();
		// MqttClientHelper mqttClient = new MqttClientHelper("SanityCheck");

		_logger.info("Starting system sanity check...");

		/*
		 * PostgreSQL
		 */
		_logger.info("Check for PostgreSQL Server availability...");

		int checkCounter = 0;
		while (!dbManager.IsPostgreSQLAvailable()) {
			checkCounter++;
			_logger.info("Attempt : " + checkCounter + " - PostgreSQL not available...will check again in 10s...");

			try {
				Thread.sleep(10000); // Wait for 10s
			} catch (Exception e) {

			}
		}

		_logger.info("PostgreSQL instance : OK");

		/*
		 * InfluxDB
		 */
		_logger.info("Check for InfluxDB Server availability...");

		checkCounter = 0;
		while (!dbManager.IsInfluxDbAvailable()) {
			checkCounter++;
			_logger.info("Attempt : " + checkCounter + " - InfluxDB not available...will check again in 10s...");

			try {
				Thread.sleep(10000); // Wait for 10s
			} catch (Exception e) {

			}
		}

		_logger.info("InfluxDB instance : OK");

		/*
		 * Mosquitto
		 */
		_logger.info("Check for Mosquitto instance availability...");

		checkCounter = 0;
		while (!_mqttService.IsMqttServerAvailable()) {
			checkCounter++;
			_logger.info("Attempt : " + checkCounter + " - Mqtt instance not available...will check again in 10s...");

			try {
				Thread.sleep(10000); // Wait for 10s
			} catch (Exception e) {

			}
		}

		_logger.info("Mosquitto instance : OK");

		/*
		 * Tomcat
		 */
		_logger.info("Check for Tomcat instance availability...");

		checkCounter = 0;
		while (IsTomcatAvailable()) {
			checkCounter++;
			_logger.info("Attempt : " + checkCounter + " - Tomcat instance not available...will check again in 10s...");

			try {
				Thread.sleep(10000); // Wait for 10s
			} catch (Exception e) {

			}
		}
		_logger.info("Tomcat instance : OK");

		dbManager = null;

		_logger.info("System sanity check done...let's roll!");
	}

	private boolean IsTomcatAvailable() {

		try {
			// TODO : change hard coded url
			URL obj = new URL("http://192.168.1.25:8510/automation-webapp/api/firmware/getversion/1");
			HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);

			return (conn.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			_logger.error("Tomcat instance not ready...", e);
			return false;
		}
	}

	// Méthode start de jsvc (Classe Deamon)
	@Override
	public void start() throws Exception {
		_mainThread.start(); // Démarrage du thread principal
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.daemon.Daemon#stop()
	 */
	@Override
	public void stop() throws Exception {

		try {
			_logger.info("Stopping automation server...");

			GpioHelper.shutdown();
			// Stopping all services
			_BLEHubService.StopService();
			_waterMeterService.StopService();
			_loRaService.StopService();
			_alarmService.StopService();
			_smsGammuService.StopService();
			// _scenariiManager.StopScenariiManager();
			_rollerShutterService.StopService();
			_teleInfoService.StopService();
			_roomService.StopService();
			_waterHeater.StopService();
			_mqttService.disconnect();

			SMSDto sms = new SMSDto("773ad55b-1007-4e3e-a325-0f568816ba68");
			sms.setMessage("Automation server has stopped...");
			_smsGammuService.sendMessage(sms);

			// On attends 5 seconds pour l'envoi du sms
			// Thread.sleep(5000);

			_isStopped = true;

			_mainThread.join(60000); // Attend la fin de l'exécution du Thread
										// principal (1 minutes)

			_logger.info("Bye bye automation server stopped...");

		} catch (Exception e) {

			_logger.error("Automation server has not stopped properly", e);
		}
	}

	// Destroy objects
	@Override
	public void destroy() {
		_mainThread = null;
		_loRaService = null;
		_rollerShutterService = null;
		_teleInfoService = null;
		_roomService = null;
		_waterHeater = null;
		_mqttService = null;
		_alarmService = null;
	}

}