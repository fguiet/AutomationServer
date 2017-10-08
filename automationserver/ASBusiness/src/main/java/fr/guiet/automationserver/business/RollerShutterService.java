package fr.guiet.automationserver.business;


import java.util.TimeZone;


import org.apache.log4j.Logger;


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
	
	//private Timer _timer = null;
	//private Calendar _sunset = null;
	//private Calendar _sunrise = null;
	private boolean _automaticManagementStatus = false; //By default
	private boolean _isStopped = false; // Service arrete?
	private SMSGammuService _smsGammuService = null;
	//private Scheduler _rollerShutterScheduler = null;
	
	//private String _cronNightWeekClose = null;	
	
	//private String _cronComputeSunSetSunRise = null;	
	//private String _cronMorningWeekClose = null;	
	//private String _cronMorningWeekOpen = null;	
	
	//private String _weekWestNightCloseId = null;
	//private String _weekNorthNightCloseId = null;
	
	//private String _weekMorningOpenId = null;
		
	private RollerShutter _rsWest = null;
	private RollerShutter _rsNorth = null;
		
	private int ROLLERSHUTTER_WEST_ID = 7;
	private int ROLLERSHUTTER_NORTH_ID = 8;
		
	public RollerShutterService(SMSGammuService smsGammuService) {
				
		/*InputStream is = null;
		try {

			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);
			
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
		}*/
		
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
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		
		/*_logger.info("Starting compute sunrise/sunset scheduler at : "+_cronComputeSunSetSunRise);
		// Creates a Scheduler instance.
		_rollerShutterScheduler = new Scheduler();	
		_rollerShutterScheduler.setTimeZone(timeZone);
		_rollerShutterScheduler.schedule(_cronComputeSunSetSunRise, new Runnable() {
		public void run() {			
			ComputeSunsetSunrise();
		}
		});*/
				
		//Schedule that close rollershutter automatically at 7h49 every day of the week
		/*_logger.info("Starting automatic rollershutter closing scheduler at : "+_cronMorningWeekClose);

		_rollerShutterScheduler.schedule(_cronMorningWeekClose, new Runnable() {
		public void run() {						
			CloseRollerShutters(true, true);
			ComputeNextWeekMorningCloseDate();
		}
		});*/
		
		//Initial lauch
		//ComputeSunsetSunrise();
		
		// Starts the scheduler.
		//_rollerShutterScheduler.start();		

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
			_rsWest.setState(state);
		
		if (rsId == ROLLERSHUTTER_NORTH_ID)
			_rsNorth.setState(state);
			
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
	
	/*private void ComputeWeekMorningOpenScheduler() {
		
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
			
			_weekMorningOpenId = _rollerShutterScheduler.schedule(newCron, new Runnable() {
				public void run() {
					
				}});	
		} 
		else {			
			_rollerShutterScheduler.reschedule(_weekMorningOpenId, newCron);			
		}
			
		ComputeWeekMorningOpenDate(newCron);
	}*/
	
	
	
	
	
	
	/*private void ComputeWeekNightCloseScheduler() {
		
		String cronNorthRS = _sunset.get(Calendar.MINUTE) + " " + _sunset.get(Calendar.HOUR_OF_DAY) + GetCronWithoutTime(_cronNightWeekClose); 
		
		//
		if (_weekNorthNightCloseId == null) {
			
			_weekNorthNightCloseId = _rollerShutterScheduler.schedule(cronNorthRS, new Runnable() {
				public void run() {
					CloseRollerShutters(true, false);
					ComputeWeekNightCloseDate(cronNorthRS);
				}});			
			
		} else {						
			_rollerShutterScheduler.reschedule(_weekNorthNightCloseId, cronNorthRS);	
		}
		
		ComputeWeekNightCloseDate(cronNorthRS);
		_logger.info("Rescheduling North rollershutter closing at night using : " + cronNorthRS);
		
		//10 minutes later
		Calendar newSunset= (Calendar) _sunset.clone();
		newSunset.add(Calendar.MINUTE, -10);
		
		String cronWestRS = newSunset.get(Calendar.MINUTE) + " " + newSunset.get(Calendar.HOUR_OF_DAY) + GetCronWithoutTime(_cronNightWeekClose); 
		if (_weekWestNightCloseId == null) {
			
			_weekWestNightCloseId = _rollerShutterScheduler.schedule(cronWestRS, new Runnable() {
				public void run() {
					CloseRollerShutters(false, true);					
				}});			
			
		} else {			
			_rollerShutterScheduler.reschedule(_weekWestNightCloseId, cronWestRS);				
		}		
		
		_logger.info("Rescheduling West rollershutter closing at night using : " + cronWestRS);
	}*/
	
	//Launched every day at 8h in the morning
	/*private void ComputeSunsetSunrise() {
						
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
	}*/
}
