package fr.guiet.automationserver.business.helper;

public class ParseUtils {

	public static Float tryFloatParse(String value) {
		Float retVal;
		try {
			retVal = Float.parseFloat(value);
		} catch (NumberFormatException nfe) {
			retVal = null; // or null if that is your preference
		}
		return retVal;
	}
	
	public static Integer tryIntParse(String value) {
		Integer retVal;
		try {
			retVal = Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			retVal = null; // or null if that is your preference
		}
		return retVal;
	}
}