package ch.ksfx.controller.admin.note;

import ch.ksfx.dao.NoteDAO;
import ch.ksfx.model.note.Note;
import ch.ksfx.model.note.NoteFile;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@RequestMapping("/admin/note")
public class NoteController
{
    private NoteDAO noteDAO;

    public NoteController(NoteDAO noteDAO)
    {
        this.noteDAO = noteDAO;
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

        noteDAO.saveOrUpdateNote(note);

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
}
