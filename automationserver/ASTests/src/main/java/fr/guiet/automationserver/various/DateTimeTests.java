package fr.guiet.automationserver.various;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeTests {
	public static void main(String args[]) {


		Calendar cal = Calendar.getInstance(); 
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
		}
		
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

