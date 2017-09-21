package fr.guiet.automationserver.business;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import fr.guiet.automationserver.dto.SMSDto;
import it.sauronsoftware.cron4j.Scheduler;

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
    
	//I think only one scheduler can be used instead of many
	
	// Logger
	private static Logger _logger = Logger.getLogger(RollerShutterService.class);
	
	private Calendar _sunset = null;
	private Calendar _sunrise = null;
	private boolean _automaticManagementStatus = false; //By default
	private boolean _isStopped = false; // Service arrete?
	private SMSGammuService _smsGammuService = null;
	private Scheduler _computeSunSetSunRiseScheduler = null;
	private String _cronComputeSunSetSunRiseScheduler = null;
	private Scheduler _weekCloseScheduler = null;
	private String _cronWeekCloseScheduler = null;
	private Scheduler _weekOpenScheduler = null;
	private String _cronWeekOpenScheduler = null;
	private Scheduler _weeknightCloseScheduler = null;
	private String _weeknightCloseSchedulerId = null;
	
	private Scheduler _weekmorningOpenScheduler = null;
	private String _weekmorningOpenSchedulerId = null;
	
	private String _apikey = null;
	private String _baseUrlRs1 = null;
	private String _baseUrlRs2 = null;
	private RollerShutter _rsWest = null;
	private RollerShutter _rsNorth = null;
	private boolean _alertSent5 = false;
	private boolean _alertSent10 = false;
	private boolean _alertSentMore = false;
	
	public RollerShutterService(SMSGammuService smsGammuService) {
				
		InputStream is = null;
		try {

			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);
			
			String apikey = prop.getProperty("url.apikey");
			if (apikey != null)
				_apikey = apikey;
			else
				_apikey = "xxx";
							
			String baseUrlRs1 = prop.getProperty("url.rollershutter_west");
			if (baseUrlRs1 != null) {
				_baseUrlRs1 = baseUrlRs1;
			}
			else {
				_baseUrlRs1 = "http://127.0.0.1";
			}
			
			String baseUrlRs2 = prop.getProperty("url.rollershutter_north");
			if (baseUrlRs2 != null) {
				_baseUrlRs2 = baseUrlRs2;
			}
			else {
				_baseUrlRs2 = "http://127.0.0.1";
			}
			
			String cronComputeSunSetSunRiseScheduler = prop.getProperty("scheduler.sunsetsunrise");
			if (cronComputeSunSetSunRiseScheduler != null) {
				_cronComputeSunSetSunRiseScheduler = cronComputeSunSetSunRiseScheduler;
			}
			else {
				_cronComputeSunSetSunRiseScheduler = "0 5 * * *";
			}
			
			String cronWeekCloseScheduler = prop.getProperty("scheduler.weekclose");
			if (cronWeekCloseScheduler != null) {
				_cronWeekCloseScheduler = cronWeekCloseScheduler;
			}
			else {
				_cronWeekCloseScheduler = "47 7 * * 1,2,3,4,5";
			}
			
			String cronWeekOpenScheduler = prop.getProperty("scheduler.weekopen");
			if (cronWeekOpenScheduler != null) {
				_cronWeekOpenScheduler = cronWeekOpenScheduler;
			}
			else {
				_cronWeekOpenScheduler = "30 6 * * 1,2,3,4,5";
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
		
		_rsWest = new RollerShutter("1", "Volet roulant Ouest", _baseUrlRs1, _apikey);
		_rsNorth = new RollerShutter("2","Volet roulant Nord", _baseUrlRs2, _apikey);
		
		ComputeSunsetSunrise();
		
		//CRON pattern
		//minute hour day of month (1-31) month (1-12) day of week (0 sunday, 6 saturday)
		
		//Get Europe/Paris TimeZone
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		
		_logger.info("Starting compute sunrise/sunset scheduler at : "+_cronComputeSunSetSunRiseScheduler);
		// Creates a Scheduler instance.
		_computeSunSetSunRiseScheduler = new Scheduler();	
		_computeSunSetSunRiseScheduler.setTimeZone(timeZone);
		_computeSunSetSunRiseScheduler.schedule(_cronComputeSunSetSunRiseScheduler, new Runnable() {
		public void run() {			
			ComputeSunsetSunrise();
		}
		});
		
		//Schedule that close rollershutter automatically at 7h40 every day of the week
		_logger.info("Starting automatic rollershutter closing scheduler at : "+_cronWeekCloseScheduler);
		_weekCloseScheduler = new Scheduler();		
		_weekCloseScheduler.setTimeZone(timeZone);
		_weekCloseScheduler.schedule(_cronWeekCloseScheduler, new Runnable() {
		public void run() {			
			CloseRollerShutters();
		}
		});
		
		//Schedule that open rollershutter automatically at 6h30 every day of the week
		_logger.info("Starting automatic rollershutter opening scheduler at : "+_cronWeekOpenScheduler);
		_weekOpenScheduler = new Scheduler();		
		_weekOpenScheduler.setTimeZone(timeZone);		
		_weekOpenScheduler.schedule(_cronWeekOpenScheduler, new Runnable() {
		public void run() {
			
			Calendar sunrise = Calendar.getInstance(); 			
			Calendar opendate = Calendar.getInstance(); //current time ...currently set to 6am30
			Calendar closedate = Calendar.getInstance(); //current time ...currently set to 7am47
			
			sunrise.set(Calendar.HOUR_OF_DAY, _sunrise.get(Calendar.HOUR_OF_DAY));
			sunrise.set(Calendar.MINUTE,_sunrise.get(Calendar.MINUTE));
			sunrise.set(Calendar.SECOND, 0);
			sunrise.set(Calendar.MILLISECOND, 0);				
						
			//sun already rised up
			if (sunrise.before(opendate)) {
				_logger.info("Sunrise : "+sunrise.getTime()+" is before : "+opendate.getTime()+ ". Try openning rollershutters...");
				OpenRollerShutters();
				return;
			}		
			
			String [] cron =  _cronWeekCloseScheduler.split(" ");
			int minutes = Integer.parseInt(cron[0]);
			int hours = Integer.parseInt(cron[1]);
			
			closedate.set(Calendar.HOUR_OF_DAY, hours);
			closedate.set(Calendar.MINUTE, minutes);
			closedate.set(Calendar.SECOND, 0);
			closedate.set(Calendar.MILLISECOND, 0);
			
			//Remove 10 minutes
			closedate.add(Calendar.MINUTE, -5);
			
			TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
			if (sunrise.before(closedate)) {
				_logger.info("Sunrise : "+sunrise.getTime()+" is before : "+closedate.getTime()+ " (planned close time minus 5 minutes). Gonna open roller shutter at : "+closedate.getTime());
				
				String morningOpenCron = sunrise.get(Calendar.MINUTE) + " " + sunrise.get(Calendar.HOUR_OF_DAY) + " * * 1,2,3,4,5";
				
				if (_weekmorningOpenSchedulerId == null) {
					
					_weekmorningOpenScheduler = new Scheduler();			
					_weekmorningOpenScheduler.setTimeZone(timeZone);
					
					_weekmorningOpenSchedulerId = _weekmorningOpenScheduler.schedule(morningOpenCron, new Runnable() {
						public void run() {
							OpenRollerShutters();
							_weekmorningOpenScheduler.deschedule(_weekmorningOpenSchedulerId);
						}});	
					_weekmorningOpenScheduler.start();
				} 
				else {
					_weekmorningOpenScheduler.setTimeZone(timeZone);
					_weekmorningOpenScheduler.reschedule(_weeknightCloseSchedulerId, morningOpenCron);	
				}
				
				return;
			}
		}
		});
				
		// Starts the scheduler.
		_computeSunSetSunRiseScheduler.start();
		_weekCloseScheduler.start();
		_weekOpenScheduler.start();

		while (!_isStopped) {

			try {
				
				// Sleep for 5 minutes
				Thread.sleep(300000);
				
				//Check state here and send message if not responding!!
				IsSensorAlive();
				
			} catch (Exception e) {
				_logger.error("Error occured in rollershutter management service", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in rollershutter management service, review error log for more details");
				_smsGammuService.sendMessage(sms, true);
			}
		}		
	}
	
	public String getWestRSState() {
		return _rsWest.getState().name();
	}
	
	public void CloseAllRollerShutters() {
		CloseRollerShutters();
	}
	
	private void CloseRollerShutters() {
		//Only if automatic management status is activated!
		if (_automaticManagementStatus) {
			if (_rsWest.getState() != RollerShutterState.CLOSED) {			
				if (!_rsWest.Close()) {
					_logger.info("Error occured when requesting close of west rollershutter");
					SMSDto sms = new SMSDto();
					sms.setMessage("Error occured when requesting close of west rollershutter");
					_smsGammuService.sendMessage(sms, true);
				}
				else {
					_logger.info("Close requested for west rollershutter");
				}
			}				
		}
		else {
			_logger.info("Close requested but automatic management is OFF, close canceled...");
		}
	}
	
	private void OpenRollerShutters() {
		//Only if automatic management status is activated!
		if (_automaticManagementStatus) {
		
			if (_rsWest.getState() != RollerShutterState.OPENED) {			
				if (!_rsWest.Open()) {
					_logger.info("Error occured when requesting open of west rollershutter");
					SMSDto sms = new SMSDto();
					sms.setMessage("Error occured when requesting open of west rollershutter");
					_smsGammuService.sendMessage(sms, true);
				}
				else {
					_logger.info("Open requested for west rollershutter");
				}
			}				
		}	
		else {
			_logger.info("Open requested but automatic management is OFF, Open canceled...");
		}
	}
	
	private void IsSensorAlive() {
		
		if (_rsWest.getState() == RollerShutterState.UNREACHABLE && !_alertSent5) {
			_alertSent5 = true;
			
			SMSDto sms = new SMSDto();
			String message = String.format("Sensor %s does not send messages anymore (5 minutes alert)", _rsWest.getName());
			sms.setMessage(message);
			_smsGammuService.sendMessage(sms, true);
			return;
		}
		
		if (_rsWest.getState() == RollerShutterState.UNREACHABLE && !_alertSent10) {
			_alertSent10 = true;
			
			SMSDto sms = new SMSDto();
			String message = String.format("Sensor %s does not send messages anymore (10 minutes alert)", _rsWest.getName());
			sms.setMessage(message);
			_smsGammuService.sendMessage(sms, true);
			return;
		}
		
		if (_rsWest.getState() == RollerShutterState.UNREACHABLE && !_alertSentMore) {
			_alertSentMore = true;
			
			SMSDto sms = new SMSDto();
			String message = String.format("Sensor %s does not send messages anymore (15 minutes alert)", _rsWest.getName());
			sms.setMessage(message);
			_smsGammuService.sendMessage(sms, true);
			return;
		}
		
		_alertSent5 = false; // Réinitialisation
		_alertSent10 = false; // Réinitialisation
		_alertSentMore = false; // Réinitialisation
		
	}
	
	public void StopService() {
		_computeSunSetSunRiseScheduler.stop();
		_weekCloseScheduler.stop();
		_weekOpenScheduler.stop();
		
		_logger.info("Stopping RollerShutter service...");

		_isStopped = true;
	}
	
	private void ComputeSunsetSunrise() {
						
		Location location = new Location("48.095428", "1.893597");
		SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, "Europe/Paris");

		_sunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());
		_sunrise = calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance());
				
		_logger.info("Computing sunrise/sunset...");
		_logger.info("Sunrise : " + _sunrise.getTime());
		_logger.info("Sunset : " + _sunset.getTime());
		
		
		_logger.info("Adjusting sunrise/sunset...");
		//Add 20 minutes to sunset (sky will begin de be dark)
		//Delete 20 minutes to sunrise (sky is already clear)
		_sunset.add(Calendar.MINUTE, 20);
		_sunrise.add(Calendar.MINUTE, -20);
		
		_logger.info("Adjusted Sunrise : " + _sunrise.getTime());
		_logger.info("Adjusted Sunset : " + _sunset.getTime());
		
		String cron = _sunset.get(Calendar.MINUTE) + " " + _sunset.get(Calendar.HOUR_OF_DAY) + " * * 1,2,3,4,5"; 
		
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		if (_weeknightCloseSchedulerId == null) {
			
			_weeknightCloseScheduler = new Scheduler();			
			_weeknightCloseScheduler.setTimeZone(timeZone);
			
			_weeknightCloseSchedulerId = _weeknightCloseScheduler.schedule(cron, new Runnable() {
				public void run() {
					CloseRollerShutters();
				}});
			_weeknightCloseScheduler.start();
			
		} else {			
			_weeknightCloseScheduler.setTimeZone(timeZone);
			_weeknightCloseScheduler.reschedule(_weeknightCloseSchedulerId, cron);	
		}
		
		_logger.info("Rescheduling rollershutter closing at night using : " + cron);
	}
}