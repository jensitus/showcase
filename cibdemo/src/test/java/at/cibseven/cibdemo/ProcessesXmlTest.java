package at.cibseven.cibdemo;

import org.cibseven.bpm.model.bpmn.Bpmn;
import org.cibseven.bpm.model.bpmn.BpmnModelInstance;
import org.cibseven.bpm.model.bpmn.instance.Process;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards what the engine deploys at startup (META-INF/processes.xml) — no engine, no database.
 *
 * <p>The workflow backend starts abstract_submission_lifecycle under tenant cfp and
 * novelty_batch under tenant screening; a process deployed without its tenant cannot be
 * started there. And since scanning is off, a .bpmn that is not listed is never deployed.
 */
class ProcessesXmlTest {

    private static final Path RESOURCES = Path.of("src/main/resources");

    /** archive name -> {tenantId, resources}. */
    private record Archive(String tenantId, List<String> resources) { }

    private Map<String, Archive> archives() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var doc = factory.newDocumentBuilder().parse(RESOURCES.resolve("META-INF/processes.xml").toFile());
        Map<String, Archive> out = new HashMap<>();
        NodeList list = doc.getElementsByTagNameNS("*", "process-archive");
        for (int i = 0; i < list.getLength(); i++) {
            Element a = (Element) list.item(i);
            NodeList res = a.getElementsByTagNameNS("*", "resource");
            List<String> resources = new java.util.ArrayList<>();
            for (int j = 0; j < res.getLength(); j++) {
                resources.add(res.item(j).getTextContent().trim());
            }
            String tenant = a.getAttribute("tenantId");
            out.put(a.getAttribute("name"), new Archive(tenant.isEmpty() ? null : tenant, resources));
        }
        return out;
    }

    private Set<String> processKeys(String resource) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, resource + " is listed in processes.xml but not on the classpath");
            BpmnModelInstance model = Bpmn.readModelFromStream(in);
            Bpmn.validateModel(model);
            Set<String> keys = new HashSet<>();
            for (Process p : model.getModelElementsByType(Process.class)) {
                if (p.isExecutable()) {
                    keys.add(p.getId());
                }
            }
            return keys;
        }
    }

    @Test
    void processesAreDeployedUnderTheTenantTheBackendStartsThemIn() throws Exception {
        Map<String, Archive> archives = archives();
        assertEquals("cfp", archives.get("cfp").tenantId());
        Set<String> cfp = new HashSet<>();
        for (String r : archives.get("cfp").resources()) {
            cfp.addAll(processKeys(r));
        }
        // the call-for-papers process calls novelty_check: both must be under the same tenant
        assertEquals(Set.of("abstract_submission_lifecycle", "novelty_check"), cfp);
        assertEquals("screening", archives.get("screening").tenantId());
        assertEquals(Set.of("novelty_batch"), processKeys(archives.get("screening").resources().get(0)));
    }

    @Test
    void everyListedResourceIsAValidExecutableModel() throws Exception {
        for (Archive a : archives().values()) {
            for (String r : a.resources()) {
                assertTrue(!processKeys(r).isEmpty(), r + " has no executable process");
            }
        }
    }

    @Test
    void everyBpmnUnderResourcesIsListed() throws Exception {
        Set<String> listed = new HashSet<>();
        archives().values().forEach(a -> listed.addAll(a.resources()));
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            List<String> missing = files
                    .filter(p -> p.toString().endsWith(".bpmn"))
                    .map(p -> RESOURCES.relativize(p).toString().replace('\\', '/'))
                    .filter(r -> !listed.contains(r))
                    .toList();
            assertTrue(missing.isEmpty(), "not in META-INF/processes.xml, so never deployed: " + missing);
        }
    }
}
