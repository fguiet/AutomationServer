package fr.guiet.automationserver.business;

import java.util.Calendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import it.sauronsoftware.cron4j.Scheduler;

public class ScenariiManager {

	// Logger
	private static Logger _logger = Logger.getLogger(RoomService.class);
		
	private Scheduler _scenariiScheduler = null;
	private Scheduler _sundayScheduler = null;
	private Scheduler _mondayScheduler = null;
	private Scheduler _tuesdayScheduler = null;
	private Scheduler _wesnesdayScheduler = null;
	private Scheduler _thursdayScheduler = null;
	private Scheduler _fridayScheduler = null;
	private Scheduler _saturdayScheduler = null;
	
	private boolean _eloAtHome = false;
	private boolean _fredAtHome = false;
	
	private Calendar _sunset = null;
	private Calendar _sunrise = null;
	
	private RollerShutterService _rollerShutterService = null;
	private AlarmService _alarmService = null;

	public ScenariiManager(RollerShutterService rollerShutterService,
							AlarmService alarmService) {		
				
		_logger.info("Starting scenarii manager...");
		
		_rollerShutterService = rollerShutterService;
		_alarmService = alarmService;
		
		// 0 = Sunday
		_scenariiScheduler.schedule("05 00 * * 0,1,2,3,4,5,6", new Runnable() {
		public void run() {	
			StartCurrentDayScenario();					
		}
		});
		
		_scenariiScheduler.start();
	}
	
	public void StopScenariiManager() {
		
		_logger.info("Stopping scenarri manager...");
		StopAllSchedulers();
		_scenariiScheduler.stop();
	}
	
	public void StartingScenariiManager() {
		
		_logger.info("Starting scenarii manager...");
		_scenariiScheduler.start();
		
		//Don't wait! start now!
		StartCurrentDayScenario();
	}
	
	private void StopAllSchedulers() {
				
		_sundayScheduler.stop();
		_mondayScheduler.stop();
		_tuesdayScheduler.stop();
		_wesnesdayScheduler.stop();
		_thursdayScheduler.stop();
		_fridayScheduler.stop();
		_saturdayScheduler.stop();
		
	}
	
	private void StartCurrentDayScenario() {
		
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		
		//Every day compute sunset/sunrise
		ComputeSunsetSunrise();
		
		//Stop all schedulers first
		StopAllSchedulers();
		
		//Start/Stop day to day scheduler
		Calendar cal = Calendar.getInstance();
		int day = cal.get(Calendar.DAY_OF_WEEK); 

		switch (day) {
		    case Calendar.SUNDAY:
		    	//_sundayScheduler.start();
		    	break;

		    case Calendar.MONDAY:
		    	_mondayScheduler = null;
		    	
		    	_mondayScheduler = new Scheduler();
				_mondayScheduler.setTimeZone(timeZone);
		    	
		    	CreateMondayScenarii();
		    	//_mondayScheduler.start();
		    	break;

		    case Calendar.TUESDAY:
		    	//_tuesdayScheduler.start();
		    	break;
		    
		    case Calendar.WEDNESDAY:
		    	//_wesnesdayScheduler.start();
		    	break;
		    
		    case Calendar.THURSDAY:
		    	//_thursdayScheduler.start();
		    	break;
		    
		    case Calendar.FRIDAY:
		    	//_fridayScheduler.start();
		    	break;
		    
		    case Calendar.SATURDAY:
		    	//_saturdayScheduler.start();
		    	break;		    
		}	
	}
	
	
	private void CreateMondayScenarii() {
		
		String cron;
		
		_logger.info("Creating monday scenarii...");
		
		//*************
		//RollerShutter Management
		//*************
		if (!_eloAtHome && !_fredAtHome) {
			//Compute Open
			cron = CreateCronRSMorningOpen(6,30,8,40,1);
			
			_mondayScheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.OpenAllRollerShutters();				
				}
			});
			
			//Close at 8am40
			cron = CreateStandardCron(8,40,1);
			_mondayScheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});
			
			//*************
			//Alarm Management
			//*************
			cron = CreateStandardCron(8,45,1);
			_mondayScheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetOn();			
					}
				});
			
			cron = CreateStandardCron(9,00,1);
			_mondayScheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetOff();			
					}
				});
			
			//Light Strip
		}		
	}
	
	private String CreateStandardCron(int hour, int minute, int dayOfWeek) {				
		return minute + " " + hour + "* * "+dayOfWeek; //" * * 1,2,3,4,5";		
	}
	
	//0 = Sunday
	private String CreateCronRSMorningOpen(int openHour, int openMinute, int closeHour, int closeMinute, int dayOfWeek) {
		
		Calendar sunrise = Calendar.getInstance(); 			
		Calendar opendate = Calendar.getInstance(); //current time ...currently set to 6am30
		Calendar closedate = Calendar.getInstance(); //current time ...currently set to 7am49
		
		opendate.set(Calendar.HOUR_OF_DAY, openHour);
		opendate.set(Calendar.MINUTE, openMinute);
		opendate.set(Calendar.SECOND, 0);
		opendate.set(Calendar.MILLISECOND, 0);		
		
		sunrise.set(Calendar.HOUR_OF_DAY, _sunrise.get(Calendar.HOUR_OF_DAY));
		sunrise.set(Calendar.MINUTE,_sunrise.get(Calendar.MINUTE));
		sunrise.set(Calendar.SECOND, 0);
		sunrise.set(Calendar.MILLISECOND, 0);	
		
		String newCron = "";
		
		//sun already rose up
		if (sunrise.before(opendate)) {
			_logger.info("Sunrise : "+sunrise.getTime()+" is before : "+opendate.getTime()+ ". Gonna open rollershutters at :"+opendate.getTime());			
			newCron = openHour + " " + openMinute + "* * "+dayOfWeek;		
		}		
		else {
			
			closedate.set(Calendar.HOUR_OF_DAY, closeHour);
			closedate.set(Calendar.MINUTE, closeMinute);
			closedate.set(Calendar.SECOND, 0);
			closedate.set(Calendar.MILLISECOND, 0);
			
			//Remove 10 minutes
			closedate.add(Calendar.MINUTE, -5);
			
		//	TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
			if (sunrise.before(closedate)) {
				_logger.info("Sunrise : "+sunrise.getTime()+" is before : "+closedate.getTime()+ " (planned close time minus 5 minutes). Gonna open rollershutters at : "+sunrise.getTime());				
				newCron = sunrise.get(Calendar.MINUTE) + " " + sunrise.get(Calendar.HOUR_OF_DAY) + "* * "+dayOfWeek; //" * * 1,2,3,4,5";
			}	
			else {
				_logger.info("Sunrise : "+sunrise.getTime()+" is after : "+closedate.getTime()+ " (planned close time minus 5 minutes). Rollershutters will not be opened this morning...bad season of year...");
				newCron = "";
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
				
		//ComputeWeekNightCloseScheduler();		
		//ComputeWeekMorningOpenScheduler();
		//ComputeNextWeekMorningCloseDate();
	}
	
}