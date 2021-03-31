package ch.ksfx.controller;

import ch.ksfx.dao.NoteDAO;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.spidering.ResourceLoaderPluginConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.note.Note;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/noteviewer")
public class NoteViewerController
{
    private TimeSeriesDAO timeSeriesDAO;
    private ActivityDAO activityDAO;
    private PublishingConfigurationDAO publishingConfigurationDAO;
    private SpideringConfigurationDAO spideringConfigurationDAO;
    private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;
    private NoteDAO noteDAO;

    private Long timeSeriesId;
    private Long activityId;
    private Long publishingConfigurationId;
    private Long spideringConfigurationId;
    private Long resourceLoaderPluginConfigurationId;

    public NoteViewerController(TimeSeriesDAO timeSeriesDAO,
                                ActivityDAO activityDAO,
                                PublishingConfigurationDAO publishingConfigurationDAO,
                                SpideringConfigurationDAO spideringConfigurationDAO,
                                ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO,
                                NoteDAO noteDAO)
    {
        this.timeSeriesDAO = timeSeriesDAO;
        this.activityDAO = activityDAO;
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.spideringConfigurationDAO = spideringConfigurationDAO;
        this.resourceLoaderPluginConfigurationDAO = resourceLoaderPluginConfigurationDAO;
        this.noteDAO = noteDAO;
    }

    @GetMapping("/")
    public String viewNote(@RequestParam(name = "timeSeriesId", required = false) Long timeSeriesId,
                           @RequestParam(name = "activityId", required = false) Long activityId,
                           @RequestParam(name = "publishingConfigurationId", required = false) Long publishingConfigurationId,
                           @RequestParam(name = "spideringConfigurationId", required = false) Long spideringConfigurationId,
                           @RequestParam(name = "resourceLoaderPluginConfigurationId", required = false) Long resourceLoaderPluginConfigurationId,
                           Model model)
    {
        this.timeSeriesId = timeSeriesId;
        this.activityId = activityId;
        this.publishingConfigurationId = publishingConfigurationId;
        this.spideringConfigurationId = spideringConfigurationId;
        this.resourceLoaderPluginConfigurationId = resourceLoaderPluginConfigurationId;

        String entityType = null;
        Long entityId = null;

        if (timeSeriesId != null) {
            entityId = timeSeriesId;
            entityType = "timeSeries";
        }

        if (activityId != null) {
            entityId = activityId;
            entityType = "activity";
        }

        if (publishingConfigurationId != null) {
            entityId = publishingConfigurationId;
            entityType = "publishingConfiguration";
        }

        if (spideringConfigurationId != null) {
            entityId = spideringConfigurationId;
            entityType = "spideringConfiguration";
        }

        if (resourceLoaderPluginConfigurationId != null) {
            entityId = resourceLoaderPluginConfigurationId;
            entityType = "resourceLoaderPluginConfiguration";
        }

        model.addAttribute("name", getName());
        model.addAttribute("mainNotes", getMainNotes());
        model.addAttribute("relatedNotes", getRelatedNotes());
        model.addAttribute("entityId", entityId);
        model.addAttribute("entityType", entityType);

        return "note_viewer";
    }

    public String getName()
    {
        if (timeSeriesId != null) {
            return timeSeriesDAO.getTimeSeriesForId(timeSeriesId).getName();
        }

        if (activityId != null) {
            return activityDAO.getActivityForId(activityId).getName();
        }

        if (publishingConfigurationId != null) {
            return publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId).getName();
        }

        if (spideringConfigurationId != null) {
            return spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId).getName();
        }

        if (resourceLoaderPluginConfigurationId != null) {
            return resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId).getName();
        }

        return "";
    }

    public List<Note> getMainNotes()
    {
        TimeSeries timeSeries = null;

        if (timeSeriesId != null) {
            timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
        }

        Activity activity = null;

        if (activityId != null) {
            activity = activityDAO.getActivityForId(activityId);
        }

        PublishingConfiguration publishingConfiguration = null;

        if (publishingConfigurationId != null) {
            publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
        }

        SpideringConfiguration spideringConfiguration = null;

        if (spideringConfigurationId != null) {
            spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        }

        ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = null;

        if (resourceLoaderPluginConfigurationId != null) {
            resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
        }

        return noteDAO.searchNotes(null, null, timeSeries, activity, publishingConfiguration, spideringConfiguration, resourceLoaderPluginConfiguration, true);
    }

    public List<Note> getRelatedNotes()
    {
        TimeSeries timeSeries = null;

        if (timeSeriesId != null) {
            timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
        }

        Activity activity = null;

        if (activityId != null) {
            activity = activityDAO.getActivityForId(activityId);
        }

        PublishingConfiguration publishingConfiguration = null;

        if (publishingConfigurationId != null) {
            publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
        }

        SpideringConfiguration spideringConfiguration = null;

        if (spideringConfigurationId != null) {
            spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        }

        ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = null;

        if (resourceLoaderPluginConfigurationId != null) {
            resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
        }

        return noteDAO.searchNotes(null, null, timeSeries, activity, publishingConfiguration, spideringConfiguration, resourceLoaderPluginConfiguration, false);
    }

    /*
    @Property
    private Note note;

    @Inject
    private NoteDAO noteDAO;

    @Inject
    private TimeSeriesDAO timeSeriesDAO;

    @Inject
    private ActivityDAO activityDAO;

    @Inject
    private PublishingConfigurationDAO publishingConfigurationDAO;

    @Inject
    private SpideringConfigurationDAO spideringConfigurationDAO;

    @Inject
    private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;

    @Inject
    private PageRenderLinkSource pageRenderLinkSource;

    private Long timeSeriesId;
    private Long activityId;
    private Long publishingConfigurationId;
    private Long spideringConfigurationId;
    private Long resourceLoaderPluginConfigurationId;


    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long timeSeriesId, Long activityId, Long publishingConfigurationId, Long spideringConfigurationId, Long resourceLoaderPluginConfigurationId)
    {
        this.timeSeriesId = timeSeriesId;
        this.activityId = activityId;
        this.publishingConfigurationId = publishingConfigurationId;
        this.spideringConfigurationId = spideringConfigurationId;
        this.resourceLoaderPluginConfigurationId = resourceLoaderPluginConfigurationId;
    }

    public Long[] onPassivate()
    {
        List<Long> p = new ArrayList<Long>();
        p.add(timeSeriesId);
        p.add(activityId);
        p.add(publishingConfigurationId);
        p.add(spideringConfigurationId);
        p.add(resourceLoaderPluginConfigurationId);

        return p.toArray(new Long[4]);
    }

    public String getCreateNewNoteLink()
    {
        return pageRenderLinkSource.createPageRenderLinkWithContext("admin/note/managenote", getCreateNewNoteContextParameters()).toURI();
    }

    public Object[] getCreateNewNoteContextParameters()
    {

        return new Object[]{timeSeriesId,
                activityId,
                publishingConfigurationId,spideringConfigurationId,resourceLoaderPluginConfigurationId};
    }

    public String getName()
    {
        if (timeSeriesId != null) {
            return timeSeriesDAO.getTimeSeriesForId(timeSeriesId).getName();
        }

        if (activityId != null) {
            return activityDAO.getActivityForId(activityId).getName();
        }

        if (publishingConfigurationId != null) {
            return publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId).getName();
        }

        if (spideringConfigurationId != null) {
            return spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId).getName();
        }

        if (resourceLoaderPluginConfigurationId != null) {
            return resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId).getName();
        }

        return "";
    }

    public List<Note> getMainNotes()
    {
        TimeSeries timeSeries = null;

        if (timeSeriesId != null) {
            timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
        }

        Activity activity = null;

        if (activityId != null) {
            activity = activityDAO.getActivityForId(activityId);
        }

        PublishingConfiguration publishingConfiguration = null;

        if (publishingConfigurationId != null) {
            publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
        }

        SpideringConfiguration spideringConfiguration = null;

        if (spideringConfigurationId != null) {
            spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        }

        ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = null;

        if (resourceLoaderPluginConfigurationId != null) {
            resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
        }

        return noteDAO.searchNotes(null, null, timeSeries, activity, publishingConfiguration, spideringConfiguration, resourceLoaderPluginConfiguration, true);
    }

    public List<Note> getNotes()
    {
        TimeSeries timeSeries = null;

        if (timeSeriesId != null) {
            timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
        }

        Activity activity = null;

        if (activityId != null) {
            activity = activityDAO.getActivityForId(activityId);
        }

        PublishingConfiguration publishingConfiguration = null;

        if (publishingConfigurationId != null) {
            publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
        }

        SpideringConfiguration spideringConfiguration = null;

        if (spideringConfigurationId != null) {
            spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        }

        ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = null;

        if (resourceLoaderPluginConfigurationId != null) {
            resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
        }

        return noteDAO.searchNotes(null, null, timeSeries, activity, publishingConfiguration, spideringConfiguration, resourceLoaderPluginConfiguration, false);
    }
     */
}
