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

    /** Container-side mount point of a project's provider directory (see {@link #resolveProjectProviderDir}). */
    public static final String CONTAINER_PROVIDER_DIR = "/provider/claude";

    /**
     * Where the Claude CLI keeps an agent's state - transcripts (needed for --resume), memory,
     * sessions, settings: {@code <workspace root>/provider/claude/project-<id>/agent-<id>}, passed
     * to the CLI as CLAUDE_CONFIG_DIR (concept "Backing up agent and project workspaces", KSFX
     * wiki). Next to the workspaces, not inside them: provider state is infrastructure the agent
     * should neither see nor delete through its own tools, and it must survive a container Throw
     * Away and land in the workspace backup. The project level exists because a container gets
     * only its own project's subtree mounted; the agent level because sessions and memory are per
     * agent. Future providers get their own namespace under {@code provider/}.
     */
    public Path resolveProjectProviderDir(Project project, AgenticConfig config)
    {
        return Paths.get(config.getWorkspaceRoot(), "provider", "claude", "project-" + project.getId());
    }

    public Path resolveProviderDir(Agent agent, AgenticConfig config)
    {
        if (agent.getProject() == null) {
            return Paths.get(config.getWorkspaceRoot(), "provider", "claude", "agent-" + agent.getId());
        }

        return resolveProjectProviderDir(agent.getProject(), config).resolve("agent-" + agent.getId());
    }

    /** The same directory as seen from inside the project's container (the project subtree is mounted at {@link #CONTAINER_PROVIDER_DIR}). */
    public String containerProviderDir(Agent agent)
    {
        return CONTAINER_PROVIDER_DIR + "/agent-" + agent.getId();
    }

    public Path ensureProviderDir(Agent agent, AgenticConfig config) throws IOException
    {
        Path providerDir = resolveProviderDir(agent, config);

        Files.createDirectories(providerDir);

        return providerDir;
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
