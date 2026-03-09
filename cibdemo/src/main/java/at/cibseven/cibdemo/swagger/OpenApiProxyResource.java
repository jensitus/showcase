package at.cibseven.cibdemo.swagger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Bridges OpenAPI descriptor files packaged in the classpath so they are available under the
 * Jersey application path (/engine-rest by default). This avoids 404 when Jersey intercepts
 * the request before Spring's static resource handling.
 */
@Component
@Path("/")
public class OpenApiProxyResource {

    private static final String[] JSON_CANDIDATES = new String[]{
            "/META-INF/resources/engine-rest/openapi.json",
            "/engine-rest/openapi.json",
            "/META-INF/resources/openapi.json",
            "/openapi.json"
    };

    private static final String[] YAML_CANDIDATES = new String[]{
            "/META-INF/resources/engine-rest/openapi",
            "/engine-rest/openapi",
            "/META-INF/resources/openapi",
            "/openapi"
    };

    @GET
    @Path("openapi.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOpenApiJson() throws IOException {
        byte[] content = readFirstAvailable(JSON_CANDIDATES);
        if (content == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("OpenAPI JSON not found")
                           .build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("openapi")
    @Produces({"application/yaml", "text/yaml"})
    public Response getOpenApiYaml() throws IOException {
        byte[] content = readFirstAvailable(YAML_CANDIDATES);
        if (content == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("OpenAPI YAML not found")
                           .build();
        }
        return Response.ok(content)
                       .type("application/yaml")
                       .build();
    }

    private byte[] readFirstAvailable(String[] candidates) throws IOException {
        for (String path : candidates) {
            try (InputStream is = OpenApiProxyResource.class.getResourceAsStream(path)) {
                if (is != null) {
                    return is.readAllBytes();
                }
            }
        }
        return null;
    }
}
