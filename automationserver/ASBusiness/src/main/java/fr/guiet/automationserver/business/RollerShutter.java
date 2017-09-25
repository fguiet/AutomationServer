package fr.guiet.automationserver.business;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class RollerShutter {

	private static Logger _logger = Logger.getLogger(RollerShutter.class);
	private RollerShutterState _state = RollerShutterState.UNDETERMINED;
	private RollerShutterState _previousState = RollerShutterState.UNDETERMINED;
	private String _id = null;
	private String _apikey = null;
	private String _baseUrl = null;
	private String _getStateUrl = null;
	private String _stopUrl = null;
	private String _upUrl = null;
	private String _downUrl = null;
	private String _name = null;
	
	//TODO : add getter/s
	public boolean _notReachable5 = false;
	public boolean _notReachable10 = false;
	public boolean _notReachable15 = false;
	
	public RollerShutter(String id, String name, String baseUrl, String apikey) {
		_id = id;
		_apikey = apikey;
		_baseUrl = baseUrl;
		_name = name;
		
		_getStateUrl = _baseUrl + "/api/rollershutter?action=state&apikey=" + _apikey;
		_stopUrl = _baseUrl + "/api/rollershutter?action=stop&apikey=" + _apikey;
		_upUrl = _baseUrl + "/api/rollershutter?action=up&apikey=" + _apikey;
		_downUrl = _baseUrl + "/api/rollershutter?action=down&apikey=" + _apikey;
	}
	
	public String getName() {
		return _name;
	}

	public boolean Close() {
		JSONObject jo = sendGetRequest(_downUrl);
		
		if (jo == null) {
			return false;
		}
		else {
			return jo.getBoolean("success");
			/*String success = jo.getString("success");
			
			if (success=="true") {
				return true;
			}
			else {
				return false;
			}*/
		}
	}
	
	public boolean Stop() {
		JSONObject jo = sendGetRequest(_stopUrl);
		
		if (jo == null) {
			return false;
		}
		else {
			return jo.getBoolean("success");
			/*String success = jo.getString("success");
			
			if (success=="true") {
				return true;
			}
			else {
				return false;
			}*/
		}
	}
	
	public boolean Open() {
		JSONObject jo = sendGetRequest(_upUrl);
		
		if (jo == null) {
			return false;
		}
		else {
			return jo.getBoolean("success");
			/*String success = jo.getString("success");
			
			if (success=="true") {
				return true;
			}
			else {
				return false;
			}*/
		}
	}
	
	public RollerShutterState getPreviousState() {		
			return _previousState;		
	}
		
	public RollerShutterState getState() {
		
		RollerShutterState previousState = _state;
		
		JSONObject jo = sendGetRequest(_getStateUrl);
		
		//By default!
		_state = RollerShutterState.UNREACHABLE;
		
		if (jo == null) {
			_state = RollerShutterState.UNREACHABLE;
		}
		else {
			String state = jo.getString("state");
			
			switch(state) {
			case "opened":
				_state = RollerShutterState.OPENED;
				break;
			case "closed":
				_state = RollerShutterState.CLOSED;
				break;
			case "undetermined":
				_state = RollerShutterState.UNDETERMINED;
				break;
			}
		}
		
		if (previousState != _state) //{
			_previousState = previousState;
	//		_previousStateChanged = true;
	//	}
	//	else 
	//		_previousStateChanged = false;
		
		return _state;
	}
	
	private JSONObject sendGetRequest(String url) {
		
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
		 
	}
}