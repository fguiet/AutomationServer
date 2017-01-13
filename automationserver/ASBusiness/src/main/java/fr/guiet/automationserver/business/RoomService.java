package fr.guiet.automationserver.business;

import java.util.List;
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
	private TeleInfoService _teleInfoService;
	private List<Heater> _heaterListPhase1 = new ArrayList<Heater>();
	private List<Heater> _heaterListPhase2 = new ArrayList<Heater>();
	private List<Heater> _heaterListPhase3 = new ArrayList<Heater>();
	private List<Heater> _allHeaterList = new ArrayList<Heater>();
	private static final int MAX_INTENSITE_PAR_PHASE = 25;
	private SMSGammuService _smsGammuService = null;
	
	// Constructeur
	/**
	 * Constructor
	 * 
	 * @param teleInfoService
	 */
	public RoomService(TeleInfoService teleInfoService, SMSGammuService smsGammuService) {
		_teleInfoService = teleInfoService;		
		_smsGammuService = smsGammuService;
	}

	/**
	 * Add heater to a list according to which phase the heater is link with
	 * Add heater to the global list  
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

	// Arret du service RoomService
	public void StopService() {

		for (Room r : _roomList) {
			r.StopService();
		}

		_logger.info("Stopping Room Service...");

		_isStopped = true;
	}

	@Override
	public void run() {

		_logger.info("Starting Room Service...");
		// Date startTime = new Date();

		LoadRoomList();

		for (Room r : _roomList) {
			for (Heater h : r.getHeaterList())
				AddHeater(h);
		}

		// Extinction des radiateurs par defaut

		while (!_isStopped) {

			try {
				// Gestion du delestage!
				ManageDelestage();

				// Gestion des radiateurs!
				ManageHeaters();

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

	private void DelesteHeater(int phase, int intensitePhase, List<Heater> _heaterList) {

		for (Heater h : _heaterList) {
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
			_logger.error("Derniere trame de teleinfo recue vide. Gestion des radiateurs impossible.");
		}
	}

	// Gestion des radiateurs par phase
	private void ManagerHeatersByPhase(int phase, int intensitePhase, List<Heater> _heaterList) {
		// _logger.info("Je vais gérer les radiateurs maintenant...");
		// _logger.info("Intensite courante phase "+phase+" : "+intensitePhase);
		for (Heater h : _heaterList) {
			// _logger.info("Radiateur de la piece : "+h.getRoom().getName()+",
			// Etat : "+h.getEtat());
			// Si intensite de radiateur + intensite phase < intensite max et si
			// radiateur eteint et si temp de la chambre < temp desire alors
			// on allume le radiateur

			/*
			 * if (!h.getRoom().IsTempInfoOk()) { //Extinction du radiateur par
			 * securite h.SetOff(); _logger.
			 * warn("La valeur temp. actuelle ou temp. desiree est non renseignee pour la piece : "
			 * +h.getRoom().getName()+
			 * ". Impossible de gerer le radiateur. Extinction du radiateur par sécurité"
			 * ); continue; }
			 */

			Float roomWantedTemp = h.getRoom().ComputeWantedTemp();
			Float roomActualTemp = h.getRoom().getActualTemp();

			/*
			 * String etat = "ALLUME"; if (!h.isOn()) { etat = "ETEINT"; }
			 */

			String tempProg = "NA";
			if (h.getRoom().GetTempProg() != null)
				tempProg = "" + h.getRoom().GetTempProg();

			String calcProg = "NA";
			if (roomWantedTemp != null)
				calcProg = "" + roomWantedTemp;

			String actualTemp = "NA";
			if (roomActualTemp != null)
				actualTemp = "" + roomActualTemp;

			_logger.info("***LOG : Radiateur de la piece : " + h.getRoom().getName() + ", Etat : " + h.getEtatLabel()
					+ ", T° prog :" + tempProg + ", T° piece : " + actualTemp + ", T° desire : " + calcProg);

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
				if (roomActualTemp < roomWantedTemp) {
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

			// Si radiateur allume on etient (on positionne un delta d'inertie
			// de chaleur de 0.2 degre)
			if (h.isOn() && roomActualTemp >= roomWantedTemp) {
				h.SetOff();
				_logger.info("EXTINCTION Radiateur " + h.getName() + ", Temp. de la pièce : " + roomActualTemp
						+ ", Temp. desiree : " + roomWantedTemp);
				_logger.info("*** Intensite phase : " + phase + " après extinction du radiateur : "
						+ (intensitePhase - h.getCurrentConsumption()));
			}
		}
	}

	private void LoadRoomList() {
		// Initialisation des pieces et chauffages
		DbManager dbManager = new DbManager();
		List<RoomDto> roomDtoList = dbManager.GetRooms();

		for (RoomDto dtoRoom : roomDtoList) {

			Room r = Room.LoadFromDto(dtoRoom);
			_roomList.add(r);
		}
	}

}
