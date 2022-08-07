package ch.ksfx.controller.admin.note;

import ch.ksfx.dao.NoteDAO;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.spidering.ResourceLoaderPluginConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.note.*;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Date;

@Controller
@RequestMapping("/admin/note")
public class NoteController
{
    private NoteDAO noteDAO;
    private ActivityDAO activityDAO;
    private TimeSeriesDAO timeSeriesDAO;
    private SpideringConfigurationDAO spideringConfigurationDAO;
    private PublishingConfigurationDAO publishingConfigurationDAO;
    private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;

    public NoteController(NoteDAO noteDAO,
                          ActivityDAO activityDAO,
                          TimeSeriesDAO timeSeriesDAO,
                          SpideringConfigurationDAO spideringConfigurationDAO,
                          PublishingConfigurationDAO publishingConfigurationDAO,
                          ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO)
    {
        this.noteDAO = noteDAO;
        this.activityDAO = activityDAO;
        this.timeSeriesDAO = timeSeriesDAO;
        this.spideringConfigurationDAO = spideringConfigurationDAO;
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.resourceLoaderPluginConfigurationDAO = resourceLoaderPluginConfigurationDAO;
    }

    @GetMapping("/")
    public String noteIndex(Pageable pageable, Model model)
    {
        model.addAttribute("notesPage", noteDAO.getNotesForPageableAndNoteCategory(pageable, null));

        return "admin/note/note";
    }

    @GetMapping({"/noteedit", "/noteedit/{id}"})
    public String noteEdit(@PathVariable(value = "id", required = false) Long noteId, Model model, Pageable pageable)
    {
        Note note = new Note();

        if (noteId != null) {
            note = noteDAO.getNoteForId(noteId);
        }

        model.addAttribute("noteFilesPage", noteDAO.getNoteFilesForPageableAndNote(pageable, note));
        model.addAttribute("allNoteCategories", noteDAO.getAllNoteCategories());
        model.addAttribute("note", note);

        return "admin/note/note_edit";
    }

    @PostMapping({"/noteedit", "/noteedit/{id}"})
    public String noteSubmit(@PathVariable(value = "id", required = false) Long noteId, @Valid @ModelAttribute Note note, BindingResult bindingResult, Model model, Pageable pageable)
    {
        if (bindingResult.hasErrors()) {
            model.addAttribute("noteFilesPage", noteDAO.getNoteFilesForPageableAndNote(pageable, note));
            model.addAttribute("allNoteCategories", noteDAO.getAllNoteCategories());

            return "admin/note/note_edit";
        }

        if (note.getNoteCategory() != null && note.getNoteCategory().getId().equals(0l)) {
            note.setNoteCategory(null);
        }

        noteDAO.saveOrUpdateNote(note);

        return "redirect:/admin/note/noteedit/" + note.getId();
    }


    @PostMapping("/notefileupload")
    public String handleFileUpload(@RequestParam(value = "noteFile", required = true) MultipartFile file,
                                   @RequestParam(value = "noteFileComment", required = false) String noteFileComment,
                                   @RequestParam(value = "noteId", required = true) Long noteId,
                                   RedirectAttributes redirectAttributes)
    {
        Note note = noteDAO.getNoteForId(noteId);

        try {
            System.out.println("Uploaded Notefile: " + file.getOriginalFilename());

            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded " + file.getOriginalFilename() + "!");

            NoteFile noteFile = new NoteFile();
            noteFile.setFileContent(file.getBytes());
            noteFile.setFileName(file.getOriginalFilename());
            noteFile.setContentType(file.getContentType());
            noteFile.setNote(note);
            noteFile.setComment(noteFileComment);

            note.setLastUpdated(new Date());

            noteDAO.saveOrUpdateNoteFile(noteFile);
            noteDAO.saveOrUpdateNote(note);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/admin/note/noteedit/" + note.getId();
    }

    @GetMapping("/notefiledelete/{noteFileId}")
    public String deleteNoteFile(@PathVariable(value = "noteFileId", required = false) Long noteFileId, RedirectAttributes redirectAttributes)
    {
        NoteFile noteFile = noteDAO.getNoteFileForId(noteFileId);

        noteDAO.deleteNoteFile(noteFile);

        redirectAttributes.addFlashAttribute("message", "Attached File Deleted...");

        return "redirect:/admin/note/noteedit/" + noteFile.getNote().getId();
    }

    @GetMapping("/notedelete/{noteId}")
    public String noteDelete(@PathVariable(value = "noteId", required = true) Long noteId)
    {
        Note note = noteDAO.getNoteForId(noteId);
        noteDAO.deleteNote(note);

        return "redirect:/admin/note/";
    }

    @PostMapping({"/relationadd"})
    public String relationAddSubmit(@RequestParam(name = "noteId", required = true) Long noteId,
                                    @RequestParam(name = "relatedNoteId", required = false) Long relatedNoteId,
                                    @RequestParam(name = "timeSeriesId", required = false) Long timeSeriesId,
                                    @RequestParam(name = "activityId", required = false) Long activityId,
                                    @RequestParam(name = "publishingConfigurationId", required = false) Long publishingConfigurationId,
                                    @RequestParam(name = "spideringConfigurationId", required = false) Long spideringConfigurationId,
                                    @RequestParam(name = "resourceLoaderPluginConfigurationId", required = false) Long resourceLoaderPluginConfigurationId)
    {
        Note note = noteDAO.getNoteForId(noteId);

        if (relatedNoteId != null) {
            Note relatedNote = noteDAO.getNoteForId(relatedNoteId);

            NoteNote noteNote = new NoteNote();
            noteNote.setNote(note);
            noteNote.setRelatedNote(relatedNote);

            noteDAO.saveOrUpdateNoteNote(noteNote);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (activityId != null) {
            Activity activity = activityDAO.getActivityForId(activityId);

            NoteActivity noteActivity = new NoteActivity();
            noteActivity.setNote(note);
            noteActivity.setActivity(activity);

            noteDAO.saveOrUpdateNoteActivity(noteActivity);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (timeSeriesId != null) {
            TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);

            NoteTimeSeries noteTimeSeries = new NoteTimeSeries();
            noteTimeSeries.setNote(note);
            noteTimeSeries.setTimeSeries(timeSeries);

            noteDAO.saveOrUpdateNoteTimeSeries(noteTimeSeries);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (spideringConfigurationId != null) {
            SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);

            NoteSpideringConfiguration noteSpideringConfiguration = new NoteSpideringConfiguration();
            noteSpideringConfiguration.setNote(note);
            noteSpideringConfiguration.setSpideringConfiguration(spideringConfiguration);

            noteDAO.saveOrUpdateNoteSpideringConfiguration(noteSpideringConfiguration);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (publishingConfigurationId != null) {
            PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);

            NotePublishingConfiguration notePublishingConfiguration = new NotePublishingConfiguration();
            notePublishingConfiguration.setNote(note);
            notePublishingConfiguration.setPublishingConfiguration(publishingConfiguration);

            noteDAO.saveOrUpdateNotePublishingConfiguration(notePublishingConfiguration);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (resourceLoaderPluginConfigurationId != null) {
            ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);

            NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration = new NoteResourceLoaderPluginConfiguration();
            noteResourceLoaderPluginConfiguration.setNote(note);
            noteResourceLoaderPluginConfiguration.setResourceLoaderPluginConfiguration(resourceLoaderPluginConfiguration);

            noteDAO.saveOrUpdateNoteResourceLoaderPluginConfiguration(noteResourceLoaderPluginConfiguration);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        return "redirect:/admin/note/noteedit/" + note.getId();
    }


    @GetMapping({"/createnoteforentity"})
    public String createNoteForEntity(@RequestParam(name = "timeSeriesId", required = false) Long timeSeriesId,
                                      @RequestParam(name = "activityId", required = false) Long activityId,
                                      @RequestParam(name = "publishingConfigurationId", required = false) Long publishingConfigurationId,
                                      @RequestParam(name = "spideringConfigurationId", required = false) Long spideringConfigurationId,
                                      @RequestParam(name = "resourceLoaderPluginConfigurationId", required = false) Long resourceLoaderPluginConfigurationId)
    {
        Note note = new Note();
        note.setCreated(new Date());
        note.setLastUpdated(new Date());
        noteDAO.saveOrUpdateNote(note);

        if (activityId != null) {
            Activity activity = activityDAO.getActivityForId(activityId);
            note.setTitle("Note for Activity: " + activity.getName());

            NoteActivity noteActivity = new NoteActivity();
            noteActivity.setNote(note);
            noteActivity.setActivity(activity);

            noteDAO.saveOrUpdateNoteActivity(noteActivity);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (timeSeriesId != null) {
            TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
            note.setTitle("Note for Time Series: " + timeSeries.getName());

            NoteTimeSeries noteTimeSeries = new NoteTimeSeries();
            noteTimeSeries.setNote(note);
            noteTimeSeries.setTimeSeries(timeSeries);

            noteDAO.saveOrUpdateNoteTimeSeries(noteTimeSeries);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (spideringConfigurationId != null) {
            SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
            note.setTitle("Note for Spidering Configuration: " + spideringConfiguration.getName());

            NoteSpideringConfiguration noteSpideringConfiguration = new NoteSpideringConfiguration();
            noteSpideringConfiguration.setNote(note);
            noteSpideringConfiguration.setSpideringConfiguration(spideringConfiguration);

            noteDAO.saveOrUpdateNoteSpideringConfiguration(noteSpideringConfiguration);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (publishingConfigurationId != null) {
            PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
            note.setTitle("Note for Publishing Configuration: " + publishingConfiguration.getName());

            NotePublishingConfiguration notePublishingConfiguration = new NotePublishingConfiguration();
            notePublishingConfiguration.setNote(note);
            notePublishingConfiguration.setPublishingConfiguration(publishingConfiguration);

            noteDAO.saveOrUpdateNotePublishingConfiguration(notePublishingConfiguration);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        if (resourceLoaderPluginConfigurationId != null) {
            ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
            note.setTitle("Note for Resource Loader: " + resourceLoaderPluginConfiguration.getName());

            NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration = new NoteResourceLoaderPluginConfiguration();
            noteResourceLoaderPluginConfiguration.setNote(note);
            noteResourceLoaderPluginConfiguration.setResourceLoaderPluginConfiguration(resourceLoaderPluginConfiguration);

            noteDAO.saveOrUpdateNoteResourceLoaderPluginConfiguration(noteResourceLoaderPluginConfiguration);

            note.setLastUpdated(new Date());
            noteDAO.saveOrUpdateNote(note);
        }

        return "redirect:/admin/note/noteedit/" + note.getId();
    }

    @GetMapping({"/notefiledownload", "/notefiledownload/{id}"})
    public HttpEntity<byte[]> noteFileDownload(@PathVariable(value = "id", required = true) Long noteFileId)
    {
        NoteFile noteFile = noteDAO.getNoteFileForId(noteFileId);

        byte[] documentBody = noteFile.getFileContent();

        HttpHeaders header = new HttpHeaders();
        //header.setContentType(noteFile.getContentType());
        header.set(HttpHeaders.CONTENT_TYPE, noteFile.getContentType());
        header.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + noteFile.getFileName().replace(" ", "_"));
        header.setContentLength(documentBody.length);

        return new HttpEntity<byte[]>(documentBody, header);
    }

    @GetMapping({"/togglemainnoteactivity/{noteActivityId}"})
    public String toggleMainNoteActivity(@PathVariable(value = "noteActivityId", required = true) Long noteActivityId, RedirectAttributes redirectAttributes)
    {
        NoteActivity noteActivity = noteDAO.getNoteActivityForId(noteActivityId);
        Note note = noteActivity.getNote();

        note.setLastUpdated(new Date());
        noteActivity.setMainNote(!noteActivity.getMainNote());

        noteDAO.saveOrUpdateNoteActivity(noteActivity);
        noteDAO.saveOrUpdateNote(note);

        redirectAttributes.addFlashAttribute("message", "Main Note for Activity Changed...");

        return "redirect:/admin/note/noteedit/" + note.getId();
    }

    @GetMapping({"/togglemainnotetimeseries/{noteTimeSeriesId}"})
    public String onActionFromToggleMainNoteTimeSeries(@PathVariable(value = "noteTimeSeriesId", required = true) Long noteTimeSeriesId, RedirectAttributes redirectAttributes)
    {
        NoteTimeSeries noteTimeSeries = noteDAO.getNoteTimeSeriesForId(noteTimeSeriesId);
        Note note = noteTimeSeries.getNote();

        note.setLastUpdated(new Date());
        noteTimeSeries.setMainNote(!noteTimeSeries.getMainNote());

        noteDAO.saveOrUpdateNoteTimeSeries(noteTimeSeries);
        noteDAO.saveOrUpdateNote(note);

        redirectAttributes.addFlashAttribute("message", "Main Note for Time Series Changed...");

        return "redirect:/admin/note/noteedit/" + note.getId();
    }

    @GetMapping({"/togglemainnotespideringconfiguration/{noteSpideringConfigurationId}"})
    public String toggleMainNoteSpideringConfiguration(@PathVariable(value = "noteSpideringConfigurationId", required = true) Long noteSpideringConfigurationId, RedirectAttributes redirectAttributes)
    {
        NoteSpideringConfiguration noteSpideringConfiguration = noteDAO.getNoteSpideringConfigurationForId(noteSpideringConfigurationId);
        Note note = noteSpideringConfiguration.getNote();

        note.setLastUpdated(new Date());
        noteSpideringConfiguration.setMainNote(!noteSpideringConfiguration.getMainNote());

        noteDAO.saveOrUpdateNoteSpideringConfiguration(noteSpideringConfiguration);
        noteDAO.saveOrUpdateNote(note);

        redirectAttributes.addFlashAttribute("message", "Main Note for Spidering Configuration Changed...");

        return "redirect:/admin/note/noteedit/" + note.getId();
    }

    @GetMapping({"/togglemainnotepublishingconfiguration/{notePublishingConfigurationId}"})
    public String toggleMainNotePublishingConfiguration(@PathVariable(value = "notePublishingConfigurationId", required = true) Long notePublishingConfigurationId, RedirectAttributes redirectAttributes)
    {
        NotePublishingConfiguration notePublishingConfiguration = noteDAO.getNotePublishingConfigurationForId(notePublishingConfigurationId);
        Note note = notePublishingConfiguration.getNote();

        note.setLastUpdated(new Date());
        notePublishingConfiguration.setMainNote(!notePublishingConfiguration.getMainNote());

        noteDAO.saveOrUpdateNotePublishingConfiguration(notePublishingConfiguration);
        noteDAO.saveOrUpdateNote(note);

        redirectAttributes.addFlashAttribute("message", "Main Note for Publishing Configuration Changed...");

        return "redirect:/admin/note/noteedit/" + note.getId();
    }

    @GetMapping({"/togglemainnoteresourceloaderpluginconfiguration/{resourceLoaderPluginConfigurationId}"})
    public String toggleMainNoteResourceLoaderPluginConfiguration(@PathVariable(value = "resourceLoaderPluginConfigurationId", required = true) Long resourceLoaderPluginConfigurationId, RedirectAttributes redirectAttributes)
    {
        NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration = noteDAO.getNoteResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
        Note note = noteResourceLoaderPluginConfiguration.getNote();

        note.setLastUpdated(new Date());
        noteResourceLoaderPluginConfiguration.setMainNote(!noteResourceLoaderPluginConfiguration.getMainNote());

        noteDAO.saveOrUpdateNoteResourceLoaderPluginConfiguration(noteResourceLoaderPluginConfiguration);
        noteDAO.saveOrUpdateNote(note);

        redirectAttributes.addFlashAttribute("message", "Main Note for Resource Loader Configuration Changed...");

        return "redirect:/admin/note/noteedit/" + note.getId();
    }
}
