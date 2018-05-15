package org.fc.brewchain.bcapi;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class JodaTimeHelper {
	static DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd'Z'HHmmss");

	public static String current() {
		return fmt.print(System.currentTimeMillis());
	}
	
	public static String format(long time) {
		return fmt.print(time);
	}
	
	public static String secondFromNow(long time) {
		return ""+Seconds.secondsBetween(new DateTime(time), new DateTime()).getSeconds();
	}
	
	public static int secondIntFromNow(long time) {
		return Seconds.secondsBetween(new DateTime(time), new DateTime()).getSeconds();
	}
	
	public static void main(String[] args) {
		long start=System.currentTimeMillis();
		System.out.println(current());
		try {
			Thread.sleep(10*1000);
			System.out.println("dis="+secondFromNow(start));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
}
