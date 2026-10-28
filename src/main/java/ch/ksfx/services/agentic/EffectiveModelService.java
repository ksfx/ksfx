package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.dao.ProjectAgenticSettingsDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.ProjectAgenticSettings;
import org.springframework.stereotype.Service;

/**
 * Answers "which model does this agent run on, and why" for the UI (agent form, chat header):
 * the resolution chain agent override > project default > provider default - exactly the logic
 * ClaudeCliSessionService.buildCommand applies per turn - plus, separately, the model that
 * ACTUALLY answered the last turn (AgentMessage.model, captured from the CLI's init event).
 * Expected and actual shown side by side end the guessing: a model asked "what are you" inside a
 * resumed conversation just parrots its transcript.
 */
@Service
public class EffectiveModelService
{
    private final ProjectAgenticSettingsDAO settingsDAO;
    private final AgentMessageDAO agentMessageDAO;

    public EffectiveModelService(ProjectAgenticSettingsDAO settingsDAO, AgentMessageDAO agentMessageDAO)
    {
        this.settingsDAO = settingsDAO;
        this.agentMessageDAO = agentMessageDAO;
    }

    public EffectiveModel effectiveModelFor(Agent agent)
    {
        if (agent.getModel() != null && !agent.getModel().trim().isEmpty()) {
            return new EffectiveModel(agent.getModel().trim(), "agent override");
        }

        ProjectAgenticSettings settings = agent.getProject() != null ? settingsDAO.getForProjectId(agent.getProject().getId()) : null;

        if (settings != null && settings.getDefaultModel() != null && !settings.getDefaultModel().trim().isEmpty()) {
            return new EffectiveModel(settings.getDefaultModel().trim(), "project default");
        }

        // The step everyone forgets, proven empirically 2026-10-27: `claude --resume` RESTORES the
        // session's previous model when no --model is passed. An agent whose chain is empty does
        // not fall back to the provider/account default - it keeps whatever its session last ran
        // (e.g. a long-removed override). Pinning it elsewhere needs an explicit model for at
        // least one turn, or a session reset.
        if (agent.getClaudeSessionId() != null && !agent.getClaudeSessionId().trim().isEmpty()) {
            String lastActual = agentMessageDAO.getLatestModelForAgent(agent.getId());

            return new EffectiveModel(lastActual, "resumed session keeps its model");
        }

        return new EffectiveModel(null, "provider default");
    }

    /** The model that actually answered the agent's most recent turn, or null (no captured turn yet). */
    public String lastActualModelFor(Agent agent)
    {
        return agentMessageDAO.getLatestModelForAgent(agent.getId());
    }

    public static class EffectiveModel
    {
        private final String model;
        private final String source;

        EffectiveModel(String model, String source)
        {
            this.model = model;
            this.source = source;
        }

        /** null = no model is sent; what runs depends on the source (provider default, or whatever the resumed session kept). */
        public String getModel() { return model; }
        public String getSource() { return source; }

        public String getDisplay()
        {
            if (model != null) {
                return model + " (" + source + ")";
            }

            return source.startsWith("resumed") ? "unknown - " + source + " (no model sent)" : "provider default (no model sent)";
        }
    }
}
