package org.service_b.workflow.workflow.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.workflow.service.DeploymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
@Slf4j
public class DeploymentController {

    private final DeploymentService deploymentService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<String> deploy(
            @RequestParam("file") MultipartFile file,
            @RequestParam("deployment-name") String deploymentName,
            @RequestParam(value = "tenant-id", required = false, defaultValue = "") String tenantId) {
        try {
            String result = deploymentService.deploy(file, deploymentName, tenantId);
            log.info("Deployed '{}' for tenant '{}'", deploymentName, tenantId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Deployment failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Deployment failed: " + e.getMessage());
        }
    }
}
