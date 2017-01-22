package fr.guiet.automationserver.business;

import org.apache.log4j.Logger;

import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialDataEvent;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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
	// private SerialDataEventListener _sdl = null;
	private String _defaultDevice = "";
	private static final int VALID_GROUPES_NUMBER = 17;
	private boolean _beginTrameDetected = false;
	private boolean _endTrameDetected = false;
	private boolean _trameFullyReceived = false;
	private boolean _checkFirstChar = false;
	private TeleInfoTrameDto _lastTeleInfoTrameReceived = null;
	private ArrayList<Character> _trame = null;
	private Timer _timer = null;
	private SMSGammuService _smsGammuService = null;
	private int _startStopCounter = 0;
	//private boolean _isCollectTeleInfoStopped = false;
	private List<ICollectInfoStopListener> _collectInfoStopListeners = new ArrayList<ICollectInfoStopListener>();
	
	public void addListener(ICollectInfoStopListener toAdd) {
		_collectInfoStopListeners.add(toAdd);
    }

    private void NotifyCollectInfoStop() {

        // Notify everybody that may be interested.
        for (ICollectInfoStopListener l : _collectInfoStopListeners)
        	l.OnCollectInfoStopped();
    }
	
	public TeleInfoService(SMSGammuService smsGammuService) {
		_smsGammuService = smsGammuService;
	}

	public void StopCollectingTeleinfo(String initiator) {
		_logger.info(String.format("Stopping collect of teleinfo (initiator is %s)", initiator));
		_startStopCounter++;
	}

	public void StartCollectingTeleinfo(String initiator) {
		_logger.info(String.format("Starting collect of teleinfo (initiator is %s)", initiator));
		_startStopCounter--;
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

		// Création de la tâche de sauvegarde en bdd
		CreateSaveToDBTask();

		while (!_isStopped) {

			try {

				if (_startStopCounter !=0) {
					NotifyCollectInfoStop();					
					_logger.info("ok teleinfoservice stoppé!!");
					Thread.sleep(2000);
					continue;
				} 

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

				// On pause le Thread pendant deux secondes...
				// recup des trames toutes les deux secondes
				// Thread.sleep(2000);

			} catch (Exception e) {
				_logger.error("Error occured in TeleInfo service...", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in TeleInfo services service, review error log for more details");
				_smsGammuService.sendMessage(sms, true);
			}
		}
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

		DbManager dbManager = new DbManager();
		dbManager.SaveTeleInfoTrame(teleInfoTrame);
		// _logger.info("Sauvegarde de la trame teleinfo en base de données");

		// if (System.getProperty("SaveToInfluxDB").equals("TRUE")) {
		dbManager.SaveTeleInfoTrameToInfluxDb(teleInfoTrame);
		// _logger.info("Sauvegarde de la trame teleinfo dans InfluxDB");
		// }
	}

	// Creation du listener sur le port serie
	private SerialDataEventListener CreateSerialListener() {

		SerialDataEventListener sdl = new SerialDataEventListener() {
			@Override
			public void dataReceived(SerialDataEvent event) {

				if (_trameFullyReceived)
					return;

				String dataSZ = "";
				try {
					dataSZ = event.getAsciiString();
				} catch (IOException e) {
					_logger.error("Unable de read serial port", e);
				}

				char[] data = dataSZ.toCharArray();

				for (int i = 0; i < data.length; i++) {
					char receivedChar = data[i];
					receivedChar &= 0x7F;

					// _logger.info("carac recu: "+(int)receivedChar);

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

		return sdl;
	}

	private String GetTeleInfoTrame() throws InterruptedException, IOException {
		_trame = new ArrayList<Character>();
		_beginTrameDetected = false;
		_endTrameDetected = false;
		_trameFullyReceived = false;
		_checkFirstChar = false;
		Serial serial = SerialFactory.createInstance();
		SerialDataEventListener sdl = null;

		try {

			// open the default serial port provided on the GPIO header at 1200
			// bauds
			// serial.open(_defaultDevice, _defaultBaud);
			SerialConfig config = new SerialConfig();
			config.device(_defaultDevice).baud(Baud._1200).dataBits(DataBits._7).parity(Parity.NONE)
					.stopBits(StopBits._1).flowControl(FlowControl.NONE);

			sdl = CreateSerialListener();
			serial.addListener(sdl);

			// TODO : voir le probleme au demarrage de l'appli...Exception in
			// thread "Thread-305"
			// java.util.concurrent.RejectedExecutionException: Task
			// com.pi4j.io.serial.tasks.SerialDataEventDispatchTaskImpl@736daa
			// rejected from
			// java.util.concurrent.ThreadPoolExecutor@1214351[Terminated, pool
			// size = 0, active threads = 0, queued tasks = 0, completed tasks =
			// 4]
			serial.open(config);

			// TODO : traduire tous les messages en anglais
			// serial.close();
			// serial.open("/dev/serial0", 1200);
			// _logger.info("*** ouverture du port serie reussir");

			Date _startTime = new Date();
			while (!_trameFullyReceived && _startStopCounter == 0) {
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
						
						if (_startStopCounter > 0)
							_logger.warn("INFO : La collecte de teleinfo est stoppée");

						SMSDto sms = new SMSDto();
						sms.setMessage("Warning ! automation server did not receive electrical information anymore !");
						_smsGammuService.sendMessage(sms, true);

						// Reinitialisation de la denrière trame reçue!
						_lastTeleInfoTrameReceived = null;

						return null;
					}
				} catch (InterruptedException ie) {
					throw ie;
				}
			}

			if (_startStopCounter > 0) {
				_logger.info("Receive stop collecting teleinfotrame event!");
				return null;
			}

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
			throw e;
		} finally {
			if (serial != null) {
				// _logger.info("remove listener");
				serial.removeListener(sdl);
				try {
					if (serial.isOpen()) {
						// _logger.info("fermeture port serie");
						serial.close();
					}
				} catch (IOException ioe) {
					_logger.error("Impossible de fermer le port serie", ioe);
				}
			}
		}
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
