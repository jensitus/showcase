package org.service_b.workflow.shared.service;

public interface MailService {
    void send(String to, String subject, String htmlContent);
}
