package com.example.book.Model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordChangeDto {

	private String oldPassword;
	private String newPassword;

	// Constructor por defecto
	public PasswordChangeDto() {
	}

	@JsonCreator
	public PasswordChangeDto(@JsonProperty("oldPassword") String oldPassword,
	                         @JsonProperty("newPassword") String newPassword) {
		this.oldPassword = oldPassword;
		this.newPassword = newPassword;
	}

	// Getters y Setters
	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
}
