package fr.guiet.automationserver.various;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateTimeTests {
	public static void main(String args[]) {


		/*Calendar cal = Calendar.getInstance(); 
		cal.add(Calendar.MONTH, -1);
		java.util.Date dt = cal.getTime();

		System.out.println(dt);
		
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String dfd = df.format(convertDate(dt, "Europe/Paris","UTC"));
			System.out.println(dfd);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		//private static Date getTomorrowMorning1AM() {

			Calendar c1 = Calendar.getInstance();
			
			c1.add(GregorianCalendar.DAY_OF_MONTH, 1);		
			c1.set(Calendar.HOUR_OF_DAY, 2);
			c1.set(Calendar.MINUTE, 0);
			c1.set(Calendar.SECOND, 0);

			System.out.println(getDateToString(c1.getTime()));

			//return c1.getTime();
		
		
	}
	
	public static String getDateToString(Date date) {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy à HH:mm:ss");
		return df.format(date);
	}
	
	public static Date convertDate(Date dateFrom, String fromTimeZone, String toTimeZone) throws ParseException {
        String pattern = "yyyy/MM/dd HH:mm:ss";
        SimpleDateFormat sdfFrom = new SimpleDateFormat (pattern);
        sdfFrom.setTimeZone(TimeZone.getTimeZone(fromTimeZone));

        SimpleDateFormat sdfTo = new SimpleDateFormat (pattern);
        sdfTo.setTimeZone(TimeZone.getTimeZone(toTimeZone));

        Date dateTo = sdfFrom.parse(sdfTo.format(dateFrom));
        return dateTo;
    }
}

