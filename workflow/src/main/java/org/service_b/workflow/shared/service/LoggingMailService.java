package org.service_b.workflow.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!staging & !prod")
@Slf4j
public class LoggingMailService implements MailService {

    @Override
    public void send(String to, String subject, String htmlContent) {
        log.info("""
                ==================== EMAIL (not sent locally) ====================
                To:      {}
                Subject: {}
                Body:
                {}
                ===================================================================
                """, to, subject, htmlContent);
    }
}
