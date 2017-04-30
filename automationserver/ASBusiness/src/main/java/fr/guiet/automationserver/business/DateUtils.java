package fr.guiet.automationserver.business;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils
{
    public static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }
    
    public static String getDateToString(Date date) {
    	DateFormat df = new SimpleDateFormat("dd/MM/yyyy à HH:mm:ss");
    	return df.format(date);
    }
    
    public static Date parseDate(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (ParseException e) {
            return null;
        }
     }
    
    public static Date getDateWithoutTime(Date date) {
    	
    	Date date1 = null;
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    	
    	try {
			date1 = sdf.parse(sdf.format(date));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return date1;
    }
}