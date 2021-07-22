package fr.guiet.automationserver.various;

import org.json.JSONObject;

public class JsonTests {
	
	
	public static void main(String args[]) {

				
		String message ="{\"applicationID\":\"2\",\"applicationName\":\"home-application\",\"deviceName\":\"watermeter-device\",\"devEUI\":\"qQfCJKaFvUM=\",\"rxInfo\":[{\"gatewayID\":\"3KYy//42XZw=\",\"time\":\"2021-05-16T12:07:13.077809Z\",\"timeSinceGPSEpoch\":\"1305202052.077s\",\"rssi\":-83,\"loRaSNR\":9,\"channel\":0,\"rfChain\":1,\"board\":0,\"antenna\":0,\"location\":{\"latitude\":0,\"longitude\":0,\"altitude\":0,\"source\":\"UNKNOWN\",\"accuracy\":0},\"fineTimestampType\":\"NONE\",\"context\":\"Ni1pkw==\",\"uplinkID\":\"9blz8UGZR8i0opkJFHZPVg==\",\"crcStatus\":\"CRC_OK\"}],\"txInfo\":{\"frequency\":868100000,\"modulation\":\"LORA\",\"loRaModulationInfo\":{\"bandwidth\":125,\"spreadingFactor\":7,\"codeRate\":\"4/5\",\"polarizationInversion\":false}},\"adr\":true,\"dr\":5,\"fCnt\":16872,\"fPort\":1,\"data\":\"MTkgMi43IDQuMTggMSAxODExOA==\",\"objectJSON\":\"{\\\"data\\\":\\\"19 2.7 4.18 1 18118\\\"}\",\"tags\":{},\"confirmedUplink\":false,\"devAddr\":\"JgER9A==\"}";
		
		JSONObject json = new JSONObject(message);
		JSONObject jsonObj = new JSONObject(json.getString("objectJSON"));
		String data = jsonObj.getString("data");
		
		System.out.println(data);

	}
}