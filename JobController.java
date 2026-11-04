package com.placement.controller;

import com.placement.dto.JobRequest;
import com.placement.dto.JobSearchResult;
import com.placement.model.Job;
import com.placement.model.Student;
import com.placement.model.enums.Role;
import com.placement.security.AccessGuard;
import com.placement.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final AccessGuard guard;

    /** Public listing - no login required. */
    @GetMapping("/public/open")
    public ResponseEntity<List<Job>> publicOpenJobs() {
        return ResponseEntity.ok(jobService.findAllOpen());
    }

    /** Search / filter / sort. For students each result also carries eligibility + applied flags. */
    @GetMapping("/search")
    public ResponseEntity<List<JobSearchResult>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minCtc,
            @RequestParam(defaultValue = "false") boolean eligibleOnly,
            @RequestParam(defaultValue = "posted") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Student student = guard.user().getRole() == Role.STUDENT ? guard.student() : null;
        return ResponseEntity.ok(jobService.search(student, q, type, location, minCtc, eligibleOnly, sort, dir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @GetMapping("/eligible")
    public ResponseEntity<List<Job>> eligibleJobsForCurrentStudent() {
        return ResponseEntity.ok(jobService.findEligibleJobsForStudent(guard.student()));
    }

    @GetMapping("/company/mine")
    public ResponseEntity<List<Job>> myCompanyJobs() {
        return ResponseEntity.ok(jobService.findByCompany(guard.company()));
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@Valid @RequestBody JobRequest req) {
        return ResponseEntity.ok(jobService.createJob(guard.company(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest req) {
        Job job = jobService.getById(id);
        guard.requireCompanyOwnerOrAdmin(job.getCompany());
        return ResponseEntity.ok(jobService.updateJob(job, req));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> closeJob(@PathVariable Long id) {
        Job job = jobService.getById(id);
        guard.requireCompanyOwnerOrAdmin(job.getCompany());
        jobService.closeJob(job);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/eligible-students")
    public ResponseEntity<List<Student>> eligibleStudents(@PathVariable Long id) {
        Job job = jobService.getById(id);
        guard.requireCompanyOwnerOrAdmin(job.getCompany());
        return ResponseEntity.ok(jobService.findEligibleStudentsForJob(job));
    }
}
