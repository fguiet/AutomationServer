package fr.guiet.automationserver.business;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import fr.guiet.automationserver.dto.SMSDto;
import it.sauronsoftware.cron4j.Predictor;
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
    
	//I think only one scheduler can be used instead of many...lets see if it works...
	
	// Logger
	private static Logger _logger = Logger.getLogger(RollerShutterService.class);
	
	
	private Timer _timer = null;
	private Calendar _sunset = null;
	private Calendar _sunrise = null;
	private boolean _automaticManagementStatus = false; //By default
	private boolean _isStopped = false; // Service arrete?
	private SMSGammuService _smsGammuService = null;
	private Scheduler _rollerShutterScheduler = null;
	
	private String _cronNightWeekClose = null;
	
	//private Scheduler _computeSunSetSunRiseScheduler = null;
	private String _cronComputeSunSetSunRise = null;
	//private Scheduler _weekCloseScheduler = null;
	private String _cronMorningWeekClose = null;
	//private Scheduler _weekOpenScheduler = null;
	private String _cronMorningWeekOpen = null;
	//private Scheduler _weeknightCloseScheduler = null;
	private String _weekNightCloseId = null;
	
	//private Scheduler _weekmorningOpenScheduler = null;
	private String _weekMorningOpenId = null;
	
	private String _apikey = null;
	private String _baseUrlRs1 = null;
	private String _baseUrlRs2 = null;
	private RollerShutter _rsWest = null;
	private RollerShutter _rsNorth = null;
	//private boolean _alertSent5 = false;
	//private boolean _alertSent10 = false;
	//private boolean _alertSentMore = false;
	
	//Lie à Sunset
	private String _nextWeekNightCloseDate = "NA";
	//a 7h79
	private String _nextWeekMorningCloseDate = "NA";
	//lié à sunrise
	private String _nextWeekMorningOpenDate = "NA";
	
	public String getNextWeekNightCloseDate() {
		if (_automaticManagementStatus)
			return _nextWeekNightCloseDate;
		else 
			return "mgt auto. désactivé";
	}
	
	public String getNextWeekMorningCloseDate() {
		if (_automaticManagementStatus)
			return _nextWeekMorningCloseDate;
		else 
			return "mgt auto. désactivé";
	}
	
	public String getNextWeekMorningOpenDate() {
		if (_automaticManagementStatus)
			return _nextWeekMorningOpenDate;
		else 
			return "mgt auto. désactivé";
	}
	
	public RollerShutterService(SMSGammuService smsGammuService) {
				
		InputStream is = null;
		try {

			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);
			
			String apikey = prop.getProperty("api.key");
			if (apikey != null)
				_apikey = apikey;
			else
				_apikey = "xxx";
							
			String baseUrlRs1 = prop.getProperty("rollershutter.west.url");
			if (baseUrlRs1 != null) {
				_baseUrlRs1 = baseUrlRs1;
			}
			else {
				_baseUrlRs1 = "http://127.0.0.1";
			}
			
			String baseUrlRs2 = prop.getProperty("rollershutter.north.url");
			if (baseUrlRs2 != null) {
				_baseUrlRs2 = baseUrlRs2;
			}
			else {
				_baseUrlRs2 = "http://127.0.0.1";
			}
			
			String cronComputeSunSetSunRise = prop.getProperty("rollershutter.schedule.sunsetsunrise");
			if (cronComputeSunSetSunRise != null) {
				_cronComputeSunSetSunRise = cronComputeSunSetSunRise;
			}
			else {
				_cronComputeSunSetSunRise = "0 5 * * *";
			}
			
			String cronMorningWeekClose = prop.getProperty("rollershutter.schedule.weekmorningclose");
			if (cronMorningWeekClose != null) {
				_cronMorningWeekClose = cronMorningWeekClose;
			}
			else {
				_cronMorningWeekClose = "47 7 * * 1,2,3,4,5";
			}
			
			String cronMorningWeekOpen = prop.getProperty("rollershutter.schedule.weekmorningopen");
			if (cronMorningWeekOpen != null) {
				_cronMorningWeekOpen = cronMorningWeekOpen;
			}
			else {
				_cronMorningWeekOpen = "30 6 * * 1,2,3,4,5";
			}
			
			String cronNightWeekClose = prop.getProperty("rollershutter.schedule.weeknightclose");
			if (cronNightWeekClose != null) {
				_cronNightWeekClose = cronNightWeekClose;
			}
			else {
				_cronNightWeekClose = "30 20 * * 0,1,2,3,4";
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
	
		//CRON pattern
		//minute hour day of month (1-31) month (1-12) day of week (0 sunday, 6 saturday)
		
		//Get Europe/Paris TimeZone
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		
		_logger.info("Starting compute sunrise/sunset scheduler at : "+_cronComputeSunSetSunRise);
		// Creates a Scheduler instance.
		_rollerShutterScheduler = new Scheduler();	
		_rollerShutterScheduler.setTimeZone(timeZone);
		_rollerShutterScheduler.schedule(_cronComputeSunSetSunRise, new Runnable() {
		public void run() {			
			ComputeSunsetSunrise();
		}
		});
				
		
		//Schedule that close rollershutter automatically at 7h49 every day of the week
		_logger.info("Starting automatic rollershutter closing scheduler at : "+_cronMorningWeekClose);
		//_weekCloseScheduler = new Scheduler();		
		//_weekCloseScheduler.setTimeZone(timeZone);
		_rollerShutterScheduler.schedule(_cronMorningWeekClose, new Runnable() {
		public void run() {						
			CloseRollerShutters();
			ComputeNextWeekMorningCloseDate();
		}
		});
		
		
		
		//Schedule that open rollershutter automatically at 6h30 every day of the week
		//_logger.info("Starting automatic rollershutter opening scheduler at : "+_cronMorningWeekOpen);
		//_weekOpenScheduler = new Scheduler();		
		//_weekOpenScheduler.setTimeZone(timeZone);		
		//_rollerShutterScheduler.schedule(_cronMorningWeekOpen, new Runnable() {
		//public void run() {
			
				
		//}
		//});
		
		CreateCheckForIntrudersTask();
		
		//Initial lauch
		ComputeSunsetSunrise();
		//
		//ComputeNextWeekMorningCloseDate();
		
		// Starts the scheduler.
		_rollerShutterScheduler.start();
		//_weekCloseScheduler.start();
		//_weekOpenScheduler.start();

		while (!_isStopped) {

			try {
												
				// Sleep for 5 minutes
				Thread.sleep(300000);
				
				//Check state here and send message if not responding!!
				IsSensorAlive(_rsWest);
				IsSensorAlive(_rsNorth);
				
			} catch (Exception e) {
				_logger.error("Error occured in rollershutter management service", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in rollershutter management service, review error log for more details");
				_smsGammuService.sendMessage(sms, true);
			}
		}		
	}
	
	
	private void CreateCheckForIntrudersTask() {
		
		_logger.info("Creating CheckForIntrudersTask");

		TimerTask intrudersTask = new TimerTask() {
			@Override
			public void run() {

				CheckForIntruders();
			}
		};

		_timer = new Timer(true);
		//Every 30s
		_timer.schedule(intrudersTask, 5000, 30000);

	}
	
	public String getNorthRSState() {
		return _rsNorth.getState().name();
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
			
			if (_rsNorth.getState() != RollerShutterState.CLOSED) {			
				if (!_rsNorth.Close()) {
					_logger.info("Error occured when requesting close of north rollershutter");
					SMSDto sms = new SMSDto();
					sms.setMessage("Error occured when requesting close of north rollershutter");
					_smsGammuService.sendMessage(sms, true);
				}
				else {
					_logger.info("Close requested for north rollershutter");
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
			
			if (_rsNorth.getState() != RollerShutterState.OPENED) {			
				if (!_rsNorth.Open()) {
					_logger.info("Error occured when requesting open of north rollershutter");
					SMSDto sms = new SMSDto();
					sms.setMessage("Error occured when requesting open of north rollershutter");
					_smsGammuService.sendMessage(sms, true);
				}
				else {
					_logger.info("Open requested for north rollershutter");
				}
			}
		}	
		else {
			_logger.info("Open requested but automatic management is OFF, Open canceled...");
		}
	}
	
	private void CheckForIntruders() {
		
		RollerShutterState previousState = _rsWest.getPreviousState();
		RollerShutterState currentState = _rsWest.getState();		
		
		if (currentState != previousState) {
			_logger.info("West Rollershutter passed from : "+previousState.name()+" to "+currentState.name());

			boolean isTimeBetween = false;
			try {
				isTimeBetween = DateUtils.isTimeBetweenTwoTime("21:00:00","06:00:00",DateUtils.getTimeFromCurrentDate());
			}
			catch (ParseException pe) {
				_logger.error("Erreur lors du parsing de la date", pe);
			}
			
			if (isTimeBetween) {
				//If state change that way...it is strange...somebody try to enter home??
				//better send a sms....
				if (previousState == RollerShutterState.CLOSED && (currentState == RollerShutterState.UNREACHABLE ||
																   currentState == RollerShutterState.OPENED || 
																   currentState == RollerShutterState.UNDETERMINED)) {
					SMSDto sms = new SMSDto();
					sms.setMessage("Le volet roulant West est passé de l'état : "+previousState.name()+ " à l'état : "+currentState.name()+ " durant la période 21:00:00 - 06:00:00. Bizarre non?");
					_smsGammuService.sendMessage(sms, true);
				}
			}
		}
		
		previousState = _rsNorth.getPreviousState();
		currentState = _rsNorth.getState();		
		
		if (currentState != previousState) {
			_logger.info("North Rollershutter passed from : "+previousState.name()+" to "+currentState.name());

			boolean isTimeBetween = false;
			try {
				isTimeBetween = DateUtils.isTimeBetweenTwoTime("21:00:00","06:00:00",DateUtils.getTimeFromCurrentDate());
			}
			catch (ParseException pe) {
				_logger.error("Erreur lors du parsing de la date", pe);
			}
			
			if (isTimeBetween) {
				//If state change that way...it is strange...somebody try to enter home??
				//better send a sms....
				if (previousState == RollerShutterState.CLOSED && (currentState == RollerShutterState.UNREACHABLE ||
																   currentState == RollerShutterState.OPENED || 
																   currentState == RollerShutterState.UNDETERMINED)) {
					SMSDto sms = new SMSDto();
					sms.setMessage("Le volet roulant Nord est passé de l'état : "+previousState.name()+ " à l'état : "+currentState.name()+ " durant la période 21:00:00 - 06:00:00. Bizarre non?");
					_smsGammuService.sendMessage(sms, true);
				}
			}
		}
	}
	
	private void IsSensorAlive(RollerShutter rs) {
		
		RollerShutterState currentState = rs.getState();
		
		if (currentState == RollerShutterState.UNREACHABLE && !rs._notReachable5) {
			rs._notReachable5 = true;
			
			SMSDto sms = new SMSDto();
			String message = String.format("Sensor %s does not send messages anymore (5 minutes alert)", _rsWest.getName());
			sms.setMessage(message);
			_smsGammuService.sendMessage(sms, true);
			return;
		}
		
		if (currentState == RollerShutterState.UNREACHABLE && !rs._notReachable5) {
			rs._notReachable10 = true;
			
			SMSDto sms = new SMSDto();
			String message = String.format("Sensor %s does not send messages anymore (10 minutes alert)", _rsWest.getName());
			sms.setMessage(message);
			_smsGammuService.sendMessage(sms, true);
			return;
		}
		
		if (currentState == RollerShutterState.UNREACHABLE && !rs._notReachable5) {
			rs._notReachable15 = true;
			
			SMSDto sms = new SMSDto();
			String message = String.format("Sensor %s does not send messages anymore (15 minutes alert)", _rsWest.getName());
			sms.setMessage(message);
			_smsGammuService.sendMessage(sms, true);
			return;
		}
		
		rs._notReachable5 = false;
		rs._notReachable10 = false;
		rs._notReachable15 = false;		
	}
	
	public void StopService() {
		
		if (_timer != null)
			_timer.cancel();
		
		_rollerShutterScheduler.stop();
		
		_logger.info("Stopping RollerShutter service...");

		_isStopped = true;
	}
	
	private void ComputeWeekMorningOpenScheduler() {
		
		Calendar sunrise = Calendar.getInstance(); 			
		Calendar opendate = Calendar.getInstance(); //current time ...currently set to 6am30
		Calendar closedate = Calendar.getInstance(); //current time ...currently set to 7am47
		
		String [] cron =  _cronMorningWeekOpen.split(" ");
		int minutes = Integer.parseInt(cron[0]);
		int hours = Integer.parseInt(cron[1]);
		
		opendate.set(Calendar.HOUR_OF_DAY, hours);
		opendate.set(Calendar.MINUTE, minutes);
		opendate.set(Calendar.SECOND, 0);
		opendate.set(Calendar.MILLISECOND, 0);		
		
		sunrise.set(Calendar.HOUR_OF_DAY, _sunrise.get(Calendar.HOUR_OF_DAY));
		sunrise.set(Calendar.MINUTE,_sunrise.get(Calendar.MINUTE));
		sunrise.set(Calendar.SECOND, 0);
		sunrise.set(Calendar.MILLISECOND, 0);			
			
		String newCron = "";
		
		//sun already rised up
		if (sunrise.before(opendate)) {
			_logger.info("Sunrise : "+sunrise.getTime()+" is before : "+opendate.getTime()+ ". Will ope ruller shutter at :"+opendate.getTime());
			//OpenRollerShutters();
			newCron = minutes + " " + hours + GetCronWithoutTime(_cronMorningWeekOpen); //" * * 1,2,3,4,5 ";			
		}		
		else {
			cron = _cronMorningWeekClose.split(" ");
			minutes = Integer.parseInt(cron[0]);
			hours = Integer.parseInt(cron[1]);
			
			closedate.set(Calendar.HOUR_OF_DAY, hours);
			closedate.set(Calendar.MINUTE, minutes);
			closedate.set(Calendar.SECOND, 0);
			closedate.set(Calendar.MILLISECOND, 0);
			
			//Remove 10 minutes
			closedate.add(Calendar.MINUTE, -5);
			
		//	TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
			if (sunrise.before(closedate)) {
				_logger.info("Sunrise : "+sunrise.getTime()+" is before : "+closedate.getTime()+ " (planned close time minus 5 minutes). Gonna open roller shutter at : "+sunrise.getTime());				
				newCron = sunrise.get(Calendar.MINUTE) + " " + sunrise.get(Calendar.HOUR_OF_DAY) + GetCronWithoutTime(_cronMorningWeekOpen); //" * * 1,2,3,4,5";
			}		
		}
		
		if (_weekMorningOpenId == null) {
			
			//_weekmorningOpenScheduler = new Scheduler();			
			//_weekmorningOpenScheduler.setTimeZone(timeZone);
			
			_weekMorningOpenId = _rollerShutterScheduler.schedule(newCron, new Runnable() {
				public void run() {
					OpenRollerShutters();
					_rollerShutterScheduler.deschedule(_weekMorningOpenId);
				}});	
			//_rollerShutterScheduler.start();
		} 
		else {
			//_weekmorningOpenScheduler.setTimeZone(timeZone);
			_rollerShutterScheduler.reschedule(_weekMorningOpenId, newCron);			
		}
			
		ComputeWeekMorningOpenDate(newCron);
	}
	
	private void ComputeWeekMorningOpenDate(String cron) {
		
		if (!cron.equals("")) {		
			Predictor predictor = new Predictor(cron);
			TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
			predictor.setTimeZone(timeZone);		
			_nextWeekMorningOpenDate = DateUtils.getDateToString(predictor.nextMatchingDate());
		}
		else {
			_nextWeekMorningOpenDate = "pas d'ouverture prévue";
		}
	}
	
	private void ComputeNextWeekMorningCloseDate() {
		
		Predictor predictor = new Predictor(_cronMorningWeekClose);
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		predictor.setTimeZone(timeZone);		
		_nextWeekMorningCloseDate = DateUtils.getDateToString(predictor.nextMatchingDate());
	}
	
	
	private void ComputeWeekNightCloseDate(String cron) {
		
		Predictor predictor = new Predictor(cron);
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		predictor.setTimeZone(timeZone);		
		_nextWeekNightCloseDate = DateUtils.getDateToString(predictor.nextMatchingDate());
	}
	
	
	private String GetCronWithoutTime(String cron) {
		
		String [] cronArray =  cron.split(" ");
				
		return " " + cronArray[2] + " " + cronArray[3] + " " + cronArray[4];
		
	}
	
	private void ComputeWeekNightCloseScheduler() {
		
		String cron = _sunset.get(Calendar.MINUTE) + " " + _sunset.get(Calendar.HOUR_OF_DAY) + GetCronWithoutTime(_cronNightWeekClose); 
		
		//
		if (_weekNightCloseId == null) {
			
			//_weeknightCloseScheduler = new Scheduler();			
			//_weeknightCloseScheduler.setTimeZone(timeZone);
			
			_weekNightCloseId = _rollerShutterScheduler.schedule(cron, new Runnable() {
				public void run() {
					CloseRollerShutters();
					ComputeWeekNightCloseDate(cron);
				}});
			//_weeknightCloseScheduler.start();
			
		} else {			
			//_weeknightCloseScheduler.setTimeZone(timeZone);
			_rollerShutterScheduler.reschedule(_weekNightCloseId, cron);	
		}
		
		ComputeWeekNightCloseDate(cron);
		_logger.info("Rescheduling rollershutter closing at night using : " + cron);
	}
	
	//Launched every day at 8h in the morning
	private void ComputeSunsetSunrise() {
						
		Location location = new Location("48.095428", "1.893597");
		SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, "Europe/Paris");

		_sunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());
		_sunrise = calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance());
				
		_logger.info("Computing sunrise/sunset...");
		_logger.info("Sunrise : " + _sunrise.getTime());
		_logger.info("Sunset : " + _sunset.getTime());
		
		
		_logger.info("Adjusting sunrise/sunset...");
		//Add 10 minutes to sunset (sky will begin de be dark)
		//Delete 20 minutes to sunrise (sky is already clear)
		_sunset.add(Calendar.MINUTE, 10);
		_sunrise.add(Calendar.MINUTE, -20);
		
		_logger.info("Adjusted Sunrise : " + _sunrise.getTime());
		_logger.info("Adjusted Sunset : " + _sunset.getTime());
				
		ComputeWeekNightCloseScheduler();		
		ComputeWeekMorningOpenScheduler();
		ComputeNextWeekMorningCloseDate();
	}
}