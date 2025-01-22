package com.financehub.services;

import com.financehub.dtos.ClientUserDTO;
import com.financehub.entities.ClientUser;
import com.financehub.repositories.ClientUserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private ClientUserRepository clientUserRepository;
    @Autowired
    private HttpSession session;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public Long getUserId(){
        String username = (String) session.getAttribute("username");
        Optional<ClientUser> userOptional = clientUserRepository.findByUsername(username);
        return Long.valueOf(userOptional.map(ClientUser::getId).orElse(0));
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


        String hashedPassword = passwordEncoder.encode(clientUserDTO.getPassword());
        ClientUser newUser = new ClientUser();
        newUser.setUsername(clientUserDTO.getUsername());
        newUser.setEmail(clientUserDTO.getEmail());
        newUser.setUsrPassword(hashedPassword);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        clientUserRepository.save(newUser);

        response.put("success", "Signup successful!");
        return response;
    }

    public boolean authenticate(String username, String password) {
        Optional<ClientUser> optionalUser = clientUserRepository.findByUsername(username);
        return optionalUser
                .map(user -> passwordEncoder.matches(password, user.getUsrPassword()))
                .orElse(false);
    }
}
