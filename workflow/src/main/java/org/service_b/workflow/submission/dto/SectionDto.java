package org.service_b.workflow.submission.dto;

/**
 * One abstract section. In a request only {@code shortname} (an AbstractSection constant,
 * e.g. PURPOSE) and {@code content} count; a response also carries the label as {@code name}.
 */
public class SectionDto {
    private String shortname;
    private String name;
    private String content;

    public SectionDto() { }

    public SectionDto(String shortname, String name, String content) {
        this.shortname = shortname;
        this.name = name;
        this.content = content;
    }

    public String getShortname() { return shortname; }
    public void setShortname(String shortname) { this.shortname = shortname; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
