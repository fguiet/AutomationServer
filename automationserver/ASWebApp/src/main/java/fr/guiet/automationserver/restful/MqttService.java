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
				
				if (roomName.contains("BUREAU")) {								
					roomId="1";
				}
				
				if (roomName.contains("SALON")) {
					roomId="2";
				}
				
				if (roomName.contains("NOE")) {
					roomId="3";
				}
				
				if (roomName.contains("MANON")) {
					roomId="4";
				}
				
				if (roomName.contains("PARENT")) {
					roomId="5";
				}				
				
				if (!roomId.equals("")) {
				
					String message = action + ";" + roomId + ";" + temp;
				
					MqttClientMgt rtt = new MqttClientMgt();
					rtt.SendMsg(_topic, message);
				}
				break;
			}
		}	
				

		return Response.ok().build();

	}


}