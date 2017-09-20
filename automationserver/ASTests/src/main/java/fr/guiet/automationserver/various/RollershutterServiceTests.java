package fr.guiet.automationserver.various;



import fr.guiet.automationserver.business.RollerShutterService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import fr.guiet.automationserver.business.RollerShutter;


public class RollershutterServiceTests {
	public static void main(String args[]) {
		
		try {	
			URL obj = new URL("http://192.168.1.40/api/rollershutter?action=down&apikey=AzFqRtUpZ@");
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
			
			
			boolean toto = json.getBoolean("success");
			
			
			System.out.println(toto);
			
		    //return json;
			}
			else {
			//	_logger.info("Le capteur du volet roulant avec l'id :  a répondu mais avec un code retour : "+responseCode);   
			//	return null;
			}
						
		}
		catch (java.net.SocketTimeoutException ste) {
		//	_logger.info("Timeout! - Impossible de contacter le capteur du volet roulant avec l'id : ");   
		//	return null;
		} catch (java.io.IOException e) {
			//_logger.info("Erreur lors de la tentative de contact avec le capteur du volet roulant avec l'id : ",e);
			//return null;
		}
		
		
		//RollerShutter rs = new RollerShutter("1");;
		//rs.getState();
		//RollerShutterService rss = new RollerShutterService();
		//Thread rssThread = new Thread(rss);
		//rssThread.start();
	
		//Test d'une minute
		
		/*try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		//Date date = DateUtils.getDateWithoutTime(new Date());
		//Date date = teleInfoService.getLastBillDate();
		//Date dateTo = DateUtils.addDays(date,6);
		
		//System.out.println(teleInfoService.GetNextElectricityBillCost());
		
	}
}