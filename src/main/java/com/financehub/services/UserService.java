package com.financehub.services;

import com.financehub.dtos.ClientUserDTO;
import com.financehub.entities.ClientUser;
import com.financehub.repositories.ClientUserRepository;
import com.financehub.security.ClientUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile(
			"^[\\w.!#$%&'*+/=?^`{|}~-]+@[\\w-]+(?:\\.[\\w-]+)+$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{6,32}$");

	private final ClientUserRepository clientUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProfilePhotoProcessor profilePhotoProcessor;

	public UserService(ClientUserRepository clientUserRepository, PasswordEncoder passwordEncoder,
			ProfilePhotoProcessor profilePhotoProcessor) {
		this.clientUserRepository = clientUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.profilePhotoProcessor = profilePhotoProcessor;
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
		newUser.setPhone(clientUserDTO.getPhone().trim());
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

	public Optional<ClientUser> getCurrentClientUser() {
		long uid = getUserId();
		if (uid <= 0) {
			return Optional.empty();
		}
		return clientUserRepository.findById(uid);
	}

	/**
	 * Updates email, phone, and optionally profile photo (or clears it) in one transaction.
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateProfile(String emailRaw, String phoneRaw, MultipartFile photo, boolean removePhoto)
			throws IOException {
		long uid = getUserId();
		if (uid <= 0) {
			throw new IllegalStateException("Not signed in.");
		}
		String email = requireNonBlank(emailRaw, "Email is required.");
		String phone = requireNonBlank(phoneRaw, "Phone is required.");
		if (!EMAIL_PATTERN.matcher(email).matches()) {
			throw new IllegalArgumentException("Please enter a valid email address.");
		}
		if (!PHONE_PATTERN.matcher(phone).matches()) {
			throw new IllegalArgumentException(
					"Phone must be 6–32 characters: digits and + - ( ) spaces only.");
		}

		ClientUser user = clientUserRepository.findById(uid).orElseThrow();
		if (!email.equalsIgnoreCase(user.getEmail())
				&& clientUserRepository.existsAnotherUserWithEmail(email, user.getId())) {
			throw new IllegalArgumentException("That email is already used by another account.");
		}
		if (!phone.equals(user.getPhone())
				&& clientUserRepository.existsAnotherUserWithPhone(phone, user.getId())) {
			throw new IllegalArgumentException("That phone number is already used by another account.");
		}
		user.setEmail(email);
		user.setPhone(phone);

		if (removePhoto) {
			user.setProfilePhoto(null);
			user.setProfilePhotoContentType(null);
		} else if (photo != null && !photo.isEmpty()) {
			byte[] jpeg = profilePhotoProcessor.processUpload(photo);
			user.setProfilePhoto(jpeg);
			user.setProfilePhotoContentType("image/jpeg");
		}

		user.setUpdatedAt(LocalDateTime.now());
		clientUserRepository.saveAndFlush(user);
	}

	public Long getProfileAvatarVersionForCurrentUser() {
		long uid = getUserId();
		if (uid <= 0) {
			return null;
		}
		return clientUserRepository.findUpdatedAtById(uid)
				.map(t -> t.atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
				.orElse(0L);
	}

	private static String requireNonBlank(String value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		String t = value.trim();
		if (t.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
		return t;
	}
}
