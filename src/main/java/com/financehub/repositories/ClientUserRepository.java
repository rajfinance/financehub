package com.financehub.repositories;

import com.financehub.entities.ClientUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientUserRepository extends JpaRepository<ClientUser, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<ClientUser> findByUsername(String username);
}
