package org.service_b.workflow.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Body for POST /screen. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenRequest {
    private List<Submission> submissions;
}
