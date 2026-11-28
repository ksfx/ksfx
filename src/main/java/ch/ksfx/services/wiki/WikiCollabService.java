package ch.ksfx.services.wiki;

import ch.ksfx.dao.WikiPageDAO;
import ch.ksfx.dao.WikiPageVersionDAO;
import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.user.User;
import ch.ksfx.model.wiki.WikiPage;
import ch.ksfx.model.wiki.WikiPageVersion;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live editing sessions for wiki pages (branch multiwiki): several people - and agents - work on the
 * same page at once. The document itself is a Yjs CRDT that lives in the browsers; this service is
 * only the relay and the memory of a session:
 *
 * - it keeps the Yjs updates of the session in order (opaque base64 - the server never interprets
 *   them; Yjs updates commute, so a late joiner just applies all of them),
 * - it relays the awareness states (who is here, where the cursors are),
 * - it lets exactly ONE client fill the shared document from the saved Markdown (two clients doing it
 *   at once would duplicate the text),
 * - it keeps the latest Markdown snapshot the clients send, so agents read the live state and an
 *   unsaved session is not lost when everybody leaves (saved as a version then),
 * - and it hands an agent's write to the session's leader (the longest-present client), which merges
 *   it into the live document (three-way, against what the agent had read) instead of the agent
 *   overwriting what the humans are typing.
 *
 * Transport is long-polling (see WikiCollabController): it works through every proxy, including the
 * web isolation that buffers streams. Sessions live in memory only - a restart ends them, the last
 * snapshot is saved before (see {@link #saveAllOnShutdown}).
 */
@Service
public class WikiCollabService
{
    private static final long CLIENT_TIMEOUT_MS = 45_000;
    private static final long EXTERNAL_TIMEOUT_MS = 20_000;
    private static final long POLL_TIMEOUT_MS = 25_000;

    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    private final WikiPageDAO wikiPageDAO;
    private final WikiPageVersionDAO wikiPageVersionDAO;
    private final WikiService wikiService;
    private final UserDAO userDAO;
    private final SystemLogger systemLogger;

    public WikiCollabService(WikiPageDAO wikiPageDAO, WikiPageVersionDAO wikiPageVersionDAO, WikiService wikiService, UserDAO userDAO, SystemLogger systemLogger)
    {
        this.wikiPageDAO = wikiPageDAO;
        this.wikiPageVersionDAO = wikiPageVersionDAO;
        this.wikiService = wikiService;
        this.userDAO = userDAO;
        this.systemLogger = systemLogger;
    }

    // ---- Browser side ------------------------------------------------------------------------------

    public Map<String, Object> join(WikiPage page, long clientId, User user)
    {
        Session session = sessions.computeIfAbsent(page.getId(), id -> new Session(id, latestContent(page)));
        Map<String, Object> answer = new LinkedHashMap<>();

        synchronized (session) {
            Client client = new Client(clientId, user);
            session.clients.put(clientId, client);
            session.gone.remove(clientId);
            session.awarenessVersion++;

            boolean initialize = !session.initialized
                    && (session.initializerClientId == null || !session.clients.containsKey(session.initializerClientId));

            if (initialize) {
                session.initializerClientId = clientId;
            }

            answer.put("initialize", initialize);
            answer.put("markdown", session.savedMarkdown);
            answer.put("updates", new ArrayList<>(session.updates));
            answer.put("index", session.updates.size());
            answer.put("userLabel", client.userLabel);
            answer.put("color", client.color);
        }

        notifyWaiters(session);
        return answer;
    }

    public Map<String, Object> push(Long pageId, long clientId, User user, int seen, List<String> updates, String awareness, String markdown)
    {
        Session session = sessions.get(pageId);

        if (session == null) {
            return null; // ended meanwhile - the client re-joins
        }

        int index;
        boolean inSync;

        synchronized (session) {
            Client client = session.clients.get(clientId);

            if (client == null) {
                return null;
            }

            client.lastSeen = System.currentTimeMillis();
            // The client has every update the session has (nobody else appended since it last
            // looked) - only then is its Markdown snapshot the complete current state.
            inSync = seen == session.updates.size();

            if (updates != null && !updates.isEmpty()) {
                session.updates.addAll(updates);
                session.initialized = true;
                session.lastChangeUserId = user != null ? user.getId() : session.lastChangeUserId;
            }

            if (awareness != null) {
                client.awareness = awareness;
                client.awarenessVersion = ++session.awarenessVersion;
            }

            if (markdown != null && inSync) {
                session.snapshotMarkdown = markdown;
            }

            index = session.updates.size();
        }

        notifyWaiters(session);

        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("index", index);
        answer.put("inSync", inSync); // false: the snapshot was stale and ignored - send it again after the next poll
        return answer;
    }

    /** Long-poll: answers as soon as there is something newer than what the client has, else after 25 s with nothing. */
    public DeferredResult<Map<String, Object>> poll(Long pageId, long clientId, int since, long awarenessSince, long externalSince)
    {
        DeferredResult<Map<String, Object>> result = new DeferredResult<>(POLL_TIMEOUT_MS);
        Session session = sessions.get(pageId);

        if (session == null) {
            result.setResult(endedAnswer());
            return result;
        }

        Waiter waiter = new Waiter(clientId, since, awarenessSince, externalSince, result);

        synchronized (session) {
            Client client = session.clients.get(clientId);

            if (client == null) {
                result.setResult(endedAnswer());
                return result;
            }

            client.lastSeen = System.currentTimeMillis();
            Map<String, Object> answer = answerFor(session, waiter);

            if (answer != null) {
                result.setResult(answer);
                return result;
            }

            session.waiters.add(waiter);
        }

        result.onTimeout(() -> {
            synchronized (session) {
                session.waiters.remove(waiter);
                Map<String, Object> empty = answerFor(session, waiter, true);
                result.setResult(empty);
            }
        });

        return result;
    }

    public void leave(Long pageId, long clientId)
    {
        Session session = sessions.get(pageId);

        if (session == null) {
            return;
        }

        synchronized (session) {
            if (session.clients.remove(clientId) != null) {
                session.gone.put(clientId, ++session.awarenessVersion);
            }
        }

        notifyWaiters(session);
    }

    /** The leader merged an agent's write into the live document - persist the merged result as the agent's version. */
    public boolean externalApplied(Long pageId, long clientId, long externalId, String markdown)
    {
        Session session = sessions.get(pageId);

        if (session == null) {
            return false;
        }

        External external;

        synchronized (session) {
            external = session.externals.stream().filter(e -> e.id == externalId).findFirst().orElse(null);

            if (external == null || session.leaderClientId() == null || session.leaderClientId() != clientId) {
                return false;
            }

            session.externals.remove(external);
            session.snapshotMarkdown = markdown;
        }

        WikiPage page = wikiPageDAO.getWikiPageForId(pageId);

        if (page != null) {
            wikiService.saveContent(page, markdown, null, external.agent);
            markSaved(pageId, markdown);
        }

        return true;
    }

    /** A regular save from the edit form (WikiController.savePage) - that content is now what is saved. */
    public void markSaved(Long pageId, String markdown)
    {
        Session session = sessions.get(pageId);

        if (session != null) {
            synchronized (session) {
                session.savedMarkdown = markdown;

                if (session.snapshotMarkdown == null) {
                    session.snapshotMarkdown = markdown;
                }
            }
        }
    }

    /** Names of the people currently in the page's live session, for the page view. */
    public List<String> editorsOf(Long pageId)
    {
        List<String> names = new ArrayList<>();
        Session session = sessions.get(pageId);

        if (session != null) {
            synchronized (session) {
                session.clients.values().forEach(c -> {
                    if (!names.contains(c.userLabel)) {
                        names.add(c.userLabel);
                    }
                });
            }
        }

        return names;
    }

    // ---- Agent side --------------------------------------------------------------------------------

    /**
     * What an agent reads: the live snapshot while a session runs (fresher than the saved version),
     * else null (= read the saved version as usual). Remembered as the base of the agent's next write.
     */
    public String liveContentForAgent(Long pageId, Agent agent)
    {
        Session session = sessions.get(pageId);

        if (session == null) {
            return null;
        }

        synchronized (session) {
            if (session.clients.isEmpty()) {
                return null;
            }

            String content = session.snapshotMarkdown != null ? session.snapshotMarkdown : session.savedMarkdown;
            session.agentReads.put(agent.getId(), content);

            return content;
        }
    }

    /**
     * An agent writes a page that is being edited live: instead of overwriting, the write goes to the
     * session's leader, which merges it in (base = what the agent read). Returns false if there is no
     * live session - the caller then saves as usual.
     */
    public boolean submitAgentWrite(Long pageId, Agent agent, String content)
    {
        Session session = sessions.get(pageId);

        if (session == null) {
            return false;
        }

        synchronized (session) {
            if (session.clients.isEmpty() || !session.initialized) {
                return false;
            }

            External external = new External();
            external.id = ++session.externalSeq;
            external.agent = agent;
            external.agentName = agent.getName();
            external.base = session.agentReads.getOrDefault(agent.getId(), session.savedMarkdown);
            external.theirs = content != null ? content : "";
            external.createdAt = System.currentTimeMillis();
            session.externals.add(external);
        }

        notifyWaiters(session);
        return true;
    }

    // ---- Housekeeping ------------------------------------------------------------------------------

    @Scheduled(initialDelay = 30_000, fixedDelay = 15_000)
    public void housekeeping()
    {
        long now = System.currentTimeMillis();

        for (Iterator<Map.Entry<Long, Session>> it = sessions.entrySet().iterator(); it.hasNext(); ) {
            Session session = it.next().getValue();
            List<External> overdue = new ArrayList<>();
            boolean end;
            boolean changed = false;

            synchronized (session) {
                for (Iterator<Client> clients = session.clients.values().iterator(); clients.hasNext(); ) {
                    Client client = clients.next();

                    if (now - client.lastSeen > CLIENT_TIMEOUT_MS) {
                        clients.remove();
                        session.gone.put(client.clientId, ++session.awarenessVersion);
                        changed = true;
                    }
                }

                for (Iterator<External> externals = session.externals.iterator(); externals.hasNext(); ) {
                    External external = externals.next();

                    if (session.clients.isEmpty() || now - external.createdAt > EXTERNAL_TIMEOUT_MS) {
                        externals.remove();
                        overdue.add(external);
                    }
                }

                end = session.clients.isEmpty();
            }

            // Nobody merged it in time (all tabs asleep): save the agent's version as it is, like
            // without a session - it is in the history either way.
            for (External external : overdue) {
                WikiPage page = wikiPageDAO.getWikiPageForId(session.pageId);

                if (page != null) {
                    wikiService.saveContent(page, external.theirs, null, external.agent);
                    systemLogger.logMessage("WIKI", "Live session of page " + session.pageId + " did not merge the write of agent "
                            + external.agentName + " in time - saved it as a version of its own.");
                }
            }

            if (end) {
                saveUnsaved(session);
                it.remove();
            } else if (changed) {
                notifyWaiters(session);
            }
        }
    }

    /** A restart ends every session - save what was typed but not saved yet. */
    @javax.annotation.PreDestroy
    public void saveAllOnShutdown()
    {
        sessions.values().forEach(this::saveUnsaved);
    }

    private void saveUnsaved(Session session)
    {
        String snapshot;
        Long userId;

        synchronized (session) {
            snapshot = session.snapshotMarkdown;
            userId = session.lastChangeUserId;

            if (snapshot == null || snapshot.equals(session.savedMarkdown)) {
                return;
            }

            session.savedMarkdown = snapshot;
        }

        try {
            WikiPage page = wikiPageDAO.getWikiPageForId(session.pageId);

            if (page != null) {
                wikiService.saveContent(page, snapshot, userId != null ? userDAO.getUser(userId) : null, null);
            }
        } catch (Exception e) {
            systemLogger.logMessage("WIKI", "Could not save the live session of page " + session.pageId, e);
        }
    }

    // ---- Internals ---------------------------------------------------------------------------------

    private String latestContent(WikiPage page)
    {
        WikiPageVersion latest = wikiPageVersionDAO.getLatestVersionForPage(page.getId());

        return latest != null && latest.getContent() != null ? latest.getContent() : "";
    }

    private void notifyWaiters(Session session)
    {
        List<Runnable> completions = new ArrayList<>();

        synchronized (session) {
            for (Iterator<Waiter> it = session.waiters.iterator(); it.hasNext(); ) {
                Waiter waiter = it.next();
                Map<String, Object> answer = answerFor(session, waiter);

                if (answer != null) {
                    it.remove();
                    completions.add(() -> waiter.result.setResult(answer));
                }
            }
        }

        completions.forEach(Runnable::run);
    }

    private Map<String, Object> answerFor(Session session, Waiter waiter)
    {
        return answerFor(session, waiter, false);
    }

    /** What is new for this waiter - or null if nothing (unless {@code force}). Caller holds the session lock. */
    private Map<String, Object> answerFor(Session session, Waiter waiter, boolean force)
    {
        boolean newUpdates = session.updates.size() > waiter.since;
        boolean newAwareness = session.awarenessVersion > waiter.awarenessSince;
        Long leader = session.leaderClientId();
        boolean isLeader = leader != null && leader == waiter.clientId;
        boolean newExternal = isLeader && session.externals.stream().anyMatch(e -> e.id > waiter.externalSince);

        if (!force && !newUpdates && !newAwareness && !newExternal) {
            return null;
        }

        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("index", session.updates.size());
        answer.put("updates", newUpdates ? new ArrayList<>(session.updates.subList(Math.min(waiter.since, session.updates.size()), session.updates.size())) : new ArrayList<>());

        List<Map<String, Object>> awareness = new ArrayList<>();
        List<Map<String, Object>> present = new ArrayList<>();

        for (Client client : session.clients.values()) {
            Map<String, Object> person = new LinkedHashMap<>();
            person.put("clientId", client.clientId);
            person.put("name", client.userLabel);
            person.put("color", client.color);
            present.add(person);

            if (client.clientId != waiter.clientId && client.awareness != null && client.awarenessVersion > waiter.awarenessSince) {
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("clientId", client.clientId);
                state.put("update", client.awareness);
                awareness.add(state);
            }
        }

        List<Long> gone = new ArrayList<>();
        session.gone.forEach((clientId, version) -> {
            if (version > waiter.awarenessSince) {
                gone.add(clientId);
            }
        });

        answer.put("awareness", awareness);
        answer.put("gone", gone);
        answer.put("present", present);
        answer.put("awarenessVersion", session.awarenessVersion);
        answer.put("leader", isLeader);

        List<Map<String, Object>> externals = new ArrayList<>();

        if (isLeader) {
            for (External external : session.externals) {
                if (external.id > waiter.externalSince) {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("id", external.id);
                    e.put("agentName", external.agentName);
                    e.put("base", external.base);
                    e.put("theirs", external.theirs);
                    externals.add(e);
                }
            }
        }

        answer.put("externals", externals);
        return answer;
    }

    private Map<String, Object> endedAnswer()
    {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("ended", true);
        return answer;
    }

    private static final String[] COLORS = { "#e8590c", "#2b8a3e", "#1971c2", "#9c36b5", "#c2255c", "#0c8599", "#5f3dc4", "#e67700" };

    private static class Session
    {
        final Long pageId;
        final List<String> updates = new ArrayList<>();
        final LinkedHashMap<Long, Client> clients = new LinkedHashMap<>();
        final Map<Long, Long> gone = new LinkedHashMap<>();
        final List<Waiter> waiters = new ArrayList<>();
        final List<External> externals = new ArrayList<>();
        final Map<Long, String> agentReads = new ConcurrentHashMap<>();
        long awarenessVersion = 0;
        long externalSeq = 0;
        boolean initialized = false;
        Long initializerClientId;
        String savedMarkdown;
        String snapshotMarkdown;
        Long lastChangeUserId;

        Session(Long pageId, String savedMarkdown)
        {
            this.pageId = pageId;
            this.savedMarkdown = savedMarkdown;
        }

        /** The longest-present client merges agents' writes. */
        Long leaderClientId()
        {
            return clients.isEmpty() ? null : clients.keySet().iterator().next();
        }
    }

    private static class Client
    {
        final long clientId;
        final String userLabel;
        final String color;
        String awareness;
        long awarenessVersion;
        long lastSeen = System.currentTimeMillis();

        Client(long clientId, User user)
        {
            this.clientId = clientId;
            this.userLabel = user == null ? "Someone"
                    : user.getFirstName() != null && !user.getFirstName().trim().isEmpty() ? user.getFirstName().trim() : user.getUsername();
            this.color = COLORS[(int) Math.abs((user != null && user.getId() != null ? user.getId() : clientId) % COLORS.length)];
        }
    }

    private static class Waiter
    {
        final long clientId;
        final int since;
        final long awarenessSince;
        final long externalSince;
        final DeferredResult<Map<String, Object>> result;

        Waiter(long clientId, int since, long awarenessSince, long externalSince, DeferredResult<Map<String, Object>> result)
        {
            this.clientId = clientId;
            this.since = since;
            this.awarenessSince = awarenessSince;
            this.externalSince = externalSince;
            this.result = result;
        }
    }

    private static class External
    {
        long id;
        Agent agent;
        String agentName;
        String base;
        String theirs;
        long createdAt;
    }
}
