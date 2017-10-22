package fr.guiet.automationserver.business;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import it.sauronsoftware.cron4j.Predictor;
import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.SchedulingPattern;

public class ScenariiManager {

	// Logger
	private static Logger _logger = Logger.getLogger(ScenariiManager.class);
		
	private Scheduler _scenariiScheduler = null;
	private Scheduler _sundayScheduler = null;
	private Scheduler _mondayScheduler = null;
	private Scheduler _tuesdayScheduler = null;
	private Scheduler _wesnesdayScheduler = null;
	private Scheduler _thursdayScheduler = null;
	private Scheduler _fridayScheduler = null;
	private Scheduler _saturdayScheduler = null;
	
	private HomeModeState _homeModeState = HomeModeState.NOTACTIVED;
	
	private Calendar _sunset = null;
	private Calendar _sunrise = null;
	
	private RollerShutterService _rollerShutterService = null;
	private AlarmService _alarmService = null;	
	
	private String _nextRSOpenDate = "NA";
	private String _nextRSCloseDate = "NA";
	
	private LinkedList<String> _rsOpenScheduleIdList = null;
	private LinkedList<String> _rsCloseScheduleIdList = null;
	
	public void SetHomeModeState(String homeMode) {
		
		switch(homeMode) {
		case "0":
			_logger.info("Setting Home mode : NOT ACTIVATED");
			_homeModeState = HomeModeState.NOTACTIVED;
			break;
		case "1":
			_logger.info("Setting Home mode : HOLIDAY");
			_homeModeState = HomeModeState.HOLIDAY;
			break;
		case "2":
			_logger.info("Setting Home mode : WORK");
			_homeModeState = HomeModeState.WORK;
			break;
		default:
			//Default
			_logger.info("Received unknown home mode : "+homeMode+", Setting default Home mode : NOT ACTIVATED");
			_homeModeState = HomeModeState.NOTACTIVED;
			break;
		}
			
	}
	
	public String GetHomeModeStatus() {
		if (_homeModeState == HomeModeState.NOTACTIVED) {
			return "0";
		}
		
		if (_homeModeState == HomeModeState.HOLIDAY) {
			return "1";
		}
		
		if (_homeModeState == HomeModeState.WORK) {
			return "2";
		}
		
		return "NOTACTIVATED";
	}
	
	

	public ScenariiManager(RollerShutterService rollerShutterService,
							AlarmService alarmService) {		
				
		_logger.info("Starting scenarii manager...");
		
		_rollerShutterService = rollerShutterService;
		_alarmService = alarmService;
		
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		_scenariiScheduler = new Scheduler();
		_scenariiScheduler.setTimeZone(timeZone);
		
		// 0 = Sunday
		_scenariiScheduler.schedule("05 00 * * 0,1,2,3,4,5,6", new Runnable() {
		public void run() {	
			StartCurrentDayScenario();					
		}
		});
		
		//_logger.info("Starting scenarii manager...");
		_scenariiScheduler.start();
		
		//Start current day
		StartCurrentDayScenario();
	}
	
	public String getNextRSOpenDate() {
		if (_rollerShutterService.GetAutomaticManagementStatus().equals("ON"))
			return _nextRSOpenDate;
		else 
			return "mgt auto. désactivé";
	}

	public String getNextRSCloseDate() {
		if (_rollerShutterService.GetAutomaticManagementStatus().equals("ON"))
			return _nextRSCloseDate;
		else 
			return "mgt auto. désactivé";
	}
	
	public void StopScenariiManager() {
		
		_logger.info("Stopping scenarri manager...");
		StopAllSchedulers();
		_scenariiScheduler.stop();
	}
	
	private void StopAllSchedulers() {
		
		if (_sundayScheduler !=null) {
			_sundayScheduler.stop();
			_sundayScheduler = null;
		}
		
		if (_mondayScheduler !=null) {
			_mondayScheduler.stop();
			_mondayScheduler = null;
		}
		
		if (_tuesdayScheduler !=null) {
			_tuesdayScheduler.stop();
			_tuesdayScheduler = null;
		}
		
		if (_wesnesdayScheduler !=null) {
			_wesnesdayScheduler.stop();
			_wesnesdayScheduler = null;
		}
		
		if (_thursdayScheduler !=null) {
			_thursdayScheduler.stop();
			_thursdayScheduler = null;
		}
		
		if (_fridayScheduler !=null) {
			_fridayScheduler.stop();
			_fridayScheduler = null;
		}
		
		if (_saturdayScheduler !=null) {
			_saturdayScheduler.stop();
			_saturdayScheduler = null;		
		}
	}
	
	private Scheduler ReturnSchedulerByDayOfWeek() {
		
		Calendar cal = Calendar.getInstance();
		int day = cal.get(Calendar.DAY_OF_WEEK); 

		switch (day) {
		    case Calendar.SUNDAY:
		    	return _sundayScheduler;		    	
		    	//break;

		    case Calendar.MONDAY:
		    	return _mondayScheduler;
		    	//break;

		    case Calendar.TUESDAY:
		    	return _tuesdayScheduler;
		    	//break;
		    
		    case Calendar.WEDNESDAY:
		    	return _wesnesdayScheduler;		    		 
		    	//break;
		    
		    case Calendar.THURSDAY:
		    	return _thursdayScheduler;	
		    	//break;
		    
		    case Calendar.FRIDAY:
		    	return _fridayScheduler;
		    	//break;
		    
		    case Calendar.SATURDAY:
		    	return _saturdayScheduler;
		    	//break;		    
		}	
		
		return null;
	}
	
	private void CreateScheduler(Scheduler scheduler, int dayOfWeek) {
		
		String RSScheduleOpen = "";
		String RSScheduleClose = "";
		String AlarmOpen = "";
		String AlarmClose = "";
		
		
		InputStream is = null;
		try {

			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);
			
			RSScheduleOpen = prop.getProperty("rollershutter.schedule.open");
			if (RSScheduleOpen == null) 
			{
				_logger.info(
						"Impossible de créer le scheduler...");
				return;
			}
			
			RSScheduleClose = prop.getProperty("rollershutter.schedule.close");
			if (RSScheduleClose == null) 
			{
				_logger.info(
						"Impossible de créer le scheduler...");
				return;
			}
			
			AlarmOpen = prop.getProperty("alarm.schedule.on");
			if (AlarmOpen == null) 
			{
				_logger.info(
						"Impossible de créer le scheduler...");
				return;
			}
			
			AlarmClose = prop.getProperty("alarm.schedule.off");
			if (AlarmClose == null) 
			{
				_logger.info(
						"Impossible de créer le scheduler...");
				return;
			}
			
		} catch (FileNotFoundException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
			return;
		} catch (IOException e) {
			_logger.error(
					"Erreur lors de la lecture du fichier de configuration classpath_folder/config/automationserver.properties",
					e);
			return;
		}
		
		if (_homeModeState == HomeModeState.WORK) {
			
			_logger.info("Activating Home Mode Management : WORK");
			
			String[] RSDaysOpen = RSScheduleOpen.split("/");
			String[] RSDaysClose = RSScheduleClose.split("/");
			String[] AlarmDaysOpen = AlarmOpen.split("/");
			String[] AlarmDaysClose = AlarmClose.split("/");
			
			_logger.info("Using RS Conf Open Schedule : "+RSDaysOpen[dayOfWeek]);
			_logger.info("Using RS Conf Close Schedule : "+RSDaysClose[dayOfWeek]);
			_logger.info("Using Alarm Conf On Schedule : "+AlarmDaysOpen[dayOfWeek]);
			_logger.info("Using Alarm Conf Off Schedule : "+AlarmDaysClose[dayOfWeek]);
			
			String[] RSConfDayOpen = RSDaysOpen[dayOfWeek].split(";");
			String[] RSConfDayClose = RSDaysClose[dayOfWeek].split(";");
			String[] AlarmConfDayOpen = AlarmDaysOpen[dayOfWeek].split(";");
			String[] AlarmConfDayClose = AlarmDaysClose[dayOfWeek].split(";");
			
			
			CreateRSDayScheduler(RSConfDayOpen, scheduler, dayOfWeek, true);
			CreateRSDayScheduler(RSConfDayClose, scheduler, dayOfWeek, false);
			CreateAlarmDayScheduler(AlarmConfDayOpen, scheduler, dayOfWeek, true);
			CreateAlarmDayScheduler(AlarmConfDayClose, scheduler, dayOfWeek, false);
		}
	}
	
	private void CreateAlarmDayScheduler(String[] schedule, Scheduler scheduler, int dayOfWeek, boolean isOn) {
		
		int cpt = 1;
		String cron = "";
		while (cpt != schedule.length) {
			
			//Normal
			if (schedule[cpt].equals("N")) {
				cpt++;
				int h1 = Integer.parseInt(schedule[cpt]);
				cpt++;
				int m1 = Integer.parseInt(schedule[cpt]);
				cpt++;
				
				cron = CreateStandardCron(h1,m1, dayOfWeek);				
				
				if (isOn) {
					_logger.info("Add Alarm ON cron : "+cron);
					AddAlarmOnSchedule(scheduler, cron);
				}
				else {
					_logger.info("Add Alarm OFF cron :"+cron);
					AddAlarmOffSchedule(scheduler, cron);
				}
				continue;
			}
		}
		
	}
	
	private void CreateRSDayScheduler(String[] schedule, Scheduler scheduler, int dayOfWeek, boolean isOpen) {
		
		int cpt = 1;
		String cron = "";
		while (cpt != schedule.length) {
		
			//Sunrise
			if (schedule[cpt].equals("SR")) {
				cpt++;
				int h1 = Integer.parseInt(schedule[cpt]);
				cpt++;
				int m1 = Integer.parseInt(schedule[cpt]);
				cpt++;
				int h2 = Integer.parseInt(schedule[cpt]);
				cpt++;
				int m2 = Integer.parseInt(schedule[cpt]);
				cpt++;
				
				cron = CreateCronRSMorningOpen(h1,m1,h2,m2, dayOfWeek);
				
				if (cron == null)
					continue;
				
				if (isOpen) {
					_logger.info("Add Sunrise RS Open cron :"+cron);
					AddRSOpenSchedule(scheduler, cron);
				}
				else {
					_logger.info("Add Sunrise RS Close cron :"+cron);
					AddRSCloseSchedule(scheduler, cron);
				}
				continue;
			}
			
			//Sunset
			if (schedule[cpt].equals("SS")) {
								
				cpt++;
				cron = CreateStandardCron(_sunset.get(Calendar.HOUR_OF_DAY),_sunset.get(Calendar.MINUTE),dayOfWeek);
				
				if (isOpen) {
					_logger.info("Add Sunset RS Open cron :"+cron);
					AddRSOpenSchedule(scheduler, cron);
				}
				else {
					_logger.info("Add Sunset RS Close cron :"+cron);
					AddRSCloseSchedule(scheduler, cron);
				}
				continue;
			}
			
			//Normal
			if (schedule[cpt].equals("N")) {
				cpt++;
				int h1 = Integer.parseInt(schedule[cpt]);
				cpt++;
				int m1 = Integer.parseInt(schedule[cpt]);
				cpt++;
				
				cron = CreateStandardCron(h1,m1, dayOfWeek);
				
				if (isOpen) {
					_logger.info("Add Normla RS Open cron :"+cron);
					AddRSOpenSchedule(scheduler, cron);
				}
				else {
					_logger.info("Add Normal RS Close cron :"+cron);
					AddRSCloseSchedule(scheduler, cron);
				}
				continue;
			}
		}
		
	}
	
	private void StartCurrentDayScenario() {
		
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		
		//Init SchedulerId List
		_rsOpenScheduleIdList = new LinkedList<String>();
		_rsCloseScheduleIdList = new LinkedList<String>();
		
		//Every day compute sunset/sunrise
		ComputeSunsetSunrise();
		
		//Stop all schedulers first
		StopAllSchedulers();
		
		//Start/Stop day to day scheduler
		Calendar cal = Calendar.getInstance();
		int day = cal.get(Calendar.DAY_OF_WEEK); 

		switch (day) {
		    case Calendar.SUNDAY:
		    	_sundayScheduler = new Scheduler();
		    	_sundayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Sunday scenarii...");
		    	
		    	CreateScheduler(_sundayScheduler, 0);
		    	break;

		    case Calendar.MONDAY:
		    	_mondayScheduler = new Scheduler();
				_mondayScheduler.setTimeZone(timeZone);
		    	
				_logger.info("Creating Monday scenarii...");
				CreateScheduler(_mondayScheduler, 1);
		    	
		    	break;

		    case Calendar.TUESDAY:
		    	_tuesdayScheduler = new Scheduler();
		    	_tuesdayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Tuesday scenarii...");
		    	CreateScheduler(_tuesdayScheduler, 2);
		    	
		    	break;
		    
		    case Calendar.WEDNESDAY:
		    	_wesnesdayScheduler = new Scheduler();
		    	_wesnesdayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Wednesday scenarii...");
		    	CreateScheduler(_wesnesdayScheduler, 3);		    		 
		    	break;
		    
		    case Calendar.THURSDAY:
		    	_thursdayScheduler = new Scheduler();
		    	_thursdayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Thursday scenarii...");
		    	CreateScheduler(_thursdayScheduler, 4);
		    	
		    	break;
		    
		    case Calendar.FRIDAY:
		    	_fridayScheduler = new Scheduler();
		    	_fridayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Friday scenarii...");
		    	CreateScheduler(_fridayScheduler, 5);
		    	
		    	break;
		    
		    case Calendar.SATURDAY:
		    	_saturdayScheduler = new Scheduler();
		    	_saturdayScheduler.setTimeZone(timeZone);
		    		    			    	
		    	_logger.info("Creating Saturday scenarii...");
		    	CreateScheduler(_saturdayScheduler, 6);
		    	break;		    
		}
		
		ReturnSchedulerByDayOfWeek().start();
		ComputeNextRSOpen();
		ComputeNextRSClose();
	}
	
	/*private void CreateStandardDayScenarii(Scheduler scheduler, int dayOfWeek) {
		
		String cron;		
				
		//Elo & Fred at work
		if (!_eloAtHome && !_fredAtHome) {
			
			//*************
			//RollerShutter Management
			//*************
						
			//Compute Open in the morning at 6h30 synchronized with sunset/sunrise
			cron = CreateCronRSMorningOpen(6,30,7,40,dayOfWeek);
			AddRSOpenSchedule(scheduler, cron);			
			
			//Close at 7am49
			cron = CreateStandardCron(7,49,dayOfWeek);
			AddRSCloseSchedule(scheduler, cron);
										
			//Close at sunset
			cron = CreateStandardCron(_sunset.get(Calendar.HOUR_OF_DAY),_sunset.get(Calendar.MINUTE),dayOfWeek); 
			AddRSCloseSchedule(scheduler, cron);
			
			//*************
			//Alarm Management
			//*************
			cron = CreateStandardCron(6,00,dayOfWeek);
			AddAlarmOffSchedule(scheduler, cron);
			
			cron = CreateStandardCron(7,55,dayOfWeek);
			AddAlarmOnSchedule(scheduler, cron);
			
			cron = CreateStandardCron(22,00,dayOfWeek);
			AddAlarmOnSchedule(scheduler, cron);
						
			
			//************
			//Light Strip
			//************
		}		
	}*/
	
	private Date ComputeNextScheduleActivationDate(SchedulingPattern pattern) {
		
		Predictor predictor = new Predictor(pattern);
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		predictor.setTimeZone(timeZone);		
		//return DateUtils.getDateToString(predictor.nextMatchingDate());
		return predictor.nextMatchingDate(); 
	}
	
	private void ComputeNextRSOpen() {
		
		String scheduleId = null;
		_nextRSOpenDate = "No more opening today";
		
		while ((scheduleId = _rsOpenScheduleIdList.poll()) != null)  {
		
			//Get Task ID to retreive CRON pattern
			//String scheduleId = _rsOpenScheduleIdList.poll();
			
			//if (scheduleId != null) {
			SchedulingPattern pattern = ReturnSchedulerByDayOfWeek().getSchedulingPattern(scheduleId);
			
			Date nextActivation = ComputeNextScheduleActivationDate(pattern);
			
			TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(timeZone);
			cal.setTime(nextActivation);
			int nextActivationDay = cal.get(Calendar.DAY_OF_MONTH);
			cal = Calendar.getInstance();
			int currentDay = cal.get(Calendar.DAY_OF_MONTH);
						
			if (nextActivationDay != currentDay) {
				_logger.info("Next RS opening is not schedule today...will check another open schedule");
			}
			else {			
				_nextRSOpenDate = DateUtils.getDateToString(nextActivation);
				_logger.info("Computed next RS Open date : "+_nextRSOpenDate);
				break;
			}
			//}
		}	
	}
	
	private void ComputeNextRSClose() {
		
		String scheduleId = null;
		_nextRSCloseDate = "No more closing today";
		
		while ((scheduleId = _rsCloseScheduleIdList.poll()) != null)  {			
			
			//if (scheduleId != null) {
			SchedulingPattern pattern = ReturnSchedulerByDayOfWeek().getSchedulingPattern(scheduleId);
			
			Date nextActivation = ComputeNextScheduleActivationDate(pattern);
			
			TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(timeZone);
			cal.setTime(nextActivation);
			int nextActivationDay = cal.get(Calendar.DAY_OF_MONTH);
			cal = Calendar.getInstance();
			int currentDay = cal.get(Calendar.DAY_OF_MONTH);
						
			if (nextActivationDay != currentDay) {
				_logger.info("Next RS closing is not schedule today...will check another close schedule");
			}
			else {			
				_nextRSCloseDate = DateUtils.getDateToString(nextActivation);
				_logger.info("Computed next RS Close date : "+_nextRSCloseDate);
				break;
			}
			//}
		}	
	}
	
	/*private void CreateSundayScenarii(Scheduler scheduler, int dayOfWeek) {
		
		String cron;		
				
		//Elo & Fred at work
		if (!_eloAtHome && !_fredAtHome) {
			
			//*************
			//RollerShutter Management
			//*************
									
			//Close at sunset
			cron = CreateStandardCron(_sunset.get(Calendar.HOUR_OF_DAY),_sunset.get(Calendar.MINUTE),dayOfWeek); 
			AddRSCloseSchedule(scheduler, cron);
			
			
			//*************
			//Alarm Management
			//*************					
			cron = CreateStandardCron(22,00,dayOfWeek);
			AddAlarmOnSchedule(scheduler, cron);
						
			
			//************
			//Light Strip
			//************
		}				
	}*/
	
	/*private void CreateFridayScenarii(Scheduler scheduler, int dayOfWeek) {
		
		String cron;			
				
		//Elo & Fred at work
		if (!_eloAtHome && !_fredAtHome) {
			
			//*************
			//RollerShutter Management
			//*************
						
			//Compute Open in the morning at 6h30 synchronized with sunset/sunrise
			cron = CreateCronRSMorningOpen(6,30,8,40,dayOfWeek);
			AddRSOpenSchedule(scheduler, cron);			
						
			//Close at 7am49
			cron = CreateStandardCron(7,49,dayOfWeek);
			AddRSCloseSchedule(scheduler, cron);										
			
			//*************
			//Alarm Management
			//*************
			cron = CreateStandardCron(6,00,dayOfWeek);
			AddAlarmOffSchedule(scheduler, cron);
			
			cron = CreateStandardCron(7,55,dayOfWeek);
			AddAlarmOnSchedule(scheduler, cron);
			
			//************
			//Light Strip
			//************
		}		
	}*/
	
	private void AddAlarmOnSchedule(Scheduler scheduler, String cron) {
		_logger.info("Scheduling alarm to turn on at : " + cron);
		scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_alarmService.SetAutomaticOn();			
				}
			});
	}
	
	private void AddAlarmOffSchedule(Scheduler scheduler, String cron) {
		_logger.info("Scheduling alarm to turn off at : " + cron);
		scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_alarmService.SetAutomaticOff();			
				}
			});
	}
	
	private void AddRSOpenSchedule(Scheduler scheduler, String cron) {
			
		String schedulerId;
		
		if (cron == null || cron.equals("")) {
			return;
		}
		
		_logger.info("Scheduling RS to open at : " + cron);
		schedulerId = scheduler.schedule(cron, new Runnable() {
		public void run() {	
			_rollerShutterService.OpenAllRollerShutters();
			ComputeNextRSOpen();
			}
		});
		_rsOpenScheduleIdList.addLast(schedulerId);
		
	}
	
	private void AddRSCloseSchedule(Scheduler scheduler, String cron) {
		
		String schedulerId;
		
		if (cron == null || cron.equals("")) {
			return;
		}
		
		_logger.info("Scheduling RS to close at : " + cron);
		schedulerId = scheduler.schedule(cron, new Runnable() {
		public void run() {	
			_rollerShutterService.CloseAllRollerShutters();	
			ComputeNextRSClose();
			}
		});
		_rsCloseScheduleIdList.addLast(schedulerId);
		
	}
	
	/*private void CreateMondayScenarii(Scheduler scheduler, int dayOfWeek) {
		
		String cron;
				
		//Elo & Fred at work
		if (!_eloAtHome && !_fredAtHome) {
			
			//*************
			//RollerShutter Management
			//*************
						
			//Compute Open in the morning at 6h30 synchronized with sunset/sunrise
			cron = CreateCronRSMorningOpen(6,30,8,40,dayOfWeek);			
			AddRSOpenSchedule(scheduler, cron);
						
			//Close at 8am40
			cron = CreateStandardCron(8,40,dayOfWeek);
			AddRSCloseSchedule(scheduler, cron);
			
			//Open at 9ham
			cron = CreateStandardCron(9,00,dayOfWeek);
			AddRSOpenSchedule(scheduler, cron);
			
			//Close at 17h15
			cron = CreateStandardCron(17,15, dayOfWeek); 
			AddRSCloseSchedule(scheduler, cron);
			
			//Close at sunset
			cron = CreateStandardCron(_sunset.get(Calendar.HOUR_OF_DAY),_sunset.get(Calendar.MINUTE),dayOfWeek); 
			AddRSCloseSchedule(scheduler, cron);
			
			//*************
			//Alarm Management
			//*************
			cron = CreateStandardCron(6,00,dayOfWeek);
			AddAlarmOffSchedule(scheduler, cron);
			
			cron = CreateStandardCron(8,45,dayOfWeek);
			AddAlarmOnSchedule(scheduler, cron);
			
			cron = CreateStandardCron(9,00,dayOfWeek);
			AddAlarmOffSchedule(scheduler, cron);
			
			cron = CreateStandardCron(22,00,dayOfWeek);
			AddAlarmOnSchedule(scheduler, cron);
			
			//************
			//Light Strip
			//************
		}		
	}*/
	
	/*private String GetCronWithoutTime(String cron) {
		
		String [] cronArray =  cron.split(" ");
				
		return " " + cronArray[2] + " " + cronArray[3] + " " + cronArray[4];		
	}*/
	
	private String CreateStandardCron(int hour, int minute, int dayOfWeek) {				
		return minute + " " + hour + " * * "+dayOfWeek; //" * * 1,2,3,4,5";		
	}
	
	//0 = Sunday
	private String CreateCronRSMorningOpen(int openHour, int openMinute, int closeHour, int closeMinute, int dayOfWeek) {
		
		Calendar sunrise = Calendar.getInstance(); 			
		Calendar opendate = Calendar.getInstance(); //current time ...currently set to 6am30
		Calendar closedate = Calendar.getInstance(); //current  de time ...currently set to 7am49
		
		opendate.set(Calendar.HOUR_OF_DAY, openHour);
		opendate.set(Calendar.MINUTE, openMinute);
		opendate.set(Calendar.SECOND, 0);
		opendate.set(Calendar.MILLISECOND, 0);		
		
		sunrise.set(Calendar.HOUR_OF_DAY, _sunrise.get(Calendar.HOUR_OF_DAY));
		sunrise.set(Calendar.MINUTE,_sunrise.get(Calendar.MINUTE));
		sunrise.set(Calendar.SECOND, 0);
		sunrise.set(Calendar.MILLISECOND, 0);	
		
		String newCron = null;
		
		//sun already rose up
		if (sunrise.before(opendate)) {
			_logger.info("Sunrise : "+sunrise.getTime()+" is before : "+opendate.getTime()+ ". Gonna open rollershutters at :"+opendate.getTime());			
			newCron = openHour + " " + openMinute + " * * "+dayOfWeek;		
		}		
		else {
			
			closedate.set(Calendar.HOUR_OF_DAY, closeHour);
			closedate.set(Calendar.MINUTE, closeMinute);
			closedate.set(Calendar.SECOND, 0);
			closedate.set(Calendar.MILLISECOND, 0);
			
			//Remove 10 minutes
			//closedate.add(Calendar.MINUTE, -10);
			
		//	TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
			if (sunrise.before(closedate)) {
				_logger.info("Sunrise : "+sunrise.getTime()+" is before : "+closedate.getTime()+ " (planned close time minus 10 minutes). Gonna open rollershutters at : "+sunrise.getTime());				
				newCron = sunrise.get(Calendar.MINUTE) + " " + sunrise.get(Calendar.HOUR_OF_DAY) + " * * "+dayOfWeek; //" * * 1,2,3,4,5";
			}	
			else {
				_logger.info("Sunrise : "+sunrise.getTime()+" is after : "+closedate.getTime()+ " (planned close time minus 10 minutes). Rollershutters will not be opened this morning...bad season of year...");
				newCron = null;
			}
		}
		
		return newCron;
		
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
		//Add 10 minutes to sunset (sky will begin to be dark)
		//Delete 20 minutes to sunrise (sky is already clear)
		_sunset.add(Calendar.MINUTE, 10);
		_sunrise.add(Calendar.MINUTE, -20);
		
		_logger.info("Adjusted Sunrise : " + _sunrise.getTime());
		_logger.info("Adjusted Sunset : " + _sunset.getTime());			
	}
	
}