package com.placement.controller;

import com.placement.dto.ApplicationStatusUpdateRequest;
import com.placement.model.Application;
import com.placement.model.Job;
import com.placement.model.Student;
import com.placement.security.AccessGuard;
import com.placement.service.ApplicationService;
import com.placement.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final JobService jobService;
    private final AccessGuard guard;

    @PostMapping("/job/{jobId}")
    public ResponseEntity<Application> apply(@PathVariable Long jobId) {
        Student student = guard.student();
        return ResponseEntity.ok(applicationService.apply(student, jobService.getById(jobId)));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Application>> myApplications() {
        return ResponseEntity.ok(applicationService.findByStudent(guard.student()));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<Application>> applicationsForJob(@PathVariable Long jobId) {
        Job job = jobService.getById(jobId);
        guard.requireCompanyOwnerOrAdmin(job.getCompany());
        return ResponseEntity.ok(applicationService.findByJob(job));
    }

    /** Company (owner) or admin shortlists / selects / rejects. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Application> updateStatus(@PathVariable Long id,
                                                    @RequestBody ApplicationStatusUpdateRequest req) {
        Application application = applicationService.getById(id);
        guard.requireCompanyOwnerOrAdmin(application.getJob().getCompany());
        return ResponseEntity.ok(applicationService.updateStatus(application, req.getStatus()));
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<Application> withdraw(@PathVariable Long id) {
        Application application = applicationService.getById(id);
        Student student = guard.student();
        if (!application.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("You do not own this application");
        }
        return ResponseEntity.ok(applicationService.withdraw(application));
    }
}
