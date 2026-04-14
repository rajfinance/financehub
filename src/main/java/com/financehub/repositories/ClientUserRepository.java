package com.financehub.repositories;

import com.financehub.entities.ClientUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ClientUserRepository extends JpaRepository<ClientUser, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<ClientUser> findByUsername(String username);

    Optional<ClientUser> findByUsernameAndEmail(String username, String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM ClientUser u WHERE LOWER(u.email) = LOWER(:email) AND u.id <> :id")
    boolean existsAnotherUserWithEmail(@Param("email") String email, @Param("id") int id);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM ClientUser u WHERE u.phone = :phone AND u.id <> :id")
    boolean existsAnotherUserWithPhone(@Param("phone") String phone, @Param("id") int id);

    @Query("SELECT u.updatedAt FROM ClientUser u WHERE u.id = :id")
    Optional<LocalDateTime> findUpdatedAtById(@Param("id") long id);
}
