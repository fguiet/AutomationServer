package fr.guiet.automationserver.business;

import com.rapplogic.xbee.api.XBeeAddress64;
import org.apache.log4j.Logger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.IOException;

import fr.guiet.automationserver.dto.*;
import fr.guiet.automationserver.dataaccess.DbManager;

public class Sensor implements IXBeeListener { 

	private static Logger _logger = Logger.getLogger(Sensor.class);

	private long _idSensor;
	private XBeeAddress64 _sensorAddress;
	private Timer _timer = null;
	private static XBeeService _XBeeInstance = null;
	private static Object _lockObj = new Object();
	private Date _lastInfoReceived = null; //Date de derniere reception d'une info du capteur
	private float _actualTemp;
	private float _actualHumidity;
	private Room _room = null;
	private boolean _alertSent = false; //Alerte envoyée par SMS en cas de non réception de message d'un capteur depuis trop longtemps
	private static final String targetURL = "https://smsapi.free-mobile.fr/sendmsg?user=@user@&pass=@pass@&msg=@msg@";
	
	public float getActualTemp() {
		return _actualTemp;
	}
	
	public float getActualHumidity() {
		return _actualHumidity;
	}
	
	private static XBeeService XBeeInstance() {
		synchronized(_lockObj) {
			if(_XBeeInstance == null){
				_XBeeInstance = new XBeeService();
			}
		}
		
		return _XBeeInstance;
	}
	
	public boolean HasTimeoutOccured() {
		
		//Si pas encore recu d'info, on considere que c'est un timeout
		if (_lastInfoReceived == null) return true;
		
		Date currentDate = new Date();
		
		long diff = currentDate.getTime() - _lastInfoReceived.getTime();
		long diffMinutes = diff / (60 * 1000);	

		//Timeout si aucune info recu du capteur au bout de 5 minutes
	    if (diffMinutes >= 5) {
	    	        //TODO : Faire une classe métier pout l'envoie des SMS
	    	        if (!_alertSent) {
	    	        	SendSMS();
	    	        }
	    	        
			return true;
		}
		else {
			_alertSent = false; //Réinitialisation
			return false;
		}
	}
	
	private void SendSMS() {
		try {
			
			String freeSMSUser = System.getProperty("freeSMSUser");
			String freeSMSPass = System.getProperty("freeSMSPass");
			
			String url = targetURL.replace("@user@",freeSMSUser);
			url = targetURL.replace("@pass@",freeSMSPass);
			url = targetURL.replace("@msg@",URLEncoder.encode("Le capteur de la pièce "+_room.getName()+"ne répond plus"));
			
			URL restServiceURL = new URL(url);

			HttpURLConnection httpConnection = (HttpURLConnection) restServiceURL.openConnection();
			httpConnection.setRequestMethod("GET");
			//httpConnection.setRequestProperty("Accept", "application/json");

			if (httpConnection.getResponseCode() != 200) {
				_logger.error("Code retour incorrect lors de l'envoi du SMS Free ("+httpConnection.getResponseCode()+")"");
			}
			else {
				_alertSent = true;
			}
			httpConnection.disconnect();

		  } catch (MalformedURLException e) {

			_logger.error(e.printStackTrace());

		  } catch (IOException e) {

			_logger.error(e.printStackTrace());
		  }
		}
	}
	
	//Reception d'un message
	public void processResponse(String message) {
		
		try {
		
		//_logger.info(_room.getName()+ " - Reception message : "+message);
		
		String [] messageArray =  message.split(";");
		
		//_logger.info(_room.getName()+ " - " + messageArray[0]);

		if (messageArray[0].equals("SETSENSORINFO")) {
			_lastInfoReceived = new Date();
			
			//_logger.info("ouhou");
			
			String actualTemp = messageArray[1];
			//wantedTemp = messageArray[2];
			
			String actualHumidity;
			//Pour la gestion des deux versions
			if (messageArray.length==4)		
				//Ancienne version du message recu des Xbee
				actualHumidity = messageArray[3];
			else {
				//Nouvelle version du message recu des Xbee
				actualHumidity = messageArray[2];
			}
			
			_actualTemp = Float.parseFloat(actualTemp);
			_actualHumidity = Float.parseFloat(actualHumidity);
			
			DbManager dbManager = new DbManager();
		    dbManager.SaveSensorInfo(_idSensor, _actualTemp, _room.getWantedTemp(), _actualHumidity);
		    _logger.info("Sauvegardee en base de donnees des infos du capteur pour la piece : " +_room.getName()+", Temp actuelle : "+_actualTemp+", Temp désirée : "+_room.getWantedTemp()+", Humidité : "+_actualHumidity);
		}
		}
		catch(Exception e) {
			_logger.error("Erreur lors du traitement du message reçu pour la piece : "+_room.getName()+", message recu : "+message);
			_lastInfoReceived = null;
		}
		
	}
	
	public String sensorAddress() {
		return _sensorAddress.toString();
	} 

	//Creation de la tache de recuperation des infos des capteurs
	private void CreateGetSensorInfoTask() {
		
		TimerTask getSensorInfoTask  = new TimerTask() {        
			@Override
			public void run()
			{
				GetSensorInfo();				
			}
        };
		
		_timer = new Timer(true);
		//Toutes les minutes on enregistre une trame 
		_timer.schedule(getSensorInfoTask, 5000, 60000);	
	}
	
	//Envoi d'un message pour recuperer les infos du capteur
	private void GetSensorInfo() {
		
		//XBeeService xbee = new XBeeService(_sensorAddress);
		XBeeInstance().SendAsynchronousMessage(_sensorAddress,"GETSENSORINFO");		
	}
	
	private Sensor(SensorDto dto,Room room) {
		
		_idSensor = dto.sensorId;
		_room = room;
		
		String [] address = dto.sensorAddress.split(" ");
				
		int i1 = Integer.parseInt(address[0], 16);
		int i2 = Integer.parseInt(address[1], 16);
		int i3 = Integer.parseInt(address[2], 16);
		int i4 = Integer.parseInt(address[3], 16);
		int i5 = Integer.parseInt(address[4], 16);
		int i6 = Integer.parseInt(address[5], 16);
		int i7 = Integer.parseInt(address[6], 16);
		int i8 = Integer.parseInt(address[7], 16);
		
		_sensorAddress = new XBeeAddress64(i1,i2,i3,i4,i5,i6,i7,i8);
		
		CreateGetSensorInfoTask();
		XBeeInstance().addXBeeListener(this);
	}
	
	public static Sensor LoadFromDto(SensorDto dto, Room room) {
		
		return new Sensor(dto, room);
	}
	
	public XBeeAddress64 getSensorAddress() {
		return _sensorAddress;
	}
	
	public long getIdSendor() {
		return _idSensor;
	}
}
