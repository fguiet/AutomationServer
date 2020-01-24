package fr.guiet.automationserver.business.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.pi4j.io.serial.SerialFactory;

import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;

public class WaterMeterService implements Runnable {

	private boolean _isStopped = false; // Service arrete?
	private static Logger _logger = LogManager.getLogger(WaterMeterService.class);
	private Timer _timer = null;
	private DbManager _dbManager = null;
	private SMSGammuService _smsGammuService = null;
	private MqttService _mqttService = null;
	
	private String _pub_topic ="/guiet/automationserver/watermeter";
	
	public WaterMeterService(SMSGammuService smsGammuService, MqttService mqttService) {
		_smsGammuService = smsGammuService;
		_mqttService = mqttService;
		_dbManager = new DbManager();	
	}
	
	private void CreateWaterMeterInfoTask() {

		TimerTask sendWaterMeterInfoTask = new TimerTask() {
			@Override
			public void run() {
				
				try {
				
				_logger.info("Getting Water Meter info...");

				JSONObject json =  _dbManager.GetWaterMeterInfo();
				
				_mqttService.SendMsg(_pub_topic, json.toString());

				_logger.info("Sending Water Meter info");
				}
				catch (Exception e) {
					_logger.error("Error occured in sendWaterMeterInfoTask...", e);

					SMSDto sms = new SMSDto("ecd763ea-0361-447b-800a-f4ad82e20498");
					sms.setMessage("Error occured in sendWaterMeterInfoTask, review error log for more details");
					_smsGammuService.sendMessage(sms);
				}
			}
		};

		_logger.info("Creating Water Meter Info task");

		_timer = new Timer(true);
		
		//1000 ms = 1s
		_timer.scheduleAtFixedRate(sendWaterMeterInfoTask, 5000, 60000);

	}
	
	@Override
	public void run() {

		_logger.info("Starting WaterMeter...");

		CreateWaterMeterInfoTask();
		
		while (!_isStopped) {

			try {
				
				Thread.sleep(2000);
				
			} catch (Exception e) {
				_logger.error("Error occured in WaterMeter service...", e);

				SMSDto sms = new SMSDto("166d9b40-30c0-4a14-9a92-c48b0ace0011");
				sms.setMessage("Error occured in WaterMeter services service, review error log for more details");
				_smsGammuService.sendMessage(sms);
			}
		}
	}
	
	// Arret du service LoRaService
	public void StopService() {
		
		if (_timer != null)
			_timer.cancel();

		_logger.info("Stopping WaterMeter service...");

		_isStopped = true;
	}
}	
