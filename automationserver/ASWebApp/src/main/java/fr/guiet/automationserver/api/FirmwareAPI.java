package fr.guiet.automationserver.api;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sun.jersey.api.client.ClientResponse.Status;

import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SensorDto;

@Path("/firmware")
public class FirmwareAPI {
	
	private static Logger _logger = Logger.getLogger(FirmwareAPI.class);
	
	@GET	
	@Path("/getversion/{sensorid}")
	@Produces({ MediaType.APPLICATION_JSON})	
	public Response getVersion(@PathParam("sensorid") String sensorid) {
		
		try {
			_logger.info("Sensor with ID : "+sensorid + " asked for last firmware version");
			
			JSONObject obj = new JSONObject();
			
			DbManager dbManager = new DbManager();
			SensorDto dto = dbManager.GetSensorById(Long.parseLong(sensorid));
			
			if ( dto !=null ) {
				obj.put("sensorid", sensorid);
				obj.put("lastversion", dto.firmware_version);
			}
			else {
				obj.put("sensorid", sensorid);
		        obj.put("lastversion", -1);
			}		
			
			return Response.status(Status.OK).entity(obj.toString()).build();
		}
		catch(Exception e) {
			_logger.error("Error lors de la récupération des infos du capteur",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("/getfirmware/{sensorid}/{version}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFirmware(@PathParam("sensorid") String sensorid, @PathParam("version") String version) {
		
		_logger.info("Sensor ID : "+sensorid+" upgrading to firmware version : "+version);
		
		String fileName = "firmware_sensorid_"+sensorid+"_version_"+version+".bin";
		File file = new File ("/home/fred/automationserver/firmwares/" + fileName);
		
		ResponseBuilder response = Response.ok((Object) file); 
		response.header("Content-Disposition", "attachment; filename="
                + file.getName());
		
		return response.build();
				
	}


}