package fr.guiet.automationserver;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

import org.apache.log4j.Logger;
import fr.guiet.automationserver.business.helper.GpioHelper;
import fr.guiet.automationserver.business.helper.MqttClientHelper;
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
	private AlarmService _alarmService = null; //Alarm service
	//private ScenariiManager _scenariiManager = null;
	private TeleInfoService _teleInfoService = null; // service de teleinfo
	private RainGaugeService _rainGaugeService = null; //service raingauge
	private RoomService _roomService = null; // service de room service
	private WaterHeaterService _waterHeater = null; // service de gestion du
												// chauffe-eau
	private RollerShutterService _rollerShutterService = null;
	private BLEHubService _BLEHubService = null;  
	private boolean _isStopped = false;
	private Thread _roomServiceThread = null;
	private Thread _teleInfoServiceThread = null;
	private Thread _rainGaugeServiceThread = null;
	private Thread _waterHeaterServiceThread = null;
	private Thread _rollerShutterServiceThread = null;
	private Thread _BLEHubServiceThread = null;
	private Print3DService _print3DService = null;
	private OutsideEnvironmentalService _outsideEnvService = null;
	private SMSGammuService _smsGammuService = null;
	private MqttService _mqttHelper = null;
	
	// Logger
	private static Logger _logger = Logger.getLogger(AutomationServer.class);	
	
	// Init deamon
	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
		/*
		 * Construct objects and initialize variables here. You can access the
		 * command line arguments that would normally be passed to your main()
		 * method as follows:
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
					
					//Starts system sanity checks
					DoSystemSanityChecks();					
					
					// Set local to en_GB
					Locale.setDefault(new Locale("en", "GB"));
					
					//SMS Service
					_smsGammuService = new SMSGammuService();
					
					// Starting rain gauge service
					_rainGaugeService = new RainGaugeService(_smsGammuService);
					_rainGaugeServiceThread = new Thread(_rainGaugeService);
					_rainGaugeServiceThread.start();

					// Starting teleinfo service
					_teleInfoService = new TeleInfoService(_smsGammuService);
					_teleInfoServiceThread = new Thread(_teleInfoService);
					_teleInfoServiceThread.start();	
					
					// Starting water heater
					_waterHeater = new WaterHeaterService(_teleInfoService, _smsGammuService);
					_waterHeaterServiceThread = new Thread(_waterHeater);
					_waterHeaterServiceThread.start();								
					
					//Start Rollershutter service
					_rollerShutterService = new RollerShutterService(_smsGammuService);
					_rollerShutterServiceThread = new Thread(_rollerShutterService);
					_rollerShutterServiceThread.start();
					
					//Start BLE Hub Service
					_BLEHubService = new BLEHubService(_smsGammuService);
					_BLEHubServiceThread = new Thread(_BLEHubService);
					_BLEHubServiceThread.start();
					
					//Start alarm service
					_alarmService = new AlarmService(_rollerShutterService, _smsGammuService);
					
					//Start scenarii service
					//_scenariiManager = new ScenariiManager(_rollerShutterService, _alarmService);
					
					//Start 3D Print service
					_print3DService = new Print3DService(_smsGammuService);
					
					//Start Outside Environmental service
					_outsideEnvService = new OutsideEnvironmentalService("Outside Environmental",_smsGammuService);
					
					// Starting room service
					_roomService = new RoomService(_teleInfoService, _smsGammuService);
					_roomServiceThread = new Thread(_roomService);
					_roomServiceThread.start();
					
					while(!_roomService.isRoomListLoaded()) {
						_logger.info("Waiting for room list to be fully loaded !");
						Thread.sleep(1000);
					}
					
					_mqttHelper = new MqttService(_smsGammuService, _roomService, _teleInfoService, _waterHeater, 
							_alarmService, _rollerShutterService);
					_mqttHelper.addClient(_BLEHubService);
					_mqttHelper.addClient(_print3DService);
					_mqttHelper.addClients(_roomService.getMqttableClients());
					_mqttHelper.addClients(_outsideEnvService.getMqttableClients());
					
					_mqttHelper.connectAndSubscribe();

					SMSDto sms = new SMSDto("ba92bca9-a67f-4945-a6ee-51bf44bfaed5");
					sms.setMessage("Automation server has started...");
					_smsGammuService.sendMessage(sms, true);
					
					while (!_isStopped) {

						//Publication des données toutes les 10s
						Thread.sleep(10000);
						
						//TODO : please remove that!!!
						_mqttHelper.PublishInfoToMqttBroker();
												
					}					
				} catch (Exception e) {
					_logger.error("Error occured in automation server...", e);
					
					SMSDto sms = new SMSDto("6b81daa5-eca5-49c4-bc58-53fa9cd192a6");
					sms.setMessage("Error occured in main loop !");
					_smsGammuService.sendMessage(sms, true);

				}
			}
		};
	}
	
	private void DoSystemSanityChecks() {
		
		DbManager dbManager = new DbManager();
		MqttClientHelper mqttClient = new MqttClientHelper("SanityCheck");
		
		_logger.info("Starting system sanity check...");
		
		/*
		 * PostgreSQL
		 */
		_logger.info("Check for PostgreSQL Server availability...");
		
		int checkCounter = 0;
		while(!dbManager.IsPostgreSQLAvailable()) {
			checkCounter++;
			_logger.info("Attempt : "+checkCounter+" - PostgreSQL not available...will check again in 10s...");
			
			try {
				Thread.sleep(10000); //Wait for 10s
			}
			catch(Exception e) {
				
			}
		}
		
		_logger.info("PostgreSQL instance : OK");
		
		/*
		 * InfluxDB
		 */
		_logger.info("Check for InfluxDB Server availability...");
		
		checkCounter = 0;
		while(!dbManager.IsInfluxDbAvailable()) {
			checkCounter++;
			_logger.info("Attempt : "+checkCounter+" - InfluxDB not available...will check again in 10s...");
			
			try {
				Thread.sleep(10000); //Wait for 10s
			}
			catch(Exception e) {
				
			}
		}
		
		_logger.info("InfluxDB instance : OK");
		
		/*
		 * Mosquitto
		 */
		_logger.info("Check for Mosquitto instance availability...");
				
		checkCounter = 0;
		while(!mqttClient.IsMqttServerAvailable()) {
			checkCounter++;
			_logger.info("Attempt : "+checkCounter+" - Mqtt instance not available...will check again in 10s...");
			
			try {
				Thread.sleep(10000); //Wait for 10s
			}
			catch(Exception e) {
				
			}
		}
		
		_logger.info("Mosquitto instance : OK");
		
		/*
		 * Tomcat
		 */
		_logger.info("Check for Tomcat instance availability...");
		
		checkCounter = 0;
		while(IsTomcatAvailable()) {
			checkCounter++;
			_logger.info("Attempt : "+checkCounter+" - Tomcat instance not available...will check again in 10s...");
			
			try {
				Thread.sleep(10000); //Wait for 10s
			}
			catch(Exception e) {
				
			}
		}
		_logger.info("Tomcat instance : OK");
		
		
		dbManager = null;		
		mqttClient = null;
		
		_logger.info("System sanity check done...let's roll!");
	}
	
	private boolean IsTomcatAvailable() {
		
		try {
			//TODO : change hard coded url
			URL obj = new URL("http://192.168.1.25:8510/automation-webapp/api/firmware/getversion/1");
			HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			
			return (conn.getResponseCode() == HttpURLConnection.HTTP_OK);
		}		
		catch(Exception e) {
			_logger.error("Tomcat instance not ready...", e);
			return false;
		}
	}
	
	// Méthode start de jsvc (Classe Deamon)
	@Override
	public void start() throws Exception {
		_mainThread.start(); // Démarrage du thread principal
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.daemon.Daemon#stop()
	 */
	@Override
	public void stop() throws Exception {

		try {
			_logger.info("Stopping automation server...");
			
			GpioHelper.shutdown();
			// Stopping all services
			_BLEHubService.StopService();
			_rainGaugeService.StopService();
			_alarmService.StopService();
			//_scenariiManager.StopScenariiManager();
			_rollerShutterService.StopService();
			_teleInfoService.StopService();
			_roomService.StopService();
			_waterHeater.StopService();
			_mqttHelper.disconnect();			
			
			SMSDto sms = new SMSDto("773ad55b-1007-4e3e-a325-0f568816ba68");
			sms.setMessage("Automation server has stopped...");
			_smsGammuService.sendMessage(sms, true);
			
			//On attends 5 seconds pour l'envoi du sms
			//Thread.sleep(5000);
			
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
		_rainGaugeService = null;
		_rollerShutterService = null;
		_teleInfoService = null;
		_roomService = null;
		_waterHeater = null;
		_mqttHelper = null;
		_alarmService = null;		
	}

}