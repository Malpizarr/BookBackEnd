package com.example.book.Model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {
    private String userId;

	private String username; // Campo para la URL de la foto del usuario

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, String userId) {
        super(username, password, authorities);
        this.userId = userId;
	    this.username = username;
    }

    public String getUserId() {
        return userId;
    }

	public String getUsername() {
		return username;
	}


}
