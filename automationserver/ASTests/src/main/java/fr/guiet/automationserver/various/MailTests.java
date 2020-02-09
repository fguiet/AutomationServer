package fr.guiet.automationserver.various;

import fr.guiet.automationserver.business.service.MailService;

public class MailTests {
	public static void main(String args[]) {

		MailService mailService = new MailService();
		mailService.SendMailSSL("AutomationServer - Error sending SMS", "Test from automationserver");

	}
}