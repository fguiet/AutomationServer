package fr.guiet.automationserver.various;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClasspathTests {
	public static void main(String args[]) {

		Path classpath;
		try {
			classpath = Paths.get(ClasspathTests.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			System.out.println("Class path : " + classpath);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}