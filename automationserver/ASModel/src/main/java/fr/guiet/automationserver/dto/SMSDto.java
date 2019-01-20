package fr.guiet.automationserver.dto;

import java.util.UUID;

/**
 * SMS DTO
 * 
 * @author guiet
 *
 */
public class SMSDto {
	
	private String _recipient;
	private String _message;
	private UUID _messageId;
	
	public SMSDto(UUID messageId) {
		_messageId = messageId;
	}
	
	public UUID getId() {
		return _messageId;
	}
	
	public String getRecipient() {
		return _recipient;
	}
	public void setRecipient(String recipient) {
		this._recipient = recipient;
	}
	
	public String getMessage() {
		return _message;
	}
	
	public void setMessage(String message) {
		this._message = message;
	}
		
	
}