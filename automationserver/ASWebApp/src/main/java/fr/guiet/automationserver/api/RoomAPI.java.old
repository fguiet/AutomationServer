package fr.guiet.automationserver.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import fr.guiet.automationserver.business.helper.MqttClientHelper;

@Path("/room")
public class RoomAPI {
	
	private static Logger _logger = Logger.getLogger(RoomAPI.class);
	private static String _topic = "/guiet/api/home";
	private static String _mqttClientId = "RoomAPIClientId";
	
	@GET	
	public Response sendMsg(@Context UriInfo info) {
		
		
		String action = info.getQueryParameters().getFirst("action");
				
		//_logger.info("Received Google Assistant Message : "+msg);
		
		//String [] messageContent = msg.split(";");
		
		//if (messageContent != null && messageContent.length > 0) {
			//String action = messageContent[0];
			
			switch(action) {
			case "SETROOMTEMP":
									
				String roomId = info.getQueryParameters().getFirst("roomId");				
				String temp = info.getQueryParameters().getFirst("temp");
				
				_logger.info("RoomAPI receives action request : " + action + ", roomId : "+roomId+", temperature :"+temp);
						
				String message = action + ";" + roomId + ";" + temp;
			
				MqttClientHelper rtt = new MqttClientHelper(_mqttClientId);
				rtt.SendMsg(_topic, message);
				
				break;
			}
		
				

		return Response.ok().build();

	}


}
