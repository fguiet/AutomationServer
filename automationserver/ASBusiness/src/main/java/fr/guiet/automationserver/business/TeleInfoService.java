package fr.guiet.automationserver.business;

import org.apache.log4j.Logger;

//import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
//import com.pi4j.io.serial.StopBits;
//import com.pi4j.io.serial.Baud;
//import com.pi4j.io.serial.DataBits;
//import com.pi4j.io.serial.FlowControl;
//import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialDataEventListener;
//import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialDataEvent;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import fr.guiet.automationserver.dto.*;
import fr.guiet.automationserver.dataaccess.DbManager;

public class TeleInfoService implements Runnable {

	// Logger
	private static Logger _logger = Logger.getLogger(TeleInfoService.class);
	private boolean _isStopped = false; // Service arrete?
	// create an instance of the serial communications class
	// final Serial _serial = SerialFactory.createInstance();
	// serial data listener
	//private SerialDataEventListener _sdl = null;
	//private SerialDataListener _sdl = null;
	private String _defaultDevice = "";
	private static final int VALID_GROUPES_NUMBER = 17;
	private boolean _beginTrameDetected = false;
	private boolean _endTrameDetected = false;
	private boolean _trameFullyReceived = false;
	private boolean _checkFirstChar = false;
	private TeleInfoTrameDto _lastTeleInfoTrameReceived = null;
	private ArrayList<Character> _trame = null;
	private Timer _timer = null;
	private Timer _timer2 = null;
	private SMSGammuService _smsGammuService = null;
	private DbManager _dbManager = null;
	private float _hpCost = 0;
	private float _hcCost = 0;
	private float _aboCost = 0;
	private float _ctaCost = 0;
	private float _cspeCost = 0;
	private float _tcfeCost = 0;
	private Date _lastBillDate;
	private final static long ONCE_PER_DAY = 1000 * 60 * 60 * 24;

	public TeleInfoService(SMSGammuService smsGammuService) {
		_smsGammuService = smsGammuService;
		_dbManager = new DbManager();
	}

	public Date getLastBillDate() {
		return _lastBillDate;
	}

	@Override
	public void run() {

		_logger.info("Démarrage du service TeleInfoService...");

		InputStream is = null;
		try {
			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);

			_defaultDevice = prop.getProperty("teleinfo.usbdevice");

			String lastBillDate = prop.getProperty("lastbill.date");
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			try {
				_lastBillDate = formatter.parse(lastBillDate);
			} catch (ParseException e) {
				Calendar cal = Calendar.getInstance();
				_lastBillDate = cal.getTime();
				_logger.warn("Bad lastbill.date defined in config file !, set to actual date by default", e);
			}

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

		// TODO : using String.format C# similar way to log pieces of
		// information
		_logger.info("Using serial device : " + _defaultDevice);

		//CreateSerialListener();

		// Création de la tâche de sauvegarde en bdd
		CreateSaveToDBTask();

		// Save electrical yesterday cost consumption
		CreateSaveElectricityToDBTask();

		while (!_isStopped) {

			try {

				// if (_startStopCounter > 0 && SerialFactory.isShutdown()) {
				/*
				 * if (_startStopCounter > 0) { NotifyCollectInfoStop();
				 * _logger.
				 * info("ok teleinfoservice stoppé!! (start_stop_counter :)" +
				 * _startStopCounter); Thread.sleep(2000); continue; }
				 */

				// Recuperation de la trame de teleinfo
				String trameReceived = GetTeleInfoTrame();
				// _logger.error("Test TeleInfoService...");
				if (trameReceived != null) {

					// Decodage de la trame
					TeleInfoTrameDto teleInfoTrame = DecodeTrame(trameReceived);

					if (teleInfoTrame != null) {
						// _logger.info("Valorisation trame recu");
						_lastTeleInfoTrameReceived = teleInfoTrame;
					}
				}

				// Necessary otherwire, serial reader stop
				Thread.sleep(2000);

			} catch (Exception e) {
				_logger.error("Error occured in TeleInfo service...", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in TeleInfo services service, review error log for more details");
				_smsGammuService.sendMessage(sms, true);
			}
		}

	}

	private static Date getTomorrowMorning1AM() {

		Calendar c1 = Calendar.getInstance();

		c1.add(GregorianCalendar.DAY_OF_MONTH, 1);
		c1.set(Calendar.HOUR_OF_DAY, 2);
		c1.set(Calendar.MINUTE, 0);
		c1.set(Calendar.SECOND, 0);		

		return c1.getTime();
	}

	// Création de la tache de sauvegarde en bdd
	private void CreateSaveElectricityToDBTask() {

		TimerTask saveElecTask = new TimerTask() {
			@Override
			public void run() {

				Date hier = DateUtils.addDays(new Date(), -1);
				hier = DateUtils.getDateWithoutTime(hier);
				// hier is today-1 at 00:00

				Date today = DateUtils.getDateWithoutTime(new Date());

				try {
					HashMap<String, Float> info = GetElectricityBillInfo(hier, today);

					_dbManager.SaveElectricityCost(hier, Math.round(info.get("hc")), Math.round(info.get("hp")),
							info.get("hc_cost"), info.get("hp_cost"), info.get("other_cost"));
				} catch (Exception e) {
					_logger.error("Could not save electricty cost for day : " + DateUtils.getDateToString(hier), e);
				}

			}
		};

		_logger.info("Creating database saving electricity cost task, first execution will occur at : "
				+ DateUtils.getDateToString(getTomorrowMorning1AM()));

		_timer2 = new Timer(true);
		// Toutes les minutes on enregistre une trame
		_timer2.scheduleAtFixedRate(saveElecTask, getTomorrowMorning1AM(), ONCE_PER_DAY);

	}

	// Création de la tache de sauvegarde en bdd
	private void CreateSaveToDBTask() {

		TimerTask teleInfoTask = new TimerTask() {
			@Override
			public void run() {
				if (_lastTeleInfoTrameReceived != null) {
					// Sauvegarde en bdd
					SaveTrameToDb(_lastTeleInfoTrameReceived);
				}
			}
		};

		_timer = new Timer(true);
		// Toutes les minutes on enregistre une trame
		_timer.schedule(teleInfoTask, 5000, 60000);

	}

	// Arret du service TeleInfoService
	public void StopService() {

		if (_timer != null)
			_timer.cancel();

		if (_timer2 != null) {
			_timer2.cancel();
		}

		_logger.info("Stopping TeleInfo service...");

		_isStopped = true;
	}

	// Récupération de la dernière trame teleinfo recue
	public TeleInfoTrameDto GetLastTrame() {
		return _lastTeleInfoTrameReceived;
	}

	// Retourne null si la dernière trame recu vaut null
	public Boolean IsHeureCreuse() {

		if (_lastTeleInfoTrameReceived != null) {
			return (_lastTeleInfoTrameReceived.PTEC.equals("HC.."));
		}

		return null;
	}

	// Sauvegarde de la trame de teleinfo recue en bdd
	private void SaveTrameToDb(TeleInfoTrameDto teleInfoTrame) {

		_dbManager.SaveTeleInfoTrame(teleInfoTrame);
		// _logger.info("Sauvegarde de la trame teleinfo en base de données");

		// if (System.getProperty("SaveToInfluxDB").equals("TRUE")) {
		_dbManager.SaveTeleInfoTrameToInfluxDb(teleInfoTrame);
		// _logger.info("Sauvegarde de la trame teleinfo dans InfluxDB");
		// }
	}

	private String convert(byte[] data) {
	    StringBuilder sb = new StringBuilder(data.length);
	    for (int i = 0; i < data.length; ++ i) {
        	//if (data[i] < 0) throw new IllegalArgumentException();
	        sb.append((char) data[i]);
	    }
	    return sb.toString();
	}

	// Creation du listener sur le port serie
	private SerialDataEventListener CreateSerialListener() {

		return new SerialDataEventListener() {
		 //_sdl = new SerialDataEventListener() {
		//_sdl = new SerialDataListener() {
			@Override
			public void dataReceived(SerialDataEvent event) {

				if (_trameFullyReceived)
					return;

				String dataSZ = "";
				 try {
					dataSZ = convert(event.getBytes());
				//event.
				//dataSZ = event.getData();
				 } catch (IOException e) {
				 _logger.error("Unable de read serial port", e);
				}

				char[] data = dataSZ.toCharArray();

				for (int i = 0; i < data.length; i++) {
					char receivedChar = data[i];
					receivedChar &= 0x7F;

					//_logger.warn("carac recu: "+(int)receivedChar);

					// System.out.println("int char : "+(int)receivedChar);
					// String decoded = String.valueOf(receivedChar);
					// _logger.warn("carac recu: "+decoded);
					// System.out.println(decoded);

					// Reception indicateur debut trame
					if (receivedChar == 0x02) {
						_beginTrameDetected = true;
						_checkFirstChar = true;

						// On continue, on ne veut pas enregistrer ce caracteres
						continue;
					}

					// Reception indicateur fin trame
					// avant on doit avoir recu l'indicateur de debut de trame
					// et le premier caractere de la trame doit avoir ete
					// verifie
					if (receivedChar == 0x03 && _beginTrameDetected && !_checkFirstChar) {
						_endTrameDetected = true;
					}

					// Si le debut de la trame a ete detecte, on enregistre...on
					// ne
					// veut pas le caractere de fin evidement ainsi que les
					// caracteres qui ont arrive
					// apres avec le buffer
					if (_beginTrameDetected && !_endTrameDetected) {

						// Verification du premier carac apres reception du
						// carac de debut
						// pour etre sur du debut de trame...
						if (_checkFirstChar) {
							_checkFirstChar = false;
							if (receivedChar != 0x0A) { // Line Feed
								_beginTrameDetected = false;
								continue;
								// System.out.println("different de 0x0D");
								// System.out.println("int char : "+
								// Character.getNumericValue(receivedChar));
							}
						}

						// System.out.println("int char : "+(int)receivedChar);
						_trame.add(receivedChar);

					}
				}

				// Trame complete recue?
				if (_beginTrameDetected && _endTrameDetected)
					_trameFullyReceived = true;
			}
		};

		// return sdl;
	}

	private String GetTeleInfoTrame() throws InterruptedException, IOException {
		_trame = new ArrayList<Character>();
		_beginTrameDetected = false;
		_endTrameDetected = false;
		_trameFullyReceived = false;
		_checkFirstChar = false;
		Serial serial = null;
		SerialDataEventListener sdl = null;

		try {

			serial = SerialFactory.createInstance();

			// open the default serial port provided on the GPIO header at 1200
			// bauds
			// serial.open(_defaultDevice, _defaultBaud);
			SerialConfig config = new SerialConfig();
			config.device(_defaultDevice).baud(Baud._1200).dataBits(DataBits._7).parity(Parity.EVEN)
			.stopBits(StopBits._1).flowControl(FlowControl.NONE);

			sdl = CreateSerialListener();
			serial.addListener(sdl);

			 serial.open(config);
			//serial.open(_defaultDevice, 1200);
			// serial.setBufferingDataReceived(false);

			// serial.discardAll();

			// TODO : traduire tous les messages en anglais
			// serial.close();
			// serial.open("/dev/serial0", 1200);
			// _logger.info("*** ouverture du port serie reussir");

			Date _startTime = new Date();
			while (!_trameFullyReceived) {
				// _logger.info("Buffer Has Data : "+serial.read());
				// System.out.println("Buffer Has Data :
				// "+serial.availableBytes());
				try {
					// wait 1 second before continuing
					Thread.sleep(1000);

					Date currentDate = new Date();

					long diff = currentDate.getTime() - _startTime.getTime();
					long diffMinutes = diff / (60 * 1000);

					// 201603 - Hack pour le lancement du service au
					// demarrage...le serial port n'est pas correctement ouvert
					// la première fois??
					if (diffMinutes >= 1) {
						_logger.warn(
								"Timeout dans la réception d'une trame, relance d'une écoute sur le serial port...");

						SMSDto sms = new SMSDto();
						sms.setMessage("Warning ! automation server did not receive electrical information anymore !");
						_smsGammuService.sendMessage(sms, true);

						// Reinitialisation de la denrière trame reçue!
						_lastTeleInfoTrameReceived = null;

						return null;
					}
				} catch (InterruptedException ie) {
					_logger.error("TeleInfoService : Interrupted Exception", ie);
					throw ie;
				}
			}

			/*
			 * if (_startStopCounter > 0) {
			 * _logger.info("Receive stop collecting teleinfotrame event!");
			 * return null; }
			 */

			// serial.removeListener(_sdl);

			// System.out.println("Trame recue :
			// "+TeleInfoService.ArrayListToStringHelper(trame));
			String trame = TeleInfoService.ArrayListToStringHelper(_trame);
			// _logger.info("Trame recue" + trame);

			return trame;
		}
		// catch(IOException ioe) {
		// _logger.error("Erreur pendant la reception de la trame", ioe);
		// throw ioe;
		// }
		catch (Exception e) {
			_logger.error("Exception dans GetTeleInfoTrame : ", e);
			return null;
		} finally {
			// _logger.info("shut down serial factory");
			// SerialFactory.shutdown();
			if (serial != null) {
				// _logger.info("remove listener");
				serial.discardAll();
				serial.removeListener(sdl);
				// try {
				if (serial.isOpen()) {
					// _logger.info("fermeture port serie");
					serial.close();
				}
				// } catch (IOException ioe) {
				// _logger.error("Impossible de fermer le port serie", ioe);
				// }
			}

			serial = null;
		}
	}

	

	public float GetNextElectricityBillCost() {

		float hc_cost = 0;
		float hp_cost = 0;
		float other_cost = 0;

		_logger.info("Computing next electricity bill cost");

		try {
			HashMap<String, Float> info = GetElectricityBillInfo(_lastBillDate, DateUtils.addDays(_lastBillDate, 59));

			hc_cost = info.get("hc_cost");
			hp_cost = info.get("hp_cost");
			other_cost = info.get("other_cost");
		} catch (Exception e) {
			_logger.error(
					"Error occured while getting electricity bill info...cannot compute next electricity bill cost, returning 0 euros",
					e);
		}

		return hc_cost + hp_cost + other_cost;

	}

	private HashMap<String, Float> GetElectricityBillInfo(Date fromDate, Date toDate) throws Exception {

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
			 * hcConso = 2252; hpConso = 2515; _hcCost = (float) 0.056; _hpCost
			 * = (float) 0.075; _aboCost = (float) 13.64;
			 * System.out.println(hcConso+hpConso);
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

			throw e;
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

		boolean invalidChecksum = false;

		// \r : CR
		// \n : LF

		// String trameInit = trame;

		// Remplacement des lignes feed par des ""
		trame = trame.replaceAll("\\n", "");

		// groupe commence par un line feed \n et termine par un carriage return
		// \r
		String[] groupes = trame.split("\\r");

		if (groupes.length != VALID_GROUPES_NUMBER) {
			// _logger.warn("Reception d'une trame invalide : DEBUT"+trame+"FIN.
			// La trame n'a pas ete prise en compte");
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
				}
			} else {
				invalidChecksum = true;
				// _logger.error("Checksum invalide pour l'etiquette :
				// "+etiquette+", valeur : "+valeur);
			}
		}

		if (invalidChecksum)
			return null;
		else
			return teleInfoTrame;
	}

	// Vérification de la trame recue
	private boolean Checksum(String etiquette, String valeur, String checksum) {
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
		 * _logger.info("etiquette : "+etiquette);
		 * _logger.info("valeur : "+valeur);
		 */
		// _logger.info("sum : "+sum);
		// _logger.info("checksum : "+(int)checksumChar);

		if (sum == checksumChar)
			return true;

		return false;
	}
}
