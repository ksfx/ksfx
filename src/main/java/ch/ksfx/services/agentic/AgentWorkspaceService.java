package ch.ksfx.services.agentic;

import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.Project;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves and creates the per-agent working directory that the headless Claude CLI process runs
 * in. This directory is the only v1 containment boundary for a subprocess that otherwise has the
 * same OS-level privileges as the KSFX server itself.
 */
@Service
public class AgentWorkspaceService
{
    public Path resolveProjectWorkspace(Project project, AgenticConfig config)
    {
        return Paths.get(config.getWorkspaceRoot(), "project-" + project.getId());
    }

    public Path resolveWorkspace(Agent agent, AgenticConfig config)
    {
        if (agent.getProject() == null) {
            return Paths.get(config.getWorkspaceRoot(), "agent-" + agent.getId());
        }

        return resolveProjectWorkspace(agent.getProject(), config).resolve("agent-" + agent.getId());
    }

    public Path ensureWorkspace(Agent agent, AgenticConfig config) throws IOException
    {
        Path workspace = resolveWorkspace(agent, config);

        Files.createDirectories(workspace);

        if (agent.getProject() != null) {
            Files.createDirectories(resolveProjectWorkspace(agent.getProject(), config).resolve("shared"));
        }

        return workspace;
    }

    /**
     * Physically relocates an agent's workspace after its Project assignment changes, so
     * existing files/history aren't silently orphaned. No-op if the paths are already the same or
     * the source doesn't exist yet (agent never ran); refuses to clobber an existing destination.
     */
    public void moveWorkspaceIfNeeded(Path oldWorkspace, Path newWorkspace) throws IOException
    {
        if (oldWorkspace.equals(newWorkspace) || !Files.exists(oldWorkspace) || Files.exists(newWorkspace)) {
            return;
        }

        Files.createDirectories(newWorkspace.getParent());
        Files.move(oldWorkspace, newWorkspace);
    }
}
