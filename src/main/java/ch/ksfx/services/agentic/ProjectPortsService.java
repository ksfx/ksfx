package ch.ksfx.services.agentic;

import ch.ksfx.model.Project;
import ch.ksfx.services.project.ProjectContextService;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The published ports of a Docker-isolated project, for the breadcrumb's ports dropdown: the
 * mappings themselves come from {@link AgenticDockerService#portMappingsFor} (one truth, same as
 * the container run command and the setup preview), and {@link #knock} answers "does anything
 * LISTEN on that host port right now" - a plain TCP connect against localhost, which either
 * succeeds or is refused within microseconds there (the connect timeout only guards exotic
 * filtering). Knocking happens only when the dropdown is opened, asynchronously from the browser
 * (ProjectPortsController) - never during page rendering - and is cached briefly so repeated
 * opening does not re-knock. Green means "a process listens", not "the app answers sensibly".
 */
@Service("projectPortsService")
public class ProjectPortsService
{
    private static final int CONNECT_TIMEOUT_MS = 300;
    private static final long CACHE_TTL_MS = 5000;

    private final AgenticDockerService agenticDockerService;
    private final ProjectContextService projectContextService;
    private final Map<Long, CachedKnock> knockCache = new ConcurrentHashMap<>();

    public ProjectPortsService(AgenticDockerService agenticDockerService, ProjectContextService projectContextService)
    {
        this.agenticDockerService = agenticDockerService;
        this.projectContextService = projectContextService;
    }

    /** container port -> host port of the CURRENT project, empty unless it is Docker-isolated - the breadcrumb fragment's render-time call. */
    public Map<Integer, Integer> currentProjectPorts()
    {
        Project project = projectContextService.current();

        if (project == null || project.getId() == null || !project.getDockerIsolationEnabled()) {
            return Collections.emptyMap();
        }

        return agenticDockerService.portMappingsFor(project.getId());
    }

    /** host port -> something listens, for exactly the project's derived ports (never arbitrary ones - no port-scan primitive). */
    public Map<Integer, Boolean> knock(Project project)
    {
        CachedKnock cached = knockCache.get(project.getId());

        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.result;
        }

        Map<Integer, Boolean> result = new LinkedHashMap<>();

        agenticDockerService.portMappingsFor(project.getId()).values().parallelStream().forEach(hostPort -> {
            boolean listening;

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", hostPort), CONNECT_TIMEOUT_MS);
                listening = true;
            } catch (Exception e) {
                listening = false;
            }

            synchronized (result) {
                result.put(hostPort, listening);
            }
        });

        Map<Integer, Boolean> ordered = new LinkedHashMap<>();
        agenticDockerService.portMappingsFor(project.getId()).values().forEach(p -> ordered.put(p, Boolean.TRUE.equals(result.get(p))));
        knockCache.put(project.getId(), new CachedKnock(ordered));

        return ordered;
    }

    private static class CachedKnock
    {
        final Map<Integer, Boolean> result;
        final long timestamp = System.currentTimeMillis();

        CachedKnock(Map<Integer, Boolean> result) { this.result = result; }
    }
}
