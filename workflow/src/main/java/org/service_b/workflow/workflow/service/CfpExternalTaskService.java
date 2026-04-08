package org.service_b.workflow.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.shared.service.MailService;
import org.service_b.workflow.submission.persistence.SubmissionState;
import org.service_b.workflow.submission.service.SubmissionService;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.config.ExternalTaskConfig;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CfpExternalTaskService extends AbstractExternalTaskService {

    private final SubmissionService submissionService;
    private final MailService mailService;
    private final Map<String, ExternalTaskConfig.TaskDefinition> taskDefinitions;

    public CfpExternalTaskService(FetchAndLockService fetchAndLockService,
                                   CibSevenProperties cibSevenProperties,
                                   CibSevenRestClient cibSevenRestClient,
                                   SubmissionService submissionService,
                                   MailService mailService) {
        super(fetchAndLockService, cibSevenProperties, cibSevenRestClient);
        this.submissionService = submissionService;
        this.mailService = mailService;
        this.taskDefinitions = initializeTaskDefinitions();
    }

    private Map<String, ExternalTaskConfig.TaskDefinition> initializeTaskDefinitions() {
        Map<String, ExternalTaskConfig.TaskDefinition> defs = new HashMap<>();
        defs.put("notifyRejection", new ExternalTaskConfig.TaskDefinition(
                "notify_author_rejection", "cfp", this::handleNotifyRejection));
        defs.put("notifyAcceptance", new ExternalTaskConfig.TaskDefinition(
                "notify_author_acceptance", "cfp", this::handleNotifyAcceptance));
        defs.put("sendRegReminder", new ExternalTaskConfig.TaskDefinition(
                "send_registration_reminder", "cfp", this::handleSendRegReminder));
        defs.put("sendConfirmReminder", new ExternalTaskConfig.TaskDefinition(
                "send_confirmation_reminder", "cfp", this::handleSendConfirmReminder));
        defs.put("mockNoResponse", new ExternalTaskConfig.TaskDefinition(
                "response_after_reminder", "cfp", this::handleMockNoResponse));
        return defs;
    }

    @Scheduled(fixedRate = 30000)
    public void notifyRejection() {
        processExternalTask(taskDefinitions, "notifyRejection");
    }

    @Scheduled(fixedRate = 30000)
    public void notifyAcceptance() {
        processExternalTask(taskDefinitions, "notifyAcceptance");
    }

    @Scheduled(fixedRate = 30000)
    public void sendRegReminder() {
        processExternalTask(taskDefinitions, "sendRegReminder");
    }

    @Scheduled(fixedRate = 30000)
    public void sendConfirmReminder() {
        processExternalTask(taskDefinitions, "sendConfirmReminder");
    }

    @Scheduled(fixedRate = 30000)
    public void mockNoResponse() {
        processExternalTask(taskDefinitions, "mockNoResponse");
    }

    private Map<String, Object> handleNotifyAcceptance(Map<String, Map<String, Object>> variables) {
        UUID submissionId = UUID.fromString((String) variables.get("submissionId").get("value"));

        String submitterEmail   = strVar(variables, "submitterEmail");
        String title            = strVarOr(variables, "title",                 "your submission");
        String format           = strVarOr(variables, "presentation_format",   "TBD");
        String sessionName      = strVarOr(variables, "session_name",          "TBD");
        String sessionDatetime  = strVarOr(variables, "session_datetime",      "TBD");
        String sessionRoom      = strVarOr(variables, "session_room",          "TBD");
        String confirmDeadline  = strVarOr(variables, "confirmation_deadline", "TBD");
        String regDeadline      = strVarOr(variables, "registration_deadline", "TBD");
        String uploadDeadline   = strVarOr(variables, "upload_deadline",       "TBD");

        submissionService.updateState(submissionId, SubmissionState.ACCEPTED);

        if (submitterEmail != null) {
            mailService.send(submitterEmail,
                             "Your abstract has been accepted",
                             buildAcceptanceEmail(title, format, sessionName, sessionDatetime,
                                                  sessionRoom, confirmDeadline, regDeadline, uploadDeadline));
        }
        return new HashMap<>();
    }

    private Map<String, Object> handleNotifyRejection(Map<String, Map<String, Object>> variables) {
        UUID submissionId = UUID.fromString((String) variables.get("submissionId").get("value"));

        String submitterEmail = strVar(variables, "submitterEmail");
        String title          = strVarOr(variables, "title", "your submission");

        submissionService.updateState(submissionId, SubmissionState.REJECTED);

        if (submitterEmail != null) {
            mailService.send(submitterEmail,
                             "Update on your abstract submission",
                             buildRejectionEmail(title));
        }
        return new HashMap<>();
    }

    private Map<String, Object> handleSendConfirmReminder(Map<String, Map<String, Object>> variables) {
        String submitterEmail  = strVar(variables, "submitterEmail");
        String title           = strVarOr(variables, "title",                 "your submission");
        String confirmDeadline = strVarOr(variables, "confirmation_deadline", "TBD");
        String sessionName     = strVarOr(variables, "session_name",          "TBD");
        String sessionDatetime = strVarOr(variables, "session_datetime",      "TBD");

        if (submitterEmail != null) {
            mailService.send(submitterEmail,
                             "Reminder: Please confirm your presentation",
                             buildConfirmReminderEmail(title, sessionName, sessionDatetime, confirmDeadline));
        }
        return new HashMap<>();
    }

    private Map<String, Object> handleSendRegReminder(Map<String, Map<String, Object>> variables) {
        String submitterEmail  = strVar(variables, "submitterEmail");
        String title           = strVarOr(variables, "title",                 "your submission");
        String regDeadline     = strVarOr(variables, "registration_deadline", "TBD");
        String sessionDatetime = strVarOr(variables, "session_datetime",      "TBD");
        String sessionName     = strVarOr(variables, "session_name",          "TBD");

        if (submitterEmail != null) {
            mailService.send(submitterEmail,
                             "Reminder: Conference registration required",
                             buildRegReminderEmail(title, sessionName, sessionDatetime, regDeadline));
        }
        return new HashMap<>();
    }

    private Map<String, Object> handleMockNoResponse(Map<String, Map<String, Object>> variables) {
        Map<String, Object> result = new HashMap<>();
        result.put("author_confirmed", false);
        return result;
    }

    // ── Email builders ────────────────────────────────────────────────────────

    private String buildAcceptanceEmail(String title, String format,
                                        String sessionName, String sessionDatetime, String sessionRoom,
                                        String confirmDeadline, String regDeadline, String uploadDeadline) {
        String formatLabel = switch (format) {
            case "ORAL"           -> "Oral Presentation (10–12 min + 3 min Q&A)";
            case "POSTER"         -> "Poster Presentation (2–3 hour poster session)";
            case "E_POSTER"       -> "E-Poster / Digital Poster (displayed on LCD kiosks)";
            case "LIGHTNING_TALK" -> "Rapid-Fire / Lightning Talk (3–5 min)";
            case "LATE_BREAKING"  -> "Late-Breaking Science Session (12 min + 3 min Q&A)";
            default               -> format;
        };
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Congratulations — Your Abstract Has Been Accepted!</h2>
                    <p>We are pleased to inform you that your abstract <strong>"%s"</strong> has been accepted for presentation at our conference.</p>

                    <h3>Presentation Details</h3>
                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr><td style="padding: 6px; color: #555;">Format</td><td style="padding: 6px;"><strong>%s</strong></td></tr>
                        <tr><td style="padding: 6px; color: #555;">Session</td><td style="padding: 6px;">%s</td></tr>
                        <tr><td style="padding: 6px; color: #555;">Date &amp; Time</td><td style="padding: 6px;">%s</td></tr>
                        <tr><td style="padding: 6px; color: #555;">Room / Hall</td><td style="padding: 6px;">%s</td></tr>
                    </table>

                    <h3>Important Deadlines</h3>
                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr><td style="padding: 6px; color: #555;">Confirm intention to present</td><td style="padding: 6px;"><strong>%s</strong></td></tr>
                        <tr><td style="padding: 6px; color: #555;">Conference registration</td><td style="padding: 6px;"><strong>%s</strong></td></tr>
                        <tr><td style="padding: 6px; color: #555;">Slide / poster upload</td><td style="padding: 6px;"><strong>%s</strong></td></tr>
                    </table>

                    <p style="margin-top: 20px;">Please confirm your intention to present by the deadline above. Non-response by the deadline may result in removal from the programme.</p>
                    <p>If you need to request a format change or substitute presenter, please contact us in writing before the confirmation deadline.</p>
                    <p>We look forward to seeing you at the conference!</p>
                </body>
                </html>
                """, title, formatLabel, sessionName, sessionDatetime, sessionRoom,
                     confirmDeadline, regDeadline, uploadDeadline);
    }

    private String buildRejectionEmail(String title) {
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Update on Your Abstract Submission</h2>
                    <p>Thank you for submitting your abstract <strong>"%s"</strong> to our conference.</p>
                    <p>After careful review by our scientific committee, we regret to inform you that your abstract was not selected for presentation this year.</p>
                    <p>We received a large number of high-quality submissions and the selection process was highly competitive. We encourage you to submit to future calls for papers.</p>
                    <p>Thank you again for your interest and contribution to the scientific community.</p>
                </body>
                </html>
                """, title);
    }

    private String buildConfirmReminderEmail(String title, String sessionName,
                                              String sessionDatetime, String confirmDeadline) {
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Reminder: Confirmation Required</h2>
                    <p>This is a reminder that we have not yet received your confirmation to present your abstract <strong>"%s"</strong>.</p>

                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr><td style="padding: 6px; color: #555;">Session</td><td style="padding: 6px;">%s</td></tr>
                        <tr><td style="padding: 6px; color: #555;">Date &amp; Time</td><td style="padding: 6px;">%s</td></tr>
                        <tr><td style="padding: 6px; color: #d9534f;">Confirmation deadline</td><td style="padding: 6px;"><strong>%s</strong></td></tr>
                    </table>

                    <p style="margin-top: 20px;">Please reply to confirm or decline your participation. Failure to respond by the deadline will result in your abstract being removed from the programme.</p>
                </body>
                </html>
                """, title, sessionName, sessionDatetime, confirmDeadline);
    }

    private String buildRegReminderEmail(String title, String sessionName,
                                         String sessionDatetime, String regDeadline) {
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Registration Reminder</h2>
                    <p>This is a reminder that your abstract <strong>"%s"</strong> has been accepted for presentation, but we have not yet received your conference registration.</p>

                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr><td style="padding: 6px; color: #555;">Session</td><td style="padding: 6px;">%s</td></tr>
                        <tr><td style="padding: 6px; color: #555;">Date &amp; Time</td><td style="padding: 6px;">%s</td></tr>
                        <tr><td style="padding: 6px; color: #d9534f;">Registration deadline</td><td style="padding: 6px;"><strong>%s</strong></td></tr>
                    </table>

                    <p style="margin-top: 20px;">Please complete your registration before the deadline. Failure to register by the deadline may result in your abstract being removed from the programme.</p>
                    <p>If you have already registered or need to arrange a substitute presenter, please contact us immediately.</p>
                </body>
                </html>
                """, title, sessionName, sessionDatetime, regDeadline);
    }
}
