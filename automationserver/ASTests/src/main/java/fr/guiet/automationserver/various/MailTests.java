package fr.guiet.automationserver.various;

import java.math.BigInteger;

import fr.guiet.automationserver.business.helper.*;

public class MailTests {
	public static void main(String args[]) {

		byte[] appKey = new byte[] { (byte)0xC3, 0x0B, (byte)0xEA, (byte)0xEE, (byte)0xE8, (byte)0xBA, 0x50, (byte)0xA8, 0x31, 0x5B, 0x1E, 0x05, (byte)0x9B, (byte)0xAA, (byte)0xE0, (byte)0xB8 };
		//appKey =  new BigInteger("A2E9B32DE6CEA07370A521ABCF51882A", 16).toByteArray();
		LoRaDecrypter ld = new LoRaDecrypter(appKey);
				
		byte[] result = ld.decrypt("QPQRASaABwABtTJ7ZAP3df8A7P8qX5XqiyfScV5t");
		String s = new String(result);
		
		String[] messageContent = s.trim().split(" ");
		
		/*byte[] lorawan = null;
		
		try {
			lorawan = Base64.getDecoder().decode(new String("QPQRASaADQABxuQg6a3bunLrey2RPSQckXOKY+c5RA==").getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//byte[] lorawan = new BigInteger("A2E9B32DE6CEA07370A521ABCF51882A", 16).toByteArray();
	    byte[] nwkSKey2 =  new BigInteger("A2E9B32DE6CEA07370A521ABCF51882A", 16).toByteArray();
		byte[] nwkSKey = new byte[] { (byte)0xA2, (byte)0xE9, (byte)0xB3, 0x2D, (byte)0xE6, (byte)0xCE, (byte)0xA0, 0x73, 0x70, (byte)0xA5, 0x21, (byte)0xAB, (byte)0xCF, 0x51, (byte)0x88, 0x2A };
	    byte[] appSKey = new byte[] { (byte)0xC3, 0x0B, (byte)0xEA, (byte)0xEE, (byte)0xE8, (byte)0xBA, 0x50, (byte)0xA8, 0x31, 0x5B, 0x1E, 0x05, (byte)0x9B, (byte)0xAA, (byte)0xE0, (byte)0xB8 };	    
	    
	    try {
			PhyPayload pp = new PhyPayload(ByteBuffer.wrap(lorawan));
			//System.out.println(pp.getClass());
			MacPayload pl =  pp.getMacPayload();
								    		    
		    DataPayload data = new DataPayload(pl, ByteBuffer.wrap(lorawan));
		   		    
		    data.setAppSKey(appSKey);
		    data.setNwkSKey(nwkSKey);
		    
		    byte[] clearData = data.getClearPayLoad(); 
		    
		    String s = new String(clearData);
			
		    String toto = "tti";
		    
		} catch (MalformedPacketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	    
	    

	}
}