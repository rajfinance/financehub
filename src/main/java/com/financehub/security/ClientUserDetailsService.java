package com.financehub.security;

import com.financehub.entities.ClientUser;
import com.financehub.repositories.ClientUserRepository;
import com.financehub.utils.UsernameDisplayUtils;

import java.time.ZoneOffset;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ClientUserDetailsService implements UserDetailsService {

	private final ClientUserRepository clientUserRepository;

	public ClientUserDetailsService(ClientUserRepository clientUserRepository) {
		this.clientUserRepository = clientUserRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		ClientUser user = clientUserRepository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		long avatarVersion = user.getUpdatedAt() == null ? 0L
				: user.getUpdatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
		return new ClientUserPrincipal(
				user.getId(),
				user.getUsername(),
				user.getUsrPassword(),
				UsernameDisplayUtils.toDisplayName(user.getUsername()),
				UsernameDisplayUtils.toDisplayFullName(
						user.getFirstName(), user.getLastName(), user.getUsername()),
				avatarVersion);
	}
}
