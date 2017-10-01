package fr.guiet.automationserver.business;

import java.text.ParseException;
import java.util.Date;

/*import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;*/

import org.apache.log4j.Logger;
//import org.json.JSONObject;

import fr.guiet.automationserver.dto.SMSDto;

public class RollerShutter {

	private static Logger _logger = Logger.getLogger(RollerShutter.class);
	private RollerShutterState _state = RollerShutterState.VOID;
	private RollerShutterState _previousState = RollerShutterState.VOID;
	private int _id = -1;
	//private String _apikey = null;	
	private String _name = null;
	private MqttClientMgt _mqttClient;
	
	private boolean _alertSent5 = false;
	private boolean _alertSent10 = false;
	private boolean _alertSentMore = false;
	
	private Date _lastStateReceived = null;
	
	private SMSGammuService _smsGammuService = null;
	private String _pub_topic ="/guiet/automationserver/rollershutter";
	
	public RollerShutter(int id, String name, SMSGammuService smsGammuService) {
		_id = id;
		//_apikey = apikey;
		//_baseUrl = baseUrl;
		_name = name;
			
		
		_mqttClient = new MqttClientMgt();
		_smsGammuService = smsGammuService;
	}
	
	public String getName() {
		return _name;
	}
	
	public boolean HasTimeoutOccured() {

		// Si pas encore recu d'info, on considere que c'est un timeout
		if (_lastStateReceived == null)
			return true;

		Date currentDate = new Date();

		long diff = currentDate.getTime() - _lastStateReceived.getTime();
		long diffMinutes = diff / (60 * 1000);

		if (diffMinutes > 5 && diffMinutes < 10) {

			if (!_alertSent5) {

				setState(RollerShutterState.UNREACHABLE);
				
				SMSDto sms = new SMSDto();
				String message = String.format("Rollershutter %s is not sending state (5 minutes alert)", _name);
				sms.setMessage(message);
				_smsGammuService.sendMessage(sms, true);
				
				_alertSent5 = true;
			}

			return true;
		}

		if (diffMinutes >= 10 && diffMinutes < 20) {

			if (!_alertSent10) {
				setState(RollerShutterState.UNREACHABLE);
				
				SMSDto sms = new SMSDto();
				String message = String.format("Rollershutter %s is not sending state (10 minutes alert)", _name);
				sms.setMessage(message);
				
				_smsGammuService.sendMessage(sms, true);

				_alertSent10 = true;
			}

			return true;
		}

		if (diffMinutes >= 20) {

			if (!_alertSentMore) {
				setState(RollerShutterState.UNREACHABLE);
				
				SMSDto sms = new SMSDto();
				String message = String.format("Rollershutter %s is not sending state (20 minutes alert)...Time to do something", _name);
				sms.setMessage(message);
				_smsGammuService.sendMessage(sms, true);

				_alertSentMore = true;
			}

			return true;
		}

		_alertSent5 = false; // Réinitialisation
		_alertSent10 = false; // Réinitialisation
		_alertSentMore = false; // Réinitialisation

		return false;

	}

	public void setState(RollerShutterState state) {
		
		_lastStateReceived = new Date();
			
		//Save previsous state
		_previousState = _state;
		//Update current state to new state
		_state = state;
		
		if (_previousState != _state) {
			_logger.info(_name+" passed from : "+_previousState.name()+" to "+_state.name());
			CheckForIntruders();			
		}
	}
	
	public void Close() {
		
		_mqttClient.SendMsg(_pub_topic, "SETACTION;" + _id + ";DOWN");
	}
	
	private void CheckForIntruders() {
				
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
			if (_previousState == RollerShutterState.CLOSED && (_state == RollerShutterState.UNREACHABLE ||
																_state == RollerShutterState.OPENED ||
															   _state == RollerShutterState.VOID ||
																	   _state == RollerShutterState.UNDETERMINED)) {
				SMSDto sms = new SMSDto();
				sms.setMessage("Le volet roulant "+_name+" est passé de l'état : "+_previousState.name()+ " à l'état : "+_state.name()+ " durant la période 21:00:00 - 06:00:00. Bizarre non?");
				_smsGammuService.sendMessage(sms, true);
			}
		}
	}		
	
	
	public void Stop() {
		
		_mqttClient.SendMsg(_pub_topic, "SETACTION;" + _id + ";STOP");		
	}
	
	public void RequestState() {		
		_mqttClient.SendMsg(_pub_topic, "SETACTION;" + _id + ";STATE");		
	}
	
	public void Open() {
		_mqttClient.SendMsg(_pub_topic, "SETACTION;" + _id + ";OPEN");		
	}
	
	public RollerShutterState getPreviousState() {		
			return _previousState;		
	}
	
	public RollerShutterState getState() {
		return _state;
	}
	
	
	/*private JSONObject sendGetRequest(String url) {
		
		try {	
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setConnectTimeout(3000);
			
			// optional default is GET
			con.setRequestMethod("GET");
	
			//add request header
			//con.setRequestProperty("User-Agent", USER_AGENT);
	
			int responseCode = con.getResponseCode();
			//System.out.println("\nSending 'GET' request to URL : " + url);
			//System.out.println("Response Code : " + responseCode);
			
			if (responseCode == 200) {
			
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
						
			JSONObject json = new JSONObject(response.toString());
			
		    return json;
			}
			else {
				_logger.info("Le capteur du volet roulant avec l'id : "+_id+" a répondu mais avec un code retour : "+responseCode);   
				return null;
			}
						
		}
		catch (java.net.SocketTimeoutException ste) {
			_logger.info("Timeout! - Impossible de contacter le capteur du volet roulant avec l'id : "+_id);   
			return null;
		} catch (java.io.IOException e) {
			_logger.info("Erreur lors de la tentative de contact avec le capteur du volet roulant avec l'id : "+_id,e);
			return null;
		}
		 
	}*/
}