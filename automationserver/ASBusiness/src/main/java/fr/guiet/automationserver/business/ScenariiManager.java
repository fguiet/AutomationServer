package fr.guiet.automationserver.business;

import java.util.Calendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import it.sauronsoftware.cron4j.Predictor;
import it.sauronsoftware.cron4j.Scheduler;

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
	
	private boolean _eloAtHome = false;
	private boolean _fredAtHome = false;
	
	private Calendar _sunset = null;
	private Calendar _sunrise = null;
	
	private RollerShutterService _rollerShutterService = null;
	private AlarmService _alarmService = null;	
	
	//Lie à Sunset
	private String _nextWeekNightCloseDate = "NA";
	//a 7h79
	private String _nextWeekMorningCloseDate = "NA";
	//lié à sunrise
	private String _nextWeekMorningOpenDate = "NA";

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
	
	public String getNextWeekNightCloseDate() {
		if (_rollerShutterService.GetAutomaticManagementStatus().equals("ON"))
			return _nextWeekNightCloseDate;
		else 
			return "mgt auto. désactivé";
	}
	
	public String getNextWeekMorningCloseDate() {
		if (_rollerShutterService.GetAutomaticManagementStatus().equals("ON"))
			return _nextWeekMorningCloseDate;
		else 
			return "mgt auto. désactivé";
	}
	
	public String getNextWeekMorningOpenDate() {
		if (_rollerShutterService.GetAutomaticManagementStatus().equals("ON"))
			return _nextWeekMorningOpenDate;
		else 
			return "mgt auto. désactivé";
	}
	
	public void StopScenariiManager() {
		
		_logger.info("Stopping scenarri manager...");
		StopAllSchedulers();
		_scenariiScheduler.stop();
	}
	
	
	
	/*private void ComputeWeekMorningOpenDate(String cron) {
		
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
	}*/
	
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
		    	_sundayScheduler = new Scheduler();
		    	_sundayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Sunday scenarii...");
		    	CreateSundayScenarii(_sundayScheduler, 0);
		    	break;

		    case Calendar.MONDAY:
		    	_mondayScheduler = new Scheduler();
				_mondayScheduler.setTimeZone(timeZone);
		    	
				_logger.info("Creating Monday scenarii...");
		    	CreateMondayScenarii(_mondayScheduler, 1);
		    	
		    	break;

		    case Calendar.TUESDAY:
		    	_tuesdayScheduler = new Scheduler();
		    	_tuesdayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Tuesday scenarii...");
		    	CreateStandardDayScenarii(_tuesdayScheduler, 2);
		    	
		    	break;
		    
		    case Calendar.WEDNESDAY:
		    	_wesnesdayScheduler = new Scheduler();
		    	_wesnesdayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Wednesday scenarii...");
		    	CreateStandardDayScenarii(_wesnesdayScheduler, 3);		    		 
		    	break;
		    
		    case Calendar.THURSDAY:
		    	_thursdayScheduler = new Scheduler();
		    	_thursdayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Thursday scenarii...");
		    	CreateStandardDayScenarii(_thursdayScheduler, 4);
		    	
		    	break;
		    
		    case Calendar.FRIDAY:
		    	_fridayScheduler = new Scheduler();
		    	_fridayScheduler.setTimeZone(timeZone);
		    	
		    	_logger.info("Creating Friday scenarii...");
		    	CreateFridayScenarii(_fridayScheduler, 5);
		    	
		    	break;
		    
		    case Calendar.SATURDAY:
		    	_logger.info("Creating Saturday scenarii...");
		    	break;		    
		}	
	}
	
	private void CreateStandardDayScenarii(Scheduler scheduler, int dayOfWeek) {
		
		String cron;
		
		//_logger.info("Creating standard scenarii...");
				
		//Elo & Fred at work
		if (!_eloAtHome && !_fredAtHome) {
			
			//*************
			//RollerShutter Management
			//*************
						
			//Compute Open in the morning at 6h30 synchronized with sunset/sunrise
			cron = CreateCronRSMorningOpen(6,30,8,40,dayOfWeek);
						
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.OpenAllRollerShutters();				
				}
			});
			
			//Close at 7am49
			cron = CreateStandardCron(7,49,dayOfWeek);
			_logger.info("Scheduling RS to close at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});
									
			//Close at sunset
			cron = CreateStandardCron(_sunset.get(Calendar.HOUR_OF_DAY),_sunset.get(Calendar.MINUTE),dayOfWeek); 
			_logger.info("Scheduling RS to close at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});
			
			//*************
			//Alarm Management
			//*************
			cron = CreateStandardCron(6,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn off at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOff();			
					}
				});
			
			cron = CreateStandardCron(7,55,dayOfWeek);
			_logger.info("Scheduling alarm to turn on at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOn();			
					}
				});
			
			cron = CreateStandardCron(22,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn on at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_alarmService.SetAutomaticOn();			
				}
			});
						
			
			//************
			//Light Strip
			//************
		}		
		
		//_logger.info("Starting standard scenarri scheduler...");
		scheduler.start();
	}	
	
	private void CreateSundayScenarii(Scheduler scheduler, int dayOfWeek) {
		
		String cron;
		
		//_logger.info("Creating standard scenarii...");
				
		//Elo & Fred at work
		if (!_eloAtHome && !_fredAtHome) {
			
			//*************
			//RollerShutter Management
			//*************
						
			//Compute Open in the morning at 6h30 synchronized with sunset/sunrise
			/*cron = CreateCronRSMorningOpen(6,30,8,40,dayOfWeek);
						
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.OpenAllRollerShutters();				
				}
			});*/
			
			//Close at 7am49
			/*cron = CreateStandardCron(7,49,dayOfWeek);
			_logger.info("Scheduling RS to close at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});*/
									
			//Close at sunset
			cron = CreateStandardCron(_sunset.get(Calendar.HOUR_OF_DAY),_sunset.get(Calendar.MINUTE),dayOfWeek); 
			_logger.info("Scheduling RS to close at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});
			
			//*************
			//Alarm Management
			//*************
			/*cron = CreateStandardCron(6,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn off at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOff();			
					}
				});
			
			cron = CreateStandardCron(7,55,dayOfWeek);
			_logger.info("Scheduling alarm to turn on at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOn();			
					}
				});*/
			
			cron = CreateStandardCron(22,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn on at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_alarmService.SetAutomaticOn();			
				}
			});
						
			
			//************
			//Light Strip
			//************
		}		
		
		//_logger.info("Starting standard scenarri scheduler...");
		scheduler.start();
	}
	
	private void CreateFridayScenarii(Scheduler scheduler, int dayOfWeek) {
		
		String cron;
		
		//_logger.info("Creating standard scenarii...");
				
		//Elo & Fred at work
		if (!_eloAtHome && !_fredAtHome) {
			
			//*************
			//RollerShutter Management
			//*************
						
			//Compute Open in the morning at 6h30 synchronized with sunset/sunrise
			cron = CreateCronRSMorningOpen(6,30,8,40,dayOfWeek);
						
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.OpenAllRollerShutters();				
				}
			});
			
			//Close at 7am49
			cron = CreateStandardCron(7,49,dayOfWeek);
			_logger.info("Scheduling RS to close at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});
									
			//Close at sunset
			/*cron = CreateStandardCron(_sunset.get(Calendar.HOUR_OF_DAY),_sunset.get(Calendar.MINUTE),dayOfWeek); 
			_logger.info("Scheduling RS to close at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});*/
			
			//*************
			//Alarm Management
			//*************
			cron = CreateStandardCron(6,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn off at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOff();			
					}
				});
			
			cron = CreateStandardCron(7,55,dayOfWeek);
			_logger.info("Scheduling alarm to turn on at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOn();			
					}
				});
			
			/*cron = CreateStandardCron(22,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn on at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_alarmService.SetAutomaticOn();			
				}
			});*/
						
			
			//************
			//Light Strip
			//************
		}		
		
		//_logger.info("Starting standard scenarri scheduler...");
		scheduler.start();
	}
	
	private void CreateMondayScenarii(Scheduler scheduler, int dayOfWeek) {
		
		String cron;
		
		//_logger.info("Creating monday scenarii...");
				
		//Elo & Fred at work
		if (!_eloAtHome && !_fredAtHome) {
			
			//*************
			//RollerShutter Management
			//*************
						
			//Compute Open in the morning at 6h30 synchronized with sunset/sunrise
			cron = CreateCronRSMorningOpen(6,30,8,40,dayOfWeek);
						
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.OpenAllRollerShutters();				
				}
			});
			
			//Close at 8am40
			cron = CreateStandardCron(8,40,dayOfWeek);
			_logger.info("Scheduling RS to close at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});
			
			//Open at 9ham
			cron = CreateStandardCron(9,00,dayOfWeek);
			_logger.info("Scheduling RS to open at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.OpenAllRollerShutters();		
				}
			});
			
			//Close at sunset
			cron = CreateStandardCron(_sunset.get(Calendar.HOUR_OF_DAY),_sunset.get(Calendar.MINUTE),dayOfWeek); 
			_logger.info("Scheduling RS to close at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_rollerShutterService.CloseAllRollerShutters();			
				}
			});
			
			//*************
			//Alarm Management
			//*************
			cron = CreateStandardCron(6,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn off at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOff();			
					}
				});
			
			cron = CreateStandardCron(8,45,dayOfWeek);
			_logger.info("Scheduling alarm to turn on at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOn();			
					}
				});
			
			cron = CreateStandardCron(9,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn off at :" + cron);
			scheduler.schedule(cron, new Runnable() {
				public void run() {	
					_alarmService.SetAutomaticOff();			
					}
				});
			
			cron = CreateStandardCron(22,00,dayOfWeek);
			_logger.info("Scheduling alarm to turn on at :" + cron);
			scheduler.schedule(cron, new Runnable() {
			public void run() {	
				_alarmService.SetAutomaticOn();			
				}
			});
			
			//************
			//Light Strip
			//************
		}		
		
		//_logger.info("Starting monday scenarri scheduler...");
		scheduler.start();
	}
	
	private String GetCronWithoutTime(String cron) {
		
		String [] cronArray =  cron.split(" ");
				
		return " " + cronArray[2] + " " + cronArray[3] + " " + cronArray[4];		
	}
	
	private String CreateStandardCron(int hour, int minute, int dayOfWeek) {				
		return minute + " " + hour + " * * "+dayOfWeek; //" * * 1,2,3,4,5";		
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