package fr.guiet.automationserver.business.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import fr.guiet.automationserver.dto.*;
import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.dataaccess.DbManager;

public class TeleInfoService implements Runnable {

	// Logger
	private static Logger _logger = LogManager.getLogger(TeleInfoService.class);
	private boolean _isStopped = false; // Service arrete?
	private String _defaultDevice = "";
	private static final int VALID_GROUPES_NUMBER = 17;
	
	private Integer _lastHCHC = null;
	private Integer _lastHCHP = null;
	private TeleInfoTrameDto _lastTeleInfoTrameReceived = null;
	
	
	private Timer _timer = null;
	private Timer _timer2 = null;
	private Timer _timer3 = null;
	private Timer _timer4 = null;
	private SMSGammuService _smsGammuService = null;
	private MqttService _mqttService = null;
	private DbManager _dbManager = null;
	private float _hpCost = 0;
	private float _hcCost = 0;
	private float _aboCost = 0;
	private float _ctaCost = 0;
	private float _cspeCost = 0;
	private float _tcfeCost = 0;

	private String _electricityCostPerCurrentDay = "NA";
	private String _electricityCostPerCurrentMonth = "NA";

	private final static long ONCE_PER_DAY = 1000 * 60 * 60 * 24;
	private Serial _serial = null;
	
	private static String MQTT_TOPIC_ELECTRICITY_INFO = "guiet/automationserver/electricity";

	public TeleInfoService(SMSGammuService smsGammuService, MqttService mqttService) {
		_smsGammuService = smsGammuService;
		_dbManager = new DbManager();
		_mqttService = mqttService;
		
		_serial = SerialFactory.createInstance();
		
		ReadConfig();
	}
	
	private void ReadConfig() {
		
		InputStream is = null;
		try {
			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);

			_defaultDevice = prop.getProperty("teleinfo.usbdevice");

			try {
				String hpCost = prop.getProperty("hp.cost");
				if (hpCost != null)
					_hpCost = Float.parseFloat(hpCost);
				else
					_logger.warn("Bad hp.cost not defined in config file !, set to 0 by default");
			} catch (NumberFormatException nfe) {
				_hpCost = Float.parseFloat("0");
				_logger.warn("Bad hp.cost defined in config file !, set to 0 by default", nfe);
			}

			try {
				String aboCost = prop.getProperty("abo.cost");
				if (aboCost != null)
					_aboCost = Float.parseFloat(aboCost);
				else
					_logger.warn("Bad abo.cost not defined in config file !, set to 0 by default");
			} catch (NumberFormatException nfe) {
				_aboCost = Float.parseFloat("0");
				_logger.warn("Bad abo.cost defined in config file !, set to 0 by default", nfe);
			}

			try {
				String ctaCost = prop.getProperty("cta.cost");
				if (ctaCost != null)
					_ctaCost = Float.parseFloat(ctaCost);
				else
					_logger.warn("Bad cta.cost not defined in config file !, set to 0 by default");
			} catch (NumberFormatException nfe) {
				_ctaCost = Float.parseFloat("0");
				_logger.warn("Bad cta.cost defined in config file !, set to 0 by default", nfe);
			}

			try {
				String cspeCost = prop.getProperty("cspe.cost");
				if (cspeCost != null)
					_cspeCost = Float.parseFloat(cspeCost);
				else
					_logger.warn("Bad cspe.cost not defined in config file !, set to 0 by default");
			} catch (NumberFormatException nfe) {
				_cspeCost = Float.parseFloat("0");
				_logger.warn("Bad cspe.cost defined in config file !, set to 0 by default", nfe);
			}

			try {
				String tcfeCost = prop.getProperty("tcfe.cost");
				if (tcfeCost != null)
					_tcfeCost = Float.parseFloat(tcfeCost);
				else
					_logger.warn("Bad tcfe.cost not defined in config file !, set to 0 by default");
			} catch (NumberFormatException nfe) {
				_tcfeCost = Float.parseFloat("0");
				_logger.warn("Bad tcfe.cost defined in config file !, set to 0 by default", nfe);
			}

			try {
				String hcCost = prop.getProperty("hc.cost");
				if (hcCost != null)
					_hcCost = Float.parseFloat(hcCost);
				else
					_logger.warn("Bad hc.cost not defined in config file !, set to 0 by default");
			} catch (NumberFormatException nfe) {
				_hcCost = Float.parseFloat("0");
				_logger.warn("Bad hc.cost defined in config file !, set to 0 by default", nfe);
			}

		} catch (FileNotFoundException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		} catch (IOException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		}
	}

	

	@Override
	public void run() {

		_logger.info("Starting TeleInfoService...");

		ReadConfig();

		// TODO : using String.format C# similar way to log pieces of
		// information
		_logger.info("Using serial device : " + _defaultDevice);

		// CreateSerialInstance();

		//OpenSerialConnection();

		CreatePublishElectricityInfoTask();

		CreateGetElectrictyCostInfoTask();

		// Création de la tâche de sauvegarde en bdd
		CreateSaveToDBTask();

		// Save electrical yesterday cost consumption
		CreateSaveElectricityToDBTask();

		while (!_isStopped) {

			try {

				// Recuperation de la trame de teleinfo
				String trameReceived = ReadTeleInfoTrame();
				// _logger.error("Test TeleInfoService...");
				if (trameReceived != null) {

					// Decodage de la trame
					TeleInfoTrameDto teleInfoTrame = DecodeTrame(trameReceived);

					if (teleInfoTrame != null) {
						// _logger.info("Valorisation trame recu");
						_lastTeleInfoTrameReceived = teleInfoTrame;
					}
				}

				// Necessary otherwise, serial reader stop
				Thread.sleep(2000);

			} catch (Exception e) {
				_logger.error("Error occured in TeleInfo service...", e);

				SMSDto sms = new SMSDto("1f5e1fa1-49c5-4700-b02d-a588d9a8f5b5");
				sms.setMessage("Error occured in TeleInfo services service, review error log for more details");
				_smsGammuService.sendMessage(sms);
			}
		}

	}

	private void CreateGetElectrictyCostInfoTask() {

		TimerTask getElectricyCostInfoTask = new TimerTask() {
			@Override
			public void run() {

				_logger.info("Computing electricity cost per current day and month...");

				_electricityCostPerCurrentMonth = Float.toString(getElectricityCostPerCurrentMonth());
				_electricityCostPerCurrentDay = Float.toString(getElectricityCostPerCurrentDay());

				_logger.info("Eletricity cost computing done !");
			}
		};

		_logger.info("Creating electricity cost info task");

		_timer4 = new Timer(true);
		// Every one hour
		_timer4.scheduleAtFixedRate(getElectricyCostInfoTask, 5000, 60000 * 60);

	}

	private void CreatePublishElectricityInfoTask() {

		TimerTask publishElectricyInfoTask = new TimerTask() {
			@Override
			public void run() {

				String hchc = "NA";
				String hchp = "NA";
				String papp = "NA";

				if (_lastTeleInfoTrameReceived != null) {
					hchc = Integer.toString(_lastTeleInfoTrameReceived.HCHC);
					hchp = Integer.toString(_lastTeleInfoTrameReceived.HCHP);
					papp = Integer.toString(_lastTeleInfoTrameReceived.PAPP);
				}

				JSONObject obj = new JSONObject();

				obj.put("index_hc", hchc);
				obj.put("index_hp", hchp);
				obj.put("conso_inst", papp);
				obj.put("today_cost", _electricityCostPerCurrentDay);
				obj.put("month_cost", _electricityCostPerCurrentMonth);

				_mqttService.SendMsg(MQTT_TOPIC_ELECTRICITY_INFO, obj.toString());

			}
		};

		_logger.info("Creating electricity publish info task");

		_timer3 = new Timer(true);
		// Every 10 s
		_timer3.scheduleAtFixedRate(publishElectricyInfoTask, 5000, 10000);

	}

	// Création de la tache de sauvegarde en bdd
	private void CreateSaveElectricityToDBTask() {

		TimerTask saveElecTask = new TimerTask() {
			@Override
			public void run() {

				Date hier = DateUtils.addDays(new Date(), -1);

				try {

					hier = DateUtils.getDateWithoutTime(hier);
					// hier is today-1 at 00:00

					Date today = DateUtils.getDateWithoutTime(new Date());

					HashMap<String, Float> info = GetElectricityBillInfo(hier, today);

					_dbManager.SaveElectricityCost(hier, Math.round(info.get("hc")), Math.round(info.get("hp")),
							info.get("hc_cost"), info.get("hp_cost"), info.get("other_cost"));
				} catch (Exception e) {
					_logger.error("Could not save electricty cost for day : " + DateUtils.getDateToString(hier), e);
				}

			}
		};

		_logger.info("Creating database saving electricity cost task, first execution will occur at : "
				+ DateUtils.getDateToString(DateUtils.getTomorrowMorning1AM()));

		_timer2 = new Timer(true);
		// Toutes les minutes on enregistre une trame
		_timer2.scheduleAtFixedRate(saveElecTask, DateUtils.getTomorrowMorning1AM(), ONCE_PER_DAY);

	}

	// Création de la tache de sauvegarde en bdd
	private void CreateSaveToDBTask() {

		TimerTask teleInfoTask = new TimerTask() {
			@Override
			public void run() {

				try {

					if (_lastTeleInfoTrameReceived != null) {
						// Sauvegarde en bdd
						SaveTrameToDb(_lastTeleInfoTrameReceived);
					}
				} catch (Exception e) {
					_logger.error("Error occured in save to db teleinfo task", e);
				}
			}
		};

		_timer = new Timer(true);
		// Toutes les minutes on enregistre une trame
		_timer.schedule(teleInfoTask, 5000, 60000);

	}

	public boolean isOperational() {
		return (null != _lastTeleInfoTrameReceived);
	}

	// Arret du service TeleInfoService
	public void StopService() {

		if (_timer != null)
			_timer.cancel();

		if (_timer2 != null) {
			_timer2.cancel();
		}

		if (_timer3 != null) {
			_timer3.cancel();
		}

		if (_timer4 != null) {
			_timer4.cancel();
		}

		CloseSerialConnection(true);

		_logger.info("Stopping TeleInfo service...");

		_isStopped = true;
	}

	// Récupération de la dernière trame teleinfo recue
	public TeleInfoTrameDto GetLastTrame() {
		return _lastTeleInfoTrameReceived;
	}

	// Retourne null si la dernière trame recu vaut null
	public Boolean IsHeureCreuse() {

		if (_lastTeleInfoTrameReceived != null && _lastTeleInfoTrameReceived.PTEC != null) {
			return (_lastTeleInfoTrameReceived.PTEC.equals("HC.."));
		}

		return null;
	}
	
	//Dunno why but it may occur...
	private boolean TeleInfoTrameSanityCheck(TeleInfoTrameDto teleInfoTrame) {
		
		boolean isValid = true;
		
		if (_lastHCHC == null) {
			_lastHCHC = teleInfoTrame.HCHC; 
		}
		
		if (_lastHCHP == null) {
			_lastHCHP = teleInfoTrame.HCHP; 
		}
		
		if (teleInfoTrame.HCHC < _lastHCHC) {									
			isValid = false;
			_logger.warn("La valeur HCHC actuelle ("+ teleInfoTrame.HCHC +") est inférieure à la valeur de la dernière trame ("+ _lastHCHC  +"), c'est impossible");
		} else {
			if (teleInfoTrame.HCHC - _lastHCHC > 1000) {
				isValid = false;
				String mess = "La valeur HCHC actuelle ("+ teleInfoTrame.HCHC +") est supérieure à la valeur de la dernière trame ("+ _lastHCHC  +") de 1000, c'est impossible"; 
				
				_logger.warn(mess);
				
				SMSDto sms = new SMSDto("79447dfa-34d9-43d6-81a5-5f6db41cfebb");
				sms.setMessage(mess);
				_smsGammuService.sendMessage(sms);
			}
		}
		
		
		if (teleInfoTrame.HCHP < _lastHCHP) {
			isValid = false;
			_logger.warn("La valeur HCHP actuelle ("+ teleInfoTrame.HCHP +") est inférieure à la valeur de la dernière trame ("+ _lastHCHP  +"), c'est impossible");
		}
		else {
			if (teleInfoTrame.HCHP - _lastHCHP > 1000) {
				isValid = false;
				String mess = "La valeur HCHP actuelle ("+ teleInfoTrame.HCHP +") est supérieure à la valeur de la dernière trame ("+ _lastHCHP  +") de 1000, c'est impossible";
				
				_logger.warn(mess);
				
				SMSDto sms = new SMSDto("b63e8c33-a394-4789-a68f-c4de517b57b2");
				sms.setMessage(mess);
				_smsGammuService.sendMessage(sms);
			}
		}
		
		return isValid;
	}
	
	// Sauvegarde de la trame de teleinfo recue en bdd
	private void SaveTrameToDb(TeleInfoTrameDto teleInfoTrame) {

		boolean isValid = TeleInfoTrameSanityCheck(teleInfoTrame);
		
		if (isValid) {		
			_dbManager.SaveTeleInfoTrameToInfluxDb(teleInfoTrame);
			
			_lastHCHC = teleInfoTrame.HCHC; 
			_lastHCHP = teleInfoTrame.HCHP;
		}
		
		
	}

	private void CloseSerialConnection(boolean killSerialFactory) {
		
		try {
			if (_serial.isOpen()) {
				_logger.info("Fermeture port serie pour la TeleInfo");
				
				_serial.discardInput();
				_serial.close();		
			}
			
			if (killSerialFactory)
				SerialFactory.shutdown();
			
		} catch (IOException ioe) {
			
			_logger.error("Impossible de fermer le port serie pour la TeleInfo", ioe);
		}
	}

	// Opening Serial Connection
	private boolean OpenSerialConnection() {

		boolean isOk = true;

		try {
			
			if (_serial.isClosed()) {

				//_serial = SerialFactory.createInstance();
	
				// open the default serial port provided on the GPIO header at 1200
				// bauds
				// serial.open(_defaultDevice, _defaultBaud);
				SerialConfig config = new SerialConfig();
				config.device(_defaultDevice).baud(Baud._1200).dataBits(DataBits._7).parity(Parity.EVEN)
						.stopBits(StopBits._1).flowControl(FlowControl.NONE);
	
				_serial.setBufferingDataReceived(true);
	
				_serial.open(config);
				_logger.info("Ouverture du port serie pour la TeleInfo effectué avec succès...");
				
			}

		} catch (IOException e) {
			
			isOk = false;
			
			_logger.error("Impossible d'ouvrir le port série pour la TeleInfo", e);
		}

		return isOk;
	}

	// Read Serial Port
	private String ReadTeleInfoTrame() {

		// Initialise variables
		ArrayList<Character> trame = new ArrayList<Character>();
		boolean beginTrameDetected = false;
		boolean endTrameDetected = false;
		boolean trameFullyReceived = false;
		boolean checkFirstChar = false;
		boolean timeoutOccured = false;
		// Timeout !
		Date startTime = new Date();

		try {
			
			//Check serial connection
			if (!OpenSerialConnection()) return null;

			// Discards any data in both the serial receive and transmit buffers.
			_serial.discardAll();

			while (!trameFullyReceived && !timeoutOccured) {

				// Trame complete recue?
				if (beginTrameDetected && endTrameDetected) {
					trameFullyReceived = true;
					continue;
				}

				// Timeout occured?
				Date currentDate = new Date();

				Long elapsedTime = DateUtils.secondsBetweenDate(startTime, currentDate);
				
				//_logger.warn("Elapsed Time : " + elapsedTime);
				
				//1 minutes
				if (elapsedTime > 60) {
					timeoutOccured = true;
				}

				// Data available in buffer ?
				if (_serial.available() > 0) {

					byte[] byteArray = _serial.read();

					String serialData = new String(byteArray, "UTF-8");
					char[] data = serialData.toCharArray();

					for (int i = 0; i < data.length; i++) {

						char receivedChar = data[i];
						receivedChar &= 0x7F;

						//_logger.warn("carac recu: "+(int)receivedChar);

						// System.out.println("int char : "+(int)receivedChar);
						 //String decoded = String.valueOf(receivedChar);
						 //_logger.warn("carac recu: "+decoded);
						// System.out.println(decoded);

						// Reception indicateur debut trame
						if (receivedChar == 0x02) {
							beginTrameDetected = true;
							checkFirstChar = true;

							// On continue, on ne veut pas enregistrer ce caracteres
							continue;
						}

						// Reception indicateur fin trame
						// avant on doit avoir recu l'indicateur de debut de trame
						// et le premier caractere de la trame doit avoir ete
						// verifie
						if (receivedChar == 0x03 && beginTrameDetected && !checkFirstChar) {
							endTrameDetected = true;
						}

						// Si le debut de la trame a ete detecte, on enregistre...on
						// ne
						// veut pas le caractere de fin evidement ainsi que les
						// caracteres qui ont arrive
						// apres avec le buffer
						if (beginTrameDetected && !endTrameDetected) {

							// Verification du premier carac apres reception du
							// carac de debut
							// pour etre sur du debut de trame...
							if (checkFirstChar) {
								checkFirstChar = false;
								if (receivedChar != 0x0A) { // Line Feed
									beginTrameDetected = false;
									continue;
									// System.out.println("different de 0x0D");
									// System.out.println("int char : "+
									// Character.getNumericValue(receivedChar));
								}
							}

							// System.out.println("int char : "+(int)receivedChar);
							trame.add(receivedChar);
						}
					}
				}
				else {
					_logger.warn("Serial buffer empty...waiting timeout to restart serial connection");
				}

				// Wait a little
				Thread.sleep(500);
			}

			if (trameFullyReceived) {
				String trameString = TeleInfoService.ArrayListToStringHelper(trame);
				//_logger.info("Trame lue : " + trameString);

				return trameString;
			}

			if (timeoutOccured) {
				String mess = "Aucune trame de téléinfo recue dans la minute qui vient de s'écouler, tentative de relance d'une instance sur le port série";

				_logger.info(mess);

				SMSDto sms = new SMSDto("954e5116-ceed-43c5-951b-ebb5d629bc74");
				sms.setMessage(mess);
				_smsGammuService.sendMessage(sms);
				
				// Reinit last received trame...
				_lastTeleInfoTrameReceived = null;
				
				CloseSerialConnection(false);
				
				return null;
			}

			// May not arrived here...
			_logger.info("Problem this code should not be executed...");
			return null;
		} catch (Exception e) {
			_logger.info("Erreur while reading TeleInfoTrame", e);

			CloseSerialConnection(false);
			
			return null;
		}
	}


	private float getElectricityCostPerCurrentMonth() {

		float hc_cost = 0;
		float hp_cost = 0;
		float other_cost = 0;

		_logger.info("Computing month's electricity cost");

		try {

			// Date today = DateUtils.getDateWithoutTime(new Date());

			Date firstDateOfMonth = DateUtils.getFirstDateOfCurrentMonth();

			HashMap<String, Float> info = GetElectricityBillInfo(firstDateOfMonth, new Date());

			hc_cost = info.get("hc_cost");
			hp_cost = info.get("hp_cost");
			other_cost = info.get("other_cost");

		} catch (Exception e) {
			_logger.error(
					"Error occured while getting electricity info...cannot compute month's electricity cost, returning 0 euros",
					e);
		}

		return hc_cost + hp_cost + other_cost;
	}

	public float getElectricityCostPerCurrentDay() {

		float hc_cost = 0;
		float hp_cost = 0;
		float other_cost = 0;

		_logger.info("Computing today's electricity cost");

		try {

			Date today = DateUtils.getDateWithoutTime(new Date());

			HashMap<String, Float> info = GetElectricityBillInfo(today, new Date());

			hc_cost = info.get("hc_cost");
			hp_cost = info.get("hp_cost");
			other_cost = info.get("other_cost");

		} catch (Exception e) {
			_logger.error(
					"Error occured while getting electricity info...cannot compute today's electricity cost, returning 0 euros",
					e);
		}

		return hc_cost + hp_cost + other_cost;
	}

	/*
	 * public float GetNextElectricityBillCost() {
	 * 
	 * float hc_cost = 0; float hp_cost = 0; float other_cost = 0;
	 * 
	 * _logger.info("Computing next electricity bill cost");
	 * 
	 * try { // HashMap<String, Float> info = GetElectricityBillInfo(_lastBillDate,
	 * // DateUtils.addDays(_lastBillDate, 59));
	 * 
	 * //yyyy-MM-dd Date beginDate = DateUtils.parseDate("2018-12-21"); Date endDate
	 * = DateUtils.parseDate("2019-01-12");
	 * 
	 * HashMap<String, Float> info = GetElectricityBillInfo(beginDate, endDate);
	 * 
	 * hc_cost = info.get("hc_cost"); hp_cost = info.get("hp_cost"); other_cost =
	 * info.get("other_cost");
	 * 
	 * } catch (Exception e) { _logger.error(
	 * "Error occured while getting electricity bill info...cannot compute next electricity bill cost, returning 0 euros"
	 * , e); }
	 * 
	 * return hc_cost + hp_cost + other_cost;
	 * 
	 * }
	 */

	// public

	public HashMap<String, Float> GetElectricityBillInfo(Date fromDate, Date toDate) {

		float hcConsoTTC = 0;
		float hpConsoTTC = 0;
		float aboCostTTC = 0;
		float ctaTTC = 0;
		float cspeTTC = 0;
		float tcfeTTC = 0;

		HashMap<String, Float> returns = new HashMap<String, Float>();

		try {
			// DbManager dbManager = new DbManager();

			// Date currentDate = new Date();
			int days = 0;
			try {
				days = DateUtils.betweenDates(fromDate, toDate).intValue();
			} catch (IOException e) {
				_logger.error("Erreur lors du calcul du nombre de jours entre deux dates", e);
			}

			HashMap<String, Integer> map = _dbManager.GetElectriciyConsumption(fromDate, toDate);

			Integer hcConso = map.get("hcConsuption");
			Integer hpConso = map.get("hpConsuption");

			returns.put("hc", (float) hcConso);
			returns.put("hp", (float) hpConso);

			/*
			 * hcConso = 2252; hpConso = 2515; _hcCost = (float) 0.056; _hpCost = (float)
			 * 0.075; _aboCost = (float) 13.64; System.out.println(hcConso+hpConso);
			 */

			// Montant HT
			float hcConsoHT = hcConso * _hcCost;
			float hpConsoHT = hpConso * _hpCost;

			// MontantTCC
			hcConsoTTC = (float) (hcConsoHT * 1.2);
			hpConsoTTC = (float) (hpConsoHT * 1.2);

			returns.put("hc_cost", (float) hcConsoTTC);
			returns.put("hp_cost", (float) hpConsoTTC);

			// Abo elec 13,64/mois, env 30jours
			float aboCostHT = (_aboCost * days) / 30;
			aboCostTTC = (float) (aboCostHT * 1.055);

			// CTA
			float ctaHT = (_ctaCost * days) / 30;
			ctaTTC = (float) (ctaHT * 1.055);

			// CSPE
			// TODO : a mettre dans le fichier de conf.
			float cspeHT = (float) ((hpConso + hcConso) * _cspeCost);
			cspeTTC = (float) (cspeHT * 1.2);

			float tcfeHT = (float) (_tcfeCost * (hpConso + hcConso));
			tcfeTTC = (float) (tcfeHT * 1.2);

			returns.put("other_cost", aboCostTTC + ctaTTC + cspeTTC + tcfeTTC);

		} catch (Exception e) {
			_logger.error("Error occured when computing next electricity bill", e);
		}

		return returns;
	}

	// Méthode de conversion
	private static String ArrayListToStringHelper(ArrayList<Character> charList) {
		StringBuilder result = new StringBuilder(charList.size());
		for (Character c : charList) {
			result.append(c);
		}

		String output = result.toString();

		return output;
	}

	// Décodage de la trame recue
	private TeleInfoTrameDto DecodeTrame(String trame) {

		// boolean invalidChecksum = false;

		// \r : CR
		// \n : LF

		// String trameInit = trame;

		// Remplacement des lignes feed par des ""
		trame = trame.replaceAll("\\n", "");

		// groupe commence par un line feed \n et termine par un carriage return
		// \r
		String[] groupes = trame.split("\\r");

		if (groupes.length != VALID_GROUPES_NUMBER) {
			//_logger.info("Trame invalide (nombre de groupe incorrect) : DEBUT_TRAME@@@" + trame + "@@@FIN_TRAME");
			return null;
		}

		TeleInfoTrameDto teleInfoTrame = new TeleInfoTrameDto();

		for (String g : groupes) {

			char[] gChar = new char[g.length()];
			gChar = g.toCharArray();

			String etiquette = "";
			String valeur = "";
			String checksum = "";
			boolean readEtiquette = true;
			boolean readValeur = false;
			boolean readChecksum = false;

			for (int i = 0; i < gChar.length; i++) {
				char charGroupe = gChar[i];

				if (readEtiquette) {
					if (charGroupe != 0x20) {
						etiquette += charGroupe;
					} else {
						readEtiquette = false;
						readValeur = true;
						continue;
					}
				}

				if (readValeur) {
					if (charGroupe != 0x20) {
						valeur += charGroupe;
					} else {
						readValeur = false;
						readChecksum = true;
						continue;
					}
				}

				if (readChecksum) {
					checksum += charGroupe;
				}
			}

			// System.out.println("etiquette : "+etiquette);
			// System.out.println("valeur : "+valeur);
			// System.out.println("checksum : "+checksum);
			if (Checksum(etiquette, valeur, checksum)) {

				switch (etiquette) {
				case "ADCO":
					teleInfoTrame.ADCO = valeur;
					break;
				case "OPTARIF":
					teleInfoTrame.OPTARIF = valeur;
					break;
				case "ISOUSC":
					teleInfoTrame.ISOUSC = Short.parseShort(valeur);
					break;
				case "HCHC":
					teleInfoTrame.HCHC = Integer.parseInt(valeur);
					break;
				case "HCHP":
					teleInfoTrame.HCHP = Integer.parseInt(valeur);
					break;
				case "PTEC":
					teleInfoTrame.PTEC = valeur;
					break;
				case "IINST1":
					teleInfoTrame.IINST1 = Short.parseShort(valeur);
					break;
				case "IINST2":
					teleInfoTrame.IINST2 = Short.parseShort(valeur);
					break;
				case "IINST3":
					teleInfoTrame.IINST3 = Short.parseShort(valeur);
					break;
				case "IMAX1":
					teleInfoTrame.IMAX1 = Short.parseShort(valeur);
					break;
				case "IMAX2":
					teleInfoTrame.IMAX2 = Short.parseShort(valeur);
					break;
				case "IMAX3":
					teleInfoTrame.IMAX3 = Short.parseShort(valeur);
					break;
				case "PMAX":
					teleInfoTrame.PMAX = Integer.parseInt(valeur);
					break;
				case "PAPP":
					teleInfoTrame.PAPP = Integer.parseInt(valeur);
					break;
				case "HHPHC":
					teleInfoTrame.HHPHC = valeur;
					break;
				case "MOTDETAT":
					teleInfoTrame.MOTDETAT = valeur;
					break;
				case "PPOT":
					teleInfoTrame.PPOT = valeur;
					break;
				default:
					// No groupe found! - Bad Trame!!! here
					//_logger.info("Etiquette : " + etiquette + " inconnue. Trame invalide");
					return null;
				}
			} else {
				//_logger.info("Checksum invalide pour l'etiquette : " + etiquette + ", valeur : " + valeur);
				// Bad checksum ! process ends here!
				return null;

			}
		} // End of for (String g : groupes)

		/*
		 * if (invalidChecksum) return null; else
		 */
		
		_logger.info("Trame TeleInfo valide réceptionnée");
		
		return teleInfoTrame;
	}

	// Vérification de la trame recue
	private boolean Checksum(String etiquette, String valeur, String checksum) {

		// Sanity check method parameters!
		if ("".equals(checksum)) // || "".equals(etiquette) || "".equals(valeur))
			return false;

		int sum = 32; // Somme des codes ASCII du message + un espace
		int i;

		char[] etiquetteChar = new char[etiquette.length()];
		char[] valeurChar = new char[valeur.length()];
		char checksumChar = checksum.charAt(0);

		etiquetteChar = etiquette.toCharArray();
		valeurChar = valeur.toCharArray();

		for (i = 0; i < etiquetteChar.length; i++) {
			sum = sum + etiquetteChar[i];
			// _logger.info("char eti : "+(int)etiquetteChar[i]);
		}

		for (i = 0; i < valeurChar.length; i++) {
			sum = sum + valeurChar[i];
			// _logger.info("char val : "+(int)valeurChar[i]);
		}
		sum = (sum & 63) + 32;

		// System.out.println("sum : "+sum);
		// System.out.println("Cheksum : "+(int)checksumChar);

		/*
		 * _logger.info("etiquette : "+etiquette); _logger.info("valeur : "+valeur);
		 */
		// _logger.info("sum : "+sum);
		// _logger.info("checksum : "+(int)checksumChar);

		if (sum == checksumChar)
			return true;

		return false;
	}
}
