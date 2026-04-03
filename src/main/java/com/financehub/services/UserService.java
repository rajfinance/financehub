package com.financehub.services;

import com.financehub.dtos.ClientUserDTO;
import com.financehub.entities.ClientUser;
import com.financehub.repositories.ClientUserRepository;
import com.financehub.security.ClientUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

	private final ClientUserRepository clientUserRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(ClientUserRepository clientUserRepository, PasswordEncoder passwordEncoder) {
		this.clientUserRepository = clientUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public long getUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof ClientUserPrincipal p) {
			return p.getUserId();
		}
		return 0L;
	}

	public void updatePasswordByUserId(int userId, String rawPassword) {
		ClientUser user = clientUserRepository.findById((long) userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
		user.setUsrPassword(passwordEncoder.encode(rawPassword));
		user.setUpdatedAt(LocalDateTime.now());
		clientUserRepository.save(user);
	}

	public boolean changePasswordForCurrentUser(String currentPassword, String newPassword) {
		long uid = getUserId();
		if (uid <= 0) {
			return false;
		}
		ClientUser user = clientUserRepository.findById(uid).orElseThrow();
		if (!passwordEncoder.matches(currentPassword, user.getUsrPassword())) {
			return false;
		}
		user.setUsrPassword(passwordEncoder.encode(newPassword));
		user.setUpdatedAt(LocalDateTime.now());
		clientUserRepository.save(user);
		return true;
	}

	public Map<String, String> handleSignup(ClientUserDTO clientUserDTO) {
		Map<String, String> response = new HashMap<>();
		if (clientUserRepository.existsByUsername(clientUserDTO.getUsername())) {
			response.put("error", "Username already exists.");
			return response;
		}
		if (clientUserRepository.existsByEmail(clientUserDTO.getEmail())) {
			response.put("error", "Email already exists.");
			return response;
		}

		ClientUser newUser = new ClientUser();
		newUser.setUsername(clientUserDTO.getUsername());
		newUser.setEmail(clientUserDTO.getEmail());
		newUser.setUsrPassword(passwordEncoder.encode(clientUserDTO.getPassword()));
		newUser.setCreatedAt(LocalDateTime.now());
		newUser.setUpdatedAt(LocalDateTime.now());

		clientUserRepository.save(newUser);

		response.put("success", "Signup successful!");
		return response;
	}

	public Optional<ClientUser> findByUsernameAndEmail(String username, String email) {
		return clientUserRepository.findByUsernameAndEmail(username, email);
	}
}
