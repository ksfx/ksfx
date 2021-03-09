package ch.ksfx.controller.admin.scheduler;

import ch.ksfx.services.scheduler.SchedulerService;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/scheduler")
public class SchedulerController
{
    private SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService)
    {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/")
    public String schedulerIndex(Model model)
    {
        model.addAttribute("schedulerService", schedulerService);

        return "admin/scheduler/scheduler";
    }

    @GetMapping("/jobpause/{job}")
    public String  onActionFromPauseJob(@PathVariable(value = "job", required = true) String job) throws SchedulerException
    {
        schedulerService.pauseJob(job.split("\\.")[1], job.split("\\.")[0]);

        return "redirect:/admin/scheduler/";
    }

    @GetMapping("/jobresume/{job}")
    public String onActionFromResumeJob(@PathVariable(value = "job", required = true) String job) throws SchedulerException
    {
        schedulerService.resumeJob(job.split("\\.")[1], job.split("\\.")[0]);

        return "redirect:/admin/scheduler/";
    }

    @GetMapping("/jobdelete/{job}")
    public String onActionFromDeleteJob(@PathVariable(value = "job", required = true) String job) throws SchedulerException
    {
        schedulerService.deleteJob(job.split("\\.")[1], job.split("\\.")[0]);

        return "redirect:/admin/scheduler/";
    }
}
