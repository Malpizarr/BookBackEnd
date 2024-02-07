package com.example.book.Model;

public class UserDto {
	private String id;
	private String username;

	private String photoUrl; // Campo para la URL de la foto del usuario

	// Constructor, getters y setters

	public UserDto(String id, String username, String photoUrl) {
		this.id = id;
		this.username = username;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	// Getters
	public String getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	// Setters
	public void setId(String id) {
		this.id = id;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}

