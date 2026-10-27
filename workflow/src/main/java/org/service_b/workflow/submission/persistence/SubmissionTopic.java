package org.service_b.workflow.submission.persistence;

/**
 * Imaging topics, as a radiology congress groups its abstracts (the topics both of two
 * congress years used). Stored by constant name; the label is for display.
 */
public enum SubmissionTopic {
    ABDOMINAL_VISCERA("Abdominal Viscera"),
    ARTIFICIAL_INTELLIGENCE("Artificial Intelligence"),
    BREAST("Breast"),
    CARDIAC("Cardiac"),
    CHEST("Chest"),
    EDUCATION_PROFESSIONAL("Education & Professional Issues"),
    EMERGENCY("Emergency Imaging"),
    GENITOURINARY("Genitourinary"),
    GI_TRACT("GI Tract"),
    HEAD_AND_NECK("Head and Neck"),
    HYBRID_MOLECULAR("Hybrid, Molecular and Translational Imaging"),
    IMAGING_INFORMATICS("Imaging Informatics"),
    INTERVENTIONAL("Interventional Radiology"),
    MUSCULOSKELETAL("Musculoskeletal"),
    NEURO("Neuro"),
    ONCOLOGIC("Oncologic Imaging"),
    PAEDIATRIC("Paediatric"),
    PHYSICS("Physics in Medical Imaging"),
    QUALITY_SAFETY("Quality & Safety"),
    RADIATION_PROTECTION("Radiation Protection"),
    RADIOGRAPHERS("Radiographers"),
    VASCULAR("Vascular"),
    OTHER("Other");

    private final String label;

    SubmissionTopic(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
