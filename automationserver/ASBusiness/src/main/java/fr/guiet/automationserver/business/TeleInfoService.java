package fr.guiet.automationserver.business;

import org.apache.log4j.Logger;

import com.pi4j.io.serial.*;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialDataEvent;

import java.util.Date;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
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
	private SerialDataEventListener _sdl = null;
	private String _defaultDevice = "/dev/serial0";
	private int _defaultBaud = 1200;
	private static final int VALID_GROUPES_NUMBER = 17;
	private boolean _beginTrameDetected = false;
	private boolean _endTrameDetected = false;
	private boolean _trameFullyReceived = false;
	private boolean _checkFirstChar = false;
	private TeleInfoTrame _lastTeleInfoTrameReceived = null;
	private ArrayList<Character> _trame = null;
	private Timer _timer = null;

	@Override
	public void run() {

		_logger.info("Démarrage du service TeleInfoService...");
		
		try {
			_defaultBaud = Integer.parseInt(System.getProperty("serialBaud"));	
			_defaultDevice = System.getProperty("serialDevice");
		}
		catch (Exception e) {
			_defaultBaud = 1200;
			_defaultDevice = "/dev/serial0";
			_logger.error("Could not set baud rate and device, set /dev/serial0 and 1200 bauds by defaults");
		}
		
		// Creation de listener
		CreateSerialListener();

		// Création de la tâche de sauvegarde en bdd
		CreateSaveToDBTask();

		while (!_isStopped) {

			try {

				// Recuperation de la trame de teleinfo
				String trameReceived = GetTeleInfoTrame();
				// _logger.error("Test TeleInfoService...");
				if (trameReceived != null) {

					// Decodage de la trame
					TeleInfoTrame teleInfoTrame = DecodeTrame(trameReceived);

					if (teleInfoTrame != null) {
						// _logger.info("Valorisation trame recu");
						_lastTeleInfoTrameReceived = teleInfoTrame;
					}
				}

				// On pause le Thread pendant deux secondes...
				// recup des trames toutes les deux secondes
				Thread.sleep(2000);
			} catch (Exception e) {
				_logger.error("Une erreur est apparue dans TeleInfoService...", e);
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

	private TeleInfoTrameDto ConvertToDto(TeleInfoTrame teleInfoTrame) {

		TeleInfoTrameDto dto = new TeleInfoTrameDto();

		dto.ADCO = teleInfoTrame.ADCO;
		dto.OPTARIF = teleInfoTrame.OPTARIF;
		dto.ISOUSC = teleInfoTrame.ISOUSC;
		dto.HCHC = teleInfoTrame.HCHC;
		dto.HCHP = teleInfoTrame.HCHP;
		dto.PTEC = teleInfoTrame.PTEC;
		dto.IINST1 = teleInfoTrame.IINST1;
		dto.IINST2 = teleInfoTrame.IINST2;
		dto.IINST3 = teleInfoTrame.IINST3;
		dto.IMAX1 = teleInfoTrame.IMAX1;
		dto.IMAX2 = teleInfoTrame.IMAX2;
		dto.IMAX3 = teleInfoTrame.IMAX3;
		dto.PMAX = teleInfoTrame.PMAX;
		dto.PAPP = teleInfoTrame.PAPP;
		dto.HHPHC = teleInfoTrame.HHPHC;
		dto.MOTDETAT = teleInfoTrame.MOTDETAT;
		dto.PPOT = teleInfoTrame.PPOT;

		return dto;

	}

	// Arret du service TeleInfoService
	public void StopService() {

		if (_timer != null)
			_timer.cancel();

		_logger.info("Arrêt du service TeleInfoService...");

		_isStopped = true;
	}

	// Récupération de la dernière trame teleinfo recue
	public synchronized TeleInfoTrame GetLastTrame() {
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
	private void SaveTrameToDb(TeleInfoTrame teleInfoTrame) {
		DbManager dbManager = new DbManager();
		dbManager.SaveTeleInfoTrame(ConvertToDto(teleInfoTrame));
		_logger.info("Sauvegarde de la trame teleinfo en base de données");
		if (System.getProperty("SaveToInfluxDB").equals("TRUE")) {
			dbManager.SaveTeleInfoTrameToInfluxDb(ConvertToDto(teleInfoTrame));
			_logger.info("Sauvegarde de la trame teleinfo dans InfluxDB");
		}
	}

	// Creation du listener sur le port serie
	private void CreateSerialListener() {

		_sdl = new SerialDataEventListener() {
			@Override
			public void dataReceived(SerialDataEvent event) {

				if (_trameFullyReceived)
					return;

				// Convert en char[]

				// String dataSz = "";
				// try {
				// dataSz = event.getAsciiString();
				// _logger.info("Info recue : "+dataSz);
				// }
				// catch(IOException ex) {
				// _logger.error("Impossible de lire le port Serie pour
				// receptionner la trame TeleInfo");
				// }
				//char[] data = new char[event.getData().length()];
				//data = event.getData().toCharArray();
				
				CharBuffer cb = null;
				try {
					cb = event.getCharBuffer(StandardCharsets.UTF_8);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				char[] data = cb.array();
				
				//char[] data = dataSZ.toCharArray();

				for (int i = 0; i < data.length; i++) {
					char receivedChar = data[i];
					receivedChar &= 0x7F;

					//_logger.info("carac recu: "+(int)receivedChar);

					// System.out.println("int char : "+(int)receivedChar);
					String decoded = String.valueOf(receivedChar);
					_logger.warn("carac recu: "+decoded);
					//System.out.println(decoded);

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
	}

	private synchronized String GetTeleInfoTrame() throws InterruptedException,IOException {
		_trame = new ArrayList<Character>();
		_beginTrameDetected = false;
		_endTrameDetected = false;
		_trameFullyReceived = false;
		_checkFirstChar = false;
		Serial serial = SerialFactory.createInstance();

		try {

			// open the default serial port provided on the GPIO header at 1200
			// bauds
			//serial.open(_defaultDevice, _defaultBaud);
			SerialConfig config = new SerialConfig();
			config.device("/dev/serial0")
                  .baud(Baud._1200)
                  .dataBits(DataBits._7)
                  .parity(Parity.NONE)
                  .stopBits(StopBits._1)
                  .flowControl(FlowControl.NONE);
			
			serial.open(config);

			serial.addListener(_sdl);

			// serial.close();
			// serial.open(DEFAULT_COM_PORT, 1200);
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
						return null;
					}
				} catch (InterruptedException ie) {
					throw ie;
				}
			}

			// serial.removeListener(_sdl);

			// System.out.println("Trame recue :
			// "+TeleInfoService.ArrayListToStringHelper(trame));
			String trame = TeleInfoService.ArrayListToStringHelper(_trame);
			_logger.info("Trame recue" + trame);

			return trame;
		}
		// catch(IOException ioe) {
		// _logger.error("Erreur pendant la reception de la trame", ioe);
		// throw ioe;
		// }
		catch (Exception e) {
			throw e;
		} finally {
			if (serial != null && serial.isOpen()) {
				serial.removeListener(_sdl);
				try {
				   serial.close();
				}
				catch(IOException ioe) {
				 _logger.error("Impossible de fermer le port serie",ioe);
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
	private TeleInfoTrame DecodeTrame(String trame) {

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

		TeleInfoTrame teleInfoTrame = new TeleInfoTrame();

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
