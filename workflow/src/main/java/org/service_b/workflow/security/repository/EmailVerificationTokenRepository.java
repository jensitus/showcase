package org.service_b.workflow.security.repository;

import org.service_b.workflow.security.entity.EmailVerificationToken;
import org.service_b.workflow.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByUser(User user);
    
    void deleteByUser(User user);
}
