package com.financehub.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class ClientUserPrincipal implements UserDetails {

	private final int userId;
	private final String username;
	private final String passwordHash;
	private final String displayUsername;
	private final String displayFullName;
	private final long profileAvatarVersion;

	public ClientUserPrincipal(int userId, String username, String passwordHash,
			String displayUsername, String displayFullName, long profileAvatarVersion) {
		this.userId = userId;
		this.username = username;
		this.passwordHash = passwordHash;
		this.displayUsername = displayUsername;
		this.displayFullName = displayFullName;
		this.profileAvatarVersion = profileAvatarVersion;
	}

	public String getDisplayUsername() {
		return displayUsername;
	}

	public String getDisplayFullName() {
		return displayFullName;
	}

	public long getProfileAvatarVersion() {
		return profileAvatarVersion;
	}

	public int getUserId() {
		return userId;
	}

	public long getUserIdAsLong() {
		return userId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
