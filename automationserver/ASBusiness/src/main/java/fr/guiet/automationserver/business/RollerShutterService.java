package fr.guiet.automationserver.business;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import fr.guiet.automationserver.dto.SMSDto;
import it.sauronsoftware.cron4j.Scheduler;

/*si pas awaymode 
ET
sunrise time < à 6h30
alors ouvre le volet

si pas awaymode
ET
sunrse time >= à 6h30 et sunrise time < à 7h40
alors ouverture = sunrise time (scheduler à mettre en place)


Fermeture des volets à 7h40

pour ouverture = verifier si pas deja ouvert (appel via http)*/


public class RollerShutterService implements Runnable {

	// Logger
	private static Logger _logger = Logger.getLogger(RollerShutterService.class);
	
	private Calendar _sunset = null;
	private Calendar _sunrise = null;
	private boolean _awayModeStatus = false;
	private boolean _isStopped = false; // Service arrete?
	private SMSGammuService _smsGammuService = null;
	private Scheduler _computeSunSetSunRiseScheduler = null;
	private Scheduler _weekCloseScheduler = null;
	private Scheduler _weekOpenScheduler = null;
	private String _apikey = null;
	private String _baseUrlRs1 = null;
	private String _baseUrlRs2 = null;
	private RollerShutter _rsWest = null;
	private RollerShutter _rsNorth = null;
	
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
	
	public void SetAwayModeOn() {
		_awayModeStatus = true;
		_logger.info("Setting away mode ON");
	}

	public void SetAwayModeOff() {
		_awayModeStatus = false;
		_logger.info("Setting away mode OFF");
	}
	
	@Override
	public void run() {
		
		_logger.info("Starting rollershutters management...");		
		
		_rsWest = new RollerShutter("1", _baseUrlRs1, _apikey);
		_rsNorth = new RollerShutter("2",_baseUrlRs2, _apikey);
		
		ComputeSunsetSunrise();
		
		// Creates a Scheduler instance.
		_computeSunSetSunRiseScheduler = new Scheduler();
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		_computeSunSetSunRiseScheduler.setTimeZone(timeZone);
		
		//Calendar calendar = new GregorianCalendar();
		//calendar.setTimeZone(timeZone);
				
		
		//CRON pattern
		//minute hour day of month (1-31) month (1-12) day of week (0 sunday, 6 saturday)
		
		_computeSunSetSunRiseScheduler.schedule("0 5 * * *", new Runnable() {
		public void run() {
			_logger.info("Starting compute sunrise/sunset scheduler at : 0 5 * * *");
			ComputeSunsetSunrise();
		}
		});
		
		//Schedule that close rollershutter automatically at 7h40 every day of the week 		
		_weekCloseScheduler.schedule("40 7 * * 1,2,3,4,5", new Runnable() {
		public void run() {
			_logger.info("Starting automatic rollershutter closing scheduler at : 40 7 * * 1,2,3,4,5");
			
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
		});
		
		//Schedule that open rollershutter automatically at 6h30 every day of the week 		
		_weekOpenScheduler.schedule("30 6 * * 1,2,3,4,5", new Runnable() {
		public void run() {
			_logger.info("Starting automatic rollershutter opening scheduler at : 30 6 * * 1,2,3,4,5");
			
			if (!_awayModeStatus) {
			
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
				_logger.info("Open requested but awaymode activated ! Open canceled...");
			}
		}
		});
		
		
		// Starts the scheduler.
		_computeSunSetSunRiseScheduler.start();
		_weekCloseScheduler.start();
		_weekOpenScheduler.start();

		while (!_isStopped) {

			try {
				
				//Check state here and send message if not responding!!
				
				// Sleep for 1 minutes
				Thread.sleep(60000);

			} catch (Exception e) {
				_logger.error("Error occured in rollershutter management service", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in rollershutter management service, review error log for more details");
				_smsGammuService.sendMessage(sms, true);
			}
		}		
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
	}
}