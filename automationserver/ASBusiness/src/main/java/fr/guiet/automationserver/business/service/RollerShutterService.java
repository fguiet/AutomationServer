package fr.guiet.automationserver.business.service;


//import java.util.TimeZone;
import org.apache.log4j.Logger;

import fr.guiet.automationserver.business.RollerShutter;
import fr.guiet.automationserver.business.RollerShutterState;
import fr.guiet.automationserver.dto.SMSDto;


/*si automatic management on 
ET
sunrise time < à 6h30
alors ouvre le volet

si automatic management on 
ET
sunrse time >= à 6h30 et sunrise time < à 7h40-10minutes
alors ouverture = sunrise time (scheduler à mettre en place)


Fermeture des volets à 7h40

pour ouverture = verifier si pas deja ouvert (appel via http)*/


public class RollerShutterService implements Runnable {
    
	// Logger
	private static Logger _logger = Logger.getLogger(RollerShutterService.class);
		
	private boolean _automaticManagementStatus = true; //By default
	private boolean _isStopped = false; // Service arrete?
	private SMSGammuService _smsGammuService = null;
		
	private RollerShutter _rsWest = null;
	private RollerShutter _rsNorth = null;
		
	private int ROLLERSHUTTER_WEST_ID = 7;
	private int ROLLERSHUTTER_NORTH_ID = 8;
		
	public RollerShutterService(SMSGammuService smsGammuService) {
		
		_smsGammuService = smsGammuService;
	}
	
	public String GetAutomaticManagementStatus() {
		if (_automaticManagementStatus) {
			return "ON";
		} else {
			return "OFF";
		}
	}
	
	public void SetAutomaticManagementOff() {
		_automaticManagementStatus = false;
		_logger.info("Setting rollershutter automatic management OFF");
	}

	public void SetAutomaticManagementOn() {
		_automaticManagementStatus = true;
		_logger.info("Setting rollershutter automatic management ON");
	}
	
	@Override
	public void run() {
		
		_logger.info("Starting rollershutters management...");		
		
		_rsWest = new RollerShutter(ROLLERSHUTTER_WEST_ID, "Volet roulant Ouest", _smsGammuService);
		_rsNorth = new RollerShutter(ROLLERSHUTTER_NORTH_ID,"Volet roulant Nord", _smsGammuService);
	
		//CRON pattern
		//minute hour day of month (1-31) month (1-12) day of week (0 sunday, 6 saturday)
		
		//Get Europe/Paris TimeZone
		//TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");		

		while (!_isStopped) {

			try {
												
				// Sleep for 2 minutes
				Thread.sleep(120000);
				
				//Check state here and send message if not responding!!
				
				_rsNorth.HasTimeoutOccured();
				_rsWest.HasTimeoutOccured();
				
				//Ask State to roller shutter every two minutes
				_rsNorth.RequestState();
				_rsWest.RequestState();
				
				
				
			} catch (Exception e) {
				_logger.error("Error occured in rollershutter management service", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in rollershutter management service, review error log for more details");
				_smsGammuService.sendMessage(sms, true);
			}
		}		
	}	
	
	public void setState(long rsId, RollerShutterState state) {
		if (rsId == ROLLERSHUTTER_WEST_ID)
			_rsWest.setState(state, true);
		
		if (rsId == ROLLERSHUTTER_NORTH_ID)
			_rsNorth.setState(state, true);
			
	}	
	
	public String getNorthRSState() {
		return _rsNorth.getState().name();
	}
	
	public String getWestRSState() {
		return _rsWest.getState().name();
	}
	
	public void CloseAllRollerShutters() {
		CloseRollerShutters(true, true);
	}
	
	private void CloseRollerShutters(boolean closeNorthRS, boolean closeWestRS) {
		//Only if automatic management status is activated!
		if (_automaticManagementStatus) {
			if (_rsWest.getState() != RollerShutterState.CLOSED && closeWestRS) {
				_rsWest.Close();				
				_logger.info("Close requested for west rollershutter");				
			}
			else {
				_logger.info("Close requested for west rollershutter but it is already closed...");
			}
			
			if (_rsNorth.getState() != RollerShutterState.CLOSED && closeNorthRS) {
				_rsNorth.Close();				
				_logger.info("Close requested for north rollershutter");				
			}
			else {
				_logger.info("Close requested for north rollershutter but it is already closed...");
			}
		}
		else {
			_logger.info("Close requested but automatic management is OFF, close canceled...");
		}
	}
	
	public void OpenAllRollerShutters() {
		//Only if automatic management status is activated!
		if (_automaticManagementStatus) {
		
			if (_rsWest.getState() != RollerShutterState.OPENED) {
				_rsWest.Open();				
				_logger.info("Open requested for west rollershutter");
				
			}
			
			if (_rsNorth.getState() != RollerShutterState.OPENED) {
				_rsNorth.Open();				
				_logger.info("Open requested for north rollershutter");				
			}
		}	
		else {
			_logger.info("Open requested but automatic management is OFF, Open canceled...");
		}
	}	
	
	public void StopService() {
		
		//if (_timer != null)
		//	_timer.cancel();
		
		//_rollerShutterScheduler.stop();
		
		_logger.info("Stopping RollerShutter service...");

		_isStopped = true;
	}	
}
