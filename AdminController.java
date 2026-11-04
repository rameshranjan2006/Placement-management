package com.placement.controller;

import com.placement.dto.AnnouncementRequest;
import com.placement.dto.RegisterCompanyRequest;
import com.placement.exception.BadRequestException;
import com.placement.model.*;
import com.placement.model.enums.ApplicationStatus;
import com.placement.model.enums.Role;
import com.placement.repository.UserRepository;
import com.placement.security.AccessGuard;
import com.placement.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CompanyService companyService;
    private final StudentService studentService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final EligibilityService eligibilityService;
    private final StatsService statsService;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AccessGuard guard;

    // ---------- students ----------
    @GetMapping("/students")
    public ResponseEntity<List<Student>> allStudents() {
        return ResponseEntity.ok(studentService.findAll());
    }

    // ---------- companies / recruiters ----------
    @GetMapping("/companies")
    public ResponseEntity<List<Company>> allCompanies() {
        return ResponseEntity.ok(companyService.findAll());
    }

    @GetMapping("/companies/pending")
    public ResponseEntity<List<Company>> pendingCompanies() {
        return ResponseEntity.ok(companyService.findPendingApproval());
    }

    @PatchMapping("/companies/{id}/approve")
    public ResponseEntity<Company> approveCompany(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.approve(companyService.getById(id)));
    }

    /** Admin creates a recruiter account + company profile, already approved. */
    @PostMapping("/companies")
    public ResponseEntity<Company> createCompany(@Valid @RequestBody RegisterCompanyRequest req) {
        Long companyId = authService.registerCompany(req).getProfileId();
        return ResponseEntity.ok(companyService.approve(companyService.getById(companyId)));
    }

    // ---------- account control ----------
    @PatchMapping("/users/{userId}/enabled")
    public ResponseEntity<Void> setEnabled(@PathVariable Long userId, @RequestParam boolean value) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (target.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be disabled");
        }
        target.setEnabled(value);
        userRepository.save(target);
        return ResponseEntity.noContent().build();
    }

    // ---------- jobs & applications ----------
    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> allJobs() {
        return ResponseEntity.ok(jobService.findAll());
    }

    @GetMapping("/applications")
    public ResponseEntity<List<Application>> allApplications(@RequestParam(required = false) ApplicationStatus status) {
        return ResponseEntity.ok(status == null ? applicationService.findAll() : applicationService.findByStatus(status));
    }

    /** Eligibility verification: why is (or isn't) this student eligible for this job? */
    @GetMapping("/eligibility/check")
    public ResponseEntity<Map<String, Object>> checkEligibility(@RequestParam Long studentId, @RequestParam Long jobId) {
        Student student = studentService.getById(studentId);
        Job job = jobService.getById(jobId);
        List<String> reasons = eligibilityService.ineligibilityReasons(student, job);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eligible", reasons.isEmpty());
        out.put("reasons", reasons);
        return ResponseEntity.ok(out);
    }

    // ---------- announcements ----------
    @PostMapping("/announcements")
    public ResponseEntity<Void> announce(@Valid @RequestBody AnnouncementRequest req) {
        notificationService.notifyAllStudents("Announcement: " + req.getTitle(), req.getMessage());
        return ResponseEntity.noContent().build();
    }

    // ---------- analytics & reports ----------
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(statsService.overview());
    }

    @GetMapping("/reports/placements.csv")
    public ResponseEntity<byte[]> placementsCsv() {
        byte[] body = statsService.placementsCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("placement-report.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
}
