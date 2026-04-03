package com.financehub.security;

import com.financehub.entities.ClientUser;
import com.financehub.repositories.ClientUserRepository;
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
		ClientUser user = clientUserRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		return new ClientUserPrincipal(user.getId(), user.getUsername(), user.getUsrPassword());
	}
}
