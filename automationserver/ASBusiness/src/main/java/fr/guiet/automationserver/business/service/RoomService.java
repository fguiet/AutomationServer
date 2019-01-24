package fr.guiet.automationserver.business.service;

import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;


import org.apache.log4j.Logger;
import fr.guiet.automationserver.dto.*;
import fr.guiet.automationserver.business.Heater;
import fr.guiet.automationserver.business.Room;
import fr.guiet.automationserver.business.helper.MqttClientHelper;
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
	//private MqttService _mqttService = null;
	private Timer _timer = null;
	private Timer _timer2 = null;
	private DbManager _dbManager = null;
	private Float _hysteresis = null;
	private Float _awayTemp = null;
	private boolean _awayModeStatus = false;
	private boolean _verboseLogEnable = false;
	private static String MQTT_CLIENT_ID = "roomServiceCliendId";
	private MqttClientHelper _mqttClient = null;
	private boolean _roomListLoaded = false;

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
		_mqttClient = new MqttClientHelper(MQTT_CLIENT_ID);
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
	
	public ArrayList<IMqttable> getMqttableClients() {
		
		ArrayList<IMqttable> m = new ArrayList<IMqttable>();
		for (Room r : _roomList) {
			m.add(r.getSensor());
		}
		
		return m;
	}

	/*public boolean isSensorOperational(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.isSensorOperational();
		} else {
			return false;
		}
	}

	public String getLastSensorUpdate(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.getLastSensorUpdate();
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
	}/*

	/*public Float GetBattery(long roomId) {
		Room r = GetRoomById(roomId);
		if (r != null) {
			return r.getBatteryVoltage();
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
	}*/

	/*public boolean IsOffForced(long roomId) {
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
	}*/

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

		if (_timer2 != null)
			_timer2.cancel();
		
		for (Room r : _roomList) {
			r.StopService();
		}

		_logger.info("Stopping Room Service...");

		_isStopped = true;
	}
	
	//Publishing room information to mqtt
	private void createPublishMqttRoomInfoTask() {
		
		_logger.info("Creating publish mqtt room info task");
		
		TimerTask publishMqttRoomInfo = new TimerTask() {
			@Override
			public void run() {
				
				try {

					for (Room room : _roomList ) {
						
						if (room.getMqttTopic() != null) {
							String message = FormatRoomInfoMessage(room);
							_mqttClient.SendMsg(room.getMqttTopic(), message);
						}
					}
				}
				catch(Exception e) {
					_logger.error("Error occured in publish mqtt room task",e);
				}
			}
		};

		_timer2 = new Timer(true);
		//Publish room information every 10s
		_timer2.schedule(publishMqttRoomInfo, 5000, 10000);
		
		_logger.info("Publish mqtt room info task has been created.");
	}
	
	private String FormatRoomInfoMessage(Room room) {

		String actualTemp = "NA";
		if (room.getActualTemp() != null) {
			actualTemp = String.format("%.2f", room.getActualTemp());
		}
	
		String wantedTemp = "NA";
		if (room.getWantedTemp() != null) {
			wantedTemp = String.format("%.2f", room.getWantedTemp());
		}
	
		String actualHumidity = "NA";
		if (room.getActualHumidity() != null) {
			actualHumidity = String.format("%.2f", room.getActualHumidity());
		}
	
		String nextDefaultTemp = room.NextChangeDefaultTemp();
	
		String hasHeaterOn = "HEATEROFF";
		if (room.AtLeastOneHeaterOn()) {
			hasHeaterOn = "HEATERON";
		}
	
		String progTemp = "NA";
		Float tempProg = room.GetTempProg();
		if (tempProg != null) {
			progTemp = String.format("%.2f", tempProg);
		}
	
		String offForced = "FORCEDHEATEROFF";
		if (room.IsOffForced()) {
			offForced = "FORCEDHEATERON";
		}
	
		String sensorKO = "SENSORKO";
		if (room.isSensorOperational()) {
			sensorKO = "SENSOROK";
		}
	
		String lastSensorUpdate = room.getLastSensorUpdate();
	
		// Battery
		String battery = "NA";
		if (room.getBatteryVoltage() != null) {
			battery = String.format("%.2f", room.getBatteryVoltage() );
		}
		
		// Rssi
		String rssi = "NA";
		if (room.getRssi() != null) {
			rssi = String.format("%.2f", room.getRssi());
		}
		
		String message = actualTemp + ";" + actualHumidity + ";" + progTemp + ";" + nextDefaultTemp + ";" + hasHeaterOn
		+ ";" + offForced + ";" + sensorKO + ";" + wantedTemp + ";" + lastSensorUpdate + ";" + battery + ";" + rssi;
			
		return message;
	}

	@Override
	public void run() {

		_logger.info("Starting Room Service...");
		
		createPublishMqttRoomInfoTask();

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

			SMSDto sms = new SMSDto("c4b3b685-992f-4973-8cb8-6522add19edb");
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

				SMSDto sms = new SMSDto("d9dfbe55-03a4-4fba-977c-ec07e9eb8477");
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
	
	public boolean isRoomListLoaded() {
		return _roomListLoaded;
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
			
			_roomListLoaded = true;
			
			_logger.info("Chargement de la liste des pièces terminé...");
			
		} catch (Exception e) {
			_logger.error("Erreur lors de la récupération de la liste des pièces");
			SMSDto sms = new SMSDto("91d7b078-3d5b-4890-9951-f01cf0c1ed5f");
			sms.setMessage("Error occured in room Service, review error log for more details");
			_smsGammuService.sendMessage(sms, true);
		}		
	}

}
