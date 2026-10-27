package org.service_b.workflow.submission.dto;

import java.util.List;

public class CreateSubmissionRequest {
    private String title;
    private String authors;
    /** Only for a request without sections (the old one-field form); generated otherwise. */
    private String abstractText;
    private List<SectionDto> sections;
    private String topic;
    private String submitterEmail;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public List<SectionDto> getSections() { return sections; }
    public void setSections(List<SectionDto> sections) { this.sections = sections; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getSubmitterEmail() { return submitterEmail; }
    public void setSubmitterEmail(String submitterEmail) { this.submitterEmail = submitterEmail; }
}
