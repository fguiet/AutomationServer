package fr.guiet.automationserver.business;

import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;


import org.apache.log4j.Logger;
import fr.guiet.automationserver.dto.*;
import fr.guiet.automationserver.dataaccess.DbManager;

public class RoomService implements Runnable {

	// Logger
	private static Logger _logger = Logger.getLogger(RoomService.class);

	private List<Room> _roomList = new ArrayList<Room>();
	private boolean _isStopped = false; // Service arrete?
	private TeleInfoService _teleInfoService = null;
	private List<Heater> _heaterListPhase1 = new ArrayList<Heater>();
	private List<Heater> _heaterListPhase2 = new ArrayList<Heater>();
	private List<Heater> _heaterListPhase3 = new ArrayList<Heater>();
	private List<Heater> _allHeaterList = new ArrayList<Heater>();
	private static final int MAX_INTENSITE_PAR_PHASE = 25;
	private SMSGammuService _smsGammuService = null;
	private Timer _timer = null;
	private DbManager _dbManager = null;
	private Float _hysteresis = null;
	private Float _awayTemp = null;
	private boolean _awayModeStatus = false;
	private boolean _verboseLogEnable = false;

	// Constructeur
	/**
	 * Constructor
	 * 
	 * @param teleInfoService
	 */
	public RoomService(TeleInfoService teleInfoService, SMSGammuService smsGammuService) {

		InputStream is = null;
		try {

			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);

			try {
				String hysteresis = prop.getProperty("heater.hysteresis");
				if (hysteresis != null)
					_hysteresis = Float.parseFloat(hysteresis);
				else
					_hysteresis = Float.parseFloat("0");
			} catch (NumberFormatException nfe) {
				_hysteresis = Float.parseFloat("0");
				_logger.warn("Bad hysteresis defined in config file !, set to 0 by default", nfe);
			}

			String verboseLogEnable = prop.getProperty("log.verbose");
			if (verboseLogEnable != null) {
				if (verboseLogEnable.equals("1"))
					_verboseLogEnable = true;
			}

			try {
				String awayTemp = prop.getProperty("away.temp");
				if (awayTemp != null)
					_awayTemp = Float.parseFloat(awayTemp);
				else
					_awayTemp = Float.parseFloat("0");
			} catch (NumberFormatException nfe) {
				_awayTemp = Float.parseFloat("17.0");
				_logger.warn("Bad away temp defined in config file !, set to 17.0°C by default", nfe);
			}

		} catch (FileNotFoundException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		} catch (IOException e) {
			_logger.error(
					"Erreur lors de la lecture du fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		}

		_teleInfoService = teleInfoService;
		_smsGammuService = smsGammuService;
		_dbManager = new DbManager();
	}

	public void SetAwayModeOn() {
		_awayModeStatus = true;

		for (Room r : _roomList) {
			r.SetWantedTemp(_awayTemp);
		}
	}

	public void SetAwayModeOff() {
		_awayModeStatus = false;

		for (Room r : _roomList) {
			r.ResetWantedTempToDefault();
		}
	}

	public String GetAwayModeStatus() {
		if (_awayModeStatus) {
			return "ON";
		} else {
			return "OFF";
		}
	}

	/**
	 * Add heater to a list according to which phase the heater is link with Add
	 * heater to the global list
	 * 
	 * @param heater
	 */
	private void AddHeater(Heater heater) {
		if (heater.getPhase() == 1) {
			_heaterListPhase1.add(heater);
		}

		if (heater.getPhase() == 2) {
			_heaterListPhase2.add(heater);
		}

		if (heater.getPhase() == 3) {
			_heaterListPhase3.add(heater);
		}

		_allHeaterList.add(heater);
	}

	public List<Room> GetRooms() {
		return _roomList;
	}

	private Room GetRoomById(long roomId) {
		for (Room r : _roomList) {
			if (r.getRoomId() == roomId) {
				return r;
			}
		}

		// Must not occur
		return null;
	}

	public boolean IsSensorResponding(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.IsSensorResponding();
		} else {
			return false;
		}
	}

	public String LastInfoReceived(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.lastInfoReceivedFromSensor();
		} else {
			return "NA";
		}
	}

	// Retourne la t° programmée
	public Float GetTempProg(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.GetTempProg();
		} else {
			return null;
		}
	}

	public boolean AtLeastOneHeaterOn(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.AtLeastOneHeaterOn();
		} else {
			return false;
		}
	}

	public Float GetWantedTemp(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.getWantedTemp();
		} else {
			return null;
		}
	}

	public Float GetBattery(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.getBattery();
		} else {
			return null;
		}
	}
	
	public Float GetRssi(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.getRssi();
		} else {
			return null;
		}
	}
 	
	public Float GetActualTemp(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.getActualTemp();
		} else {
			return null;
		}
	}

	public boolean IsOffForced(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.IsOffForced();
		}

		return false;
	}

	public String NextChangeDefaultTemp(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.NextChangeDefaultTemp();
		}

		return "NA";
	}

	public Float GetActualHumidity(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.getActualHumidity();
		} else {
			return null;
		}
	}

	// Définit la temperature desiree pour une piece
	public void SetWantedTemp(long roomId, float wantedTemp) {

		Room r = GetRoomById(roomId);
		if (r != null) {
			r.SetWantedTemp(wantedTemp);
		}
	}

	public void ForceExtinctionRadiateurs(long roomId) {

		Room r = GetRoomById(roomId);
		if (r != null) {
			r.ForceExtinctionRadiateurs();
		}
	}

	public void DesactiveExtinctionForceRadiateurs(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			r.DesactiveExtinctionForceRadiateurs();
		}
	}

	public Heater getHeaterById(long heaterId) {
		for (Heater h : _allHeaterList) {
			if (h.getId() == heaterId) {
				return h;
			}
		}

		return null;
	}

	// Arret du service RoomService
	public void StopService() {

		if (_timer != null)
			_timer.cancel();

		for (Room r : _roomList) {
			r.StopService();
		}

		_logger.info("Stopping Room Service...");

		_isStopped = true;
	}

	// Création de la tache de sauvegarde en bdd
	private void CreateSaveToDBTask() {

		TimerTask roomServiceTask = new TimerTask() {
			@Override
			public void run() {

				for (Room room : _roomList) {

					// Pas de sauvegarde si une valeur est nulle
					if (room.getActualTemp() != null && room.getWantedTemp() != null
							&& room.getActualHumidity() != null) {
						// DbManager dbManager = new DbManager();
						//_dbManager.SaveSensorInfo(room.getSensor().getIdSendor(), room.getActualTemp(),
						//		room.getWantedTemp(), room.getActualHumidity());
						//_logger.info("Sauvegardee en base de donnees des infos du capteur pour la piece : "
						//		+ room.getName() + ", Temp actuelle : " + room.getActualTemp() + ", Temp désirée : "
						//		+ room.getWantedTemp() + ", Humidité : " + room.getActualHumidity());

						
						_dbManager.SaveSensorInfoInfluxDB(room.getInfluxdbMeasurement(), room.getActualTemp(),
								room.getWantedTemp(), room.getActualHumidity(), room.getBattery());
					}
				}
			}
		};

		_timer = new Timer(true);
		// Toutes les minutes on enregistre
		_timer.schedule(roomServiceTask, 5000, 60000);

	}

	@Override
	public void run() {

		_logger.info("Starting Room Service...");

		CreateSaveToDBTask();

		LoadRoomList();

		for (Room r : _roomList) {
			for (Heater h : r.getHeaterList())
				AddHeater(h);
		}
		
		//Wait a little so information from sensor and teleinformation are receiced
		//avoid warning message in log file
		try {
			_logger.info("Waiting for sensors/teleinfoservice to receive their first information...");
			Thread.sleep(60000);
		}
		catch (Exception e) {
			_logger.error("Error occured in roomservice", e);

			SMSDto sms = new SMSDto();
			sms.setMessage("Error occured in room Service, review error log for more details");
			_smsGammuService.sendMessage(sms, true);
		}

		_logger.info("Activating Room Service...");
		
		while (!_isStopped) {			
			
			try {
				
				if (_teleInfoService.isOperational()) { 
									
					//Gestion du delestage!
					ManageDelestage();

					//Gestion des radiateurs!
					ManageHeaters();					
				}

				// Toutes les 5 secondes
				Thread.sleep(5000);

			} catch (Exception e) {
				_logger.error("Error occured in room Service", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in room Service, review error log for more details");
				_smsGammuService.sendMessage(sms, true);
			}
		}

	}

	/*
	 * Gere le delestage si necessaire...
	 */
	private void ManageDelestage() {

		Collections.reverse(_heaterListPhase1);
		Collections.reverse(_heaterListPhase2);
		Collections.reverse(_heaterListPhase3);

		TeleInfoTrameDto teleInfoTrame = _teleInfoService.GetLastTrame();
		if (teleInfoTrame != null) {
			DelesteHeater(1, teleInfoTrame.IINST1, _heaterListPhase1);
			DelesteHeater(2, teleInfoTrame.IINST2, _heaterListPhase2);
			DelesteHeater(3, teleInfoTrame.IINST3, _heaterListPhase3);
		} else {
			_logger.error("Derniere trame teleinfo recue vide. Calcul pour le delestage impossible.");
		}
		// _logger.info("*** FIN GESTION DELESTAGE ***");
	}

	private void DelesteHeater(int phase, int intensitePhase, List<Heater> heaterList) {

		for (Heater h : heaterList) {
			// _logger.info("INFO DELESTAGE : Radiateur de la piece :
			// "+h.getRoom().getName()+", Etat : "+h.getEtat());
			if (intensitePhase >= MAX_INTENSITE_PAR_PHASE && h.isOn()) {
				_logger.info("Intensite courante phase " + phase + " : " + intensitePhase);
				_logger.info("Delestage du radiateur " + h.getName() + " de la piece : " + h.getRoom().getName());
				h.SetOff();
				intensitePhase = intensitePhase - h.getCurrentConsumption();
			}
		}
	}

	// Methode des gestions de radiateurs des pieces
	private void ManageHeaters() {

		// _logger.info("Pas mort...");

		Collections.sort(_heaterListPhase1);
		Collections.sort(_heaterListPhase2);
		Collections.sort(_heaterListPhase3);

		TeleInfoTrameDto teleInfoTrame = _teleInfoService.GetLastTrame();
		if (teleInfoTrame != null) {
			ManagerHeatersByPhase(1, teleInfoTrame.IINST1, _heaterListPhase1);
			ManagerHeatersByPhase(2, teleInfoTrame.IINST2, _heaterListPhase2);
			ManagerHeatersByPhase(3, teleInfoTrame.IINST3, _heaterListPhase3);
		} else {
			// ICi il faut tout coupé au cas ou des radiateurs soit allumés...
			for (Heater h : _allHeaterList) {
				if (h.isOn())
					h.SetOff();
			}

			_logger.error("Derniere trame de teleinfo recue vide. Gestion des radiateurs impossible.");
		}
	}

	// Gestion des radiateurs par phase
	private void ManagerHeatersByPhase(int phase, int intensitePhase, List<Heater> _heaterList) {
		// _logger.info("Je vais gérer les radiateurs maintenant...");
		// _logger.info("Intensite courante phase "+phase+" : "+intensitePhase);
		for (Heater h : _heaterList) {

			Float roomWantedTemp = h.getRoom().ComputeWantedTemp(_awayModeStatus, _awayTemp);
			Float roomActualTemp = h.getRoom().getActualTemp();

			String tempProg = "NA";
			if (h.getRoom().GetTempProg() != null)
				tempProg = "" + h.getRoom().GetTempProg();

			String calcProg = "NA";
			if (roomWantedTemp != null)
				calcProg = "" + roomWantedTemp;

			String actualTemp = "NA";
			if (roomActualTemp != null)
				actualTemp = "" + roomActualTemp;

			if (_verboseLogEnable)
				_logger.info("***LOG : Radiateur  " + h.getName() + " de la pièce " + h.getRoom().getName() + ", Etat : "
						+ h.getEtatLabel() + ", T° prog :" + tempProg + ", T° piece : " + actualTemp + ", T° desire : "
						+ calcProg);

			// On arrive pas a obtenir la temperature courante de la piece donc
			// on eteint le radiateur
			if (h.isOn() && roomActualTemp == null) {
				h.SetOff();
				_logger.warn("Impossible de déterminer la temperature de la piece : " + h.getName()
						+ ". Extinction du radiateur. Vérifier le capteur.");
				continue;
			}

			if (!h.isOn() && roomActualTemp == null) {
				_logger.warn("Impossible de déterminer la temperature de la piece : " + h.getName()
						+ ". Vérifier le capteur.");
				continue;
			}

			if (h.isOn() && roomWantedTemp == null) {
				h.SetOff();
				_logger.warn("Impossible de déterminer la temperature voulue pour de la piece : " + h.getName()
						+ ". Extinction du radiateur.");
				continue;
			}

			if (!h.isOn() && roomWantedTemp == null) {
				_logger.warn("Impossible de déterminer la temperature voulue pour de la piece : " + h.getName() + ".");
				continue;
			}

			// Le radiateur est eteint
			if (!h.isOn()) {
				if (roomActualTemp < roomWantedTemp - _hysteresis) {
					if (h.getCurrentConsumption() + intensitePhase < MAX_INTENSITE_PAR_PHASE) {

						if (!h.IsOffForced()) {
							h.SetOn();
							_logger.info("ALLUMAGE Radiateur " + h.getName() + ", Temp. de la pièce : " + roomActualTemp
									+ ", Temp. desiree : " + roomWantedTemp);
							_logger.info("*** Intensite phase : " + phase + " après activation du radiateur : "
									+ (intensitePhase + h.getCurrentConsumption()));
							intensitePhase = intensitePhase + h.getCurrentConsumption();
						} else {
							_logger.info("Impossibilite d'allumer le radiateur " + h.getName() + " de la piece : "
									+ h.getRoom().getName() + ". L'utilisateur a forcé l'extinction manuelle");
						}
					} else {
						_logger.info("Impossibilite d'allumer le radiateur " + h.getName() + " de la piece : "
								+ h.getRoom().getName() + ". Depassement de l'intensite pour la phase " + phase);
					}
				}
			}

			if (h.isOn() && roomActualTemp >= roomWantedTemp + _hysteresis) {
				h.SetOff();
				_logger.info("EXTINCTION Radiateur " + h.getName() + ", Temp. de la pièce : " + roomActualTemp
						+ ", Temp. desiree : " + roomWantedTemp);
				_logger.info("*** Intensite phase : " + phase + " après extinction du radiateur : "
						+ (intensitePhase - h.getCurrentConsumption()));
			}
		}	
	}

	private void LoadRoomList() {
		
		_logger.info("Chargement de la liste des pièces...");
		
		// Initialisation des pieces et chauffages
		// DbManager dbManager = new DbManager();
		List<RoomDto> roomDtoList;
		try {
			roomDtoList = _dbManager.GetRooms();
			
			for (RoomDto dtoRoom : roomDtoList) {
                                
				Room r = Room.LoadFromDto(dtoRoom, _smsGammuService);				
				_roomList.add(r);
				
				_logger.info("Chargement de la pièce : " + r.getName());
			}
		} catch (Exception e) {
			_logger.error("Erreur lors de la récupération de la liste des pièces");
			SMSDto sms = new SMSDto();
			sms.setMessage("Error occured in room Service, review error log for more details");
			_smsGammuService.sendMessage(sms, true);
		}		
	}

}
