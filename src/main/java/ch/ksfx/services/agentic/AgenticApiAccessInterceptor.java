package ch.ksfx.services.agentic;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Restricts /agentic/api/** (the self-service API, e.g. AgentScheduleApiController - called by the
 * local Claude CLI subprocess, see ClaudeCliSessionService) to same-host callers - or, for a
 * Docker-isolated agent, callers from the Docker bridge network the agent's own container runs on
 * (see AgenticDockerService.isTrustedDockerAddress; those calls arrive via host.docker.internal, so
 * the remote address KSFX sees is a bridge-network IP, not localhost) - and to any endpoints added
 * under this path later.
 *
 * KSFX has no trusted-reverse-proxy configuration yet (no server.forward-headers-strategy /
 * RemoteIpFilter), so an X-Forwarded-For header can't be trusted to reflect the real client IP -
 * its mere presence is treated as "this came through an intermediary we don't yet trust" and
 * rejected outright, rather than risk honoring a spoofed/forwarded address as if it were local. This
 * rejection applies unconditionally, even to would-be Docker-bridge callers.
 * Revisit (proper trusted-proxies allowlist + a real IP allowlist for external callers) once KSFX's
 * actual reverse-proxy setup needs to be accounted for.
 */
@Component
public class AgenticApiAccessInterceptor implements HandlerInterceptor
{
    private static final Set<String> LOCALHOST_ADDRESSES = new HashSet<>(Arrays.asList("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"));

    private final AgenticDockerService agenticDockerService;

    public AgenticApiAccessInterceptor(AgenticDockerService agenticDockerService)
    {
        this.agenticDockerService = agenticDockerService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException
    {
        if (request.getHeader("X-Forwarded-For") != null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only local requests are allowed.");
            return false;
        }

        if (LOCALHOST_ADDRESSES.contains(request.getRemoteAddr()) || agenticDockerService.isTrustedDockerAddress(request.getRemoteAddr())) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only local requests are allowed.");
        return false;
    }
}
