package at.cibseven.cibdemo.swagger;


import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
public class OpenApiController {

    private static final String[] JSON_CANDIDATES = new String[]{
            "META-INF/resources/engine-rest/openapi.json",
            "engine-rest/openapi.json",
            "META-INF/resources/openapi.json",
            "openapi.json"
    };

    private static final String[] YAML_CANDIDATES = new String[]{
        "META-INF/resources/engine-rest/openapi",
        "engine-rest/openapi",
        "META-INF/resources/openapi",
        "openapi"
    };

    @GetMapping(value = "/cib-openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> json() throws IOException {
        byte[] body = readFirstAvailable(JSON_CANDIDATES);
        if (body == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("OpenAPI JSON not found".getBytes());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/cib-openapi", produces = {"application/yaml", "text/yaml"})
    public ResponseEntity<byte[]> yaml() throws IOException {
        byte[] body = readFirstAvailable(YAML_CANDIDATES);
        if (body == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("OpenAPI YAML not found".getBytes());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/yaml");
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private byte[] readFirstAvailable(String[] candidates) throws IOException {
        for (String path : candidates) {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return StreamUtils.copyToByteArray(is);
                }
            }
        }
        return null;
    }
}
