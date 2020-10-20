package fr.guiet.automationserver.business.helper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateUtils {
	
	/*private static long millisToNextHour(Calendar calendar) {
		
	    int minutes = calendar.get(Calendar.MINUTE);
	    int seconds = calendar.get(Calendar.SECOND);
	    int millis = calendar.get(Calendar.MILLISECOND);
	    int minutesToNextHour = 60 - minutes;
	    int secondsToNextHour = 60 - seconds;
	    int millisToNextHour = 1000 - millis;
	    
	    return minutesToNextHour*60*1000 + secondsToNextHour*1000 + millisToNextHour;
	    
	}*/

	public static Date addDays(Date date, int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, days); // minus number would decrement the days
		return cal.getTime();
	}
	
	public static Date getTomorrowMorning1AM() {

		Calendar c1 = Calendar.getInstance();

		c1.add(GregorianCalendar.DAY_OF_MONTH, 1);
		c1.set(Calendar.HOUR_OF_DAY, 2);
		c1.set(Calendar.MINUTE, 0);
		c1.set(Calendar.SECOND, 0);

		return c1.getTime();
	}

	public static Date getFirstDateOfCurrentMonth() {
		// Create a Calendar instance
		// and set it to the date of interest

		Calendar cal = Calendar.getInstance();
		cal.setTime(getDateWithoutTime(new Date()));

		// Set the day of the month to the first day of the month

		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));

		// Extract the Date from the Calendar instance

		return cal.getTime();
	}

	public static String GetDayName(int day) {

		String dayString = "NA";

		switch (day) {
		case 2:
			dayString = "Lundi";
			break;

		case 3:
			dayString = "Mardi";
			break;

		case 4:
			dayString = "Mercredi";
			break;

		case 5:
			dayString = "Jeudi";
			break;

		case 6:
			dayString = "Vendredi";
			break;

		case 7:
			dayString = "Samedi";
			break;

		case 1:
			dayString = "Dimanche";
			break;
		}

		return dayString;
	}

	public static boolean isTimeBetweenTwoTime(String initialTime, String finalTime, String currentTime)
			throws ParseException {
		String reg = "^([0-1][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$";
		if (initialTime.matches(reg) && finalTime.matches(reg) && currentTime.matches(reg)) {
			boolean valid = false;
			// Start Time
			java.util.Date inTime = new SimpleDateFormat("HH:mm:ss").parse(initialTime);
			Calendar calendar1 = Calendar.getInstance();
			calendar1.setTime(inTime);

			// Current Time
			java.util.Date checkTime = new SimpleDateFormat("HH:mm:ss").parse(currentTime);
			Calendar calendar3 = Calendar.getInstance();
			calendar3.setTime(checkTime);

			// End Time
			java.util.Date finTime = new SimpleDateFormat("HH:mm:ss").parse(finalTime);
			Calendar calendar2 = Calendar.getInstance();
			calendar2.setTime(finTime);

			if (finalTime.compareTo(initialTime) < 0) {
				calendar2.add(Calendar.DATE, 1);
				calendar3.add(Calendar.DATE, 1);
			}

			java.util.Date actualTime = calendar3.getTime();
			if ((actualTime.after(calendar1.getTime()) || actualTime.compareTo(calendar1.getTime()) == 0)
					&& actualTime.before(calendar2.getTime())) {
				valid = true;
			}
			return valid;
		} else {
			throw new IllegalArgumentException("Not a valid time, expecting HH:MM:SS format");
		}

	}

	public static String getDateToString(Date date) {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy à HH:mm:ss");
		return df.format(date);
	}

	public static Long betweenDates(Date firstDate, Date secondDate) throws IOException {
		return ChronoUnit.DAYS.between(firstDate.toInstant(), secondDate.toInstant());
	}

	public static Long minutesBetweenDate(Date firstDate, Date secondDate) {
		return ChronoUnit.MINUTES.between(firstDate.toInstant(), secondDate.toInstant());
	}
	
	public static Long secondsBetweenDate(Date firstDate, Date secondDate) {
		return ChronoUnit.SECONDS.between(firstDate.toInstant(), secondDate.toInstant());
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

	public static String getTimeFromCurrentDate() {

		// String result = "error";
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		Date dateobj = new Date();

		// try {
		return df.format(dateobj);

		// } catch (ParseException e) {

		// }

		// return result;
	}

	public static int getCurrentHour() {
		Date date = new Date(); // given date
		Calendar calendar = GregorianCalendar.getInstance(); // creates a new
																// calendar
																// instance
		calendar.setTime(date); // assigns calendar to given date
		return calendar.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format
	}
}