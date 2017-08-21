package fr.guiet.automationserver.restful;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import fr.guiet.automationserver.business.MqttClientMgt;
import org.apache.log4j.Logger;

@Path("/mqtt")
public class MqttService {
	
	private static Logger _logger = Logger.getLogger(MqttService.class);
	private static String _topic = "/guiet/googleassistant";
	
	@GET	
	public Response sendMsg(@Context UriInfo info) {
		
		String msg = info.getQueryParameters().getFirst("msg");
		
		_logger.info("Received Google Assistant Message : "+msg);
		
		String [] messageContent = msg.split(";");
		
		if (messageContent != null && messageContent.length > 0) {
			String action = messageContent[0];
			
			switch(action) {
			case "SETROOMTEMP":
				String roomName = messageContent[1].toUpperCase(); 
				String temp = messageContent[2];
				String roomId = "";
				
				switch(roomName) {
				case "BUREAU":
					roomId="1";
					break;
				case "SALON":
					roomId="2";
					break;
				case "NOE":
					roomId="3";
					break;
				case "MANON":
					roomId="4";
					break;
				case "PARENT":
					roomId="5";
					break;
				}
				
				
			    String message = action + ";" + roomId + ";" + temp;
				
				MqttClientMgt rtt = new MqttClientMgt();
				rtt.SendMsg(_topic, message);
				break;
			}
		}	
				

		return Response.ok().build();

	}


}