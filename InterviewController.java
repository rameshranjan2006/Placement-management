package com.placement.controller;

import com.placement.dto.InterviewRequest;
import com.placement.model.Application;
import com.placement.model.Interview;
import com.placement.model.enums.Role;
import com.placement.security.AccessGuard;
import com.placement.service.ApplicationService;
import com.placement.service.InterviewService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final ApplicationService applicationService;
    private final AccessGuard guard;

    @Data
    public static class FeedbackRequest {
        private String feedback;
    }

    /** Interviews relevant to the caller: own (student), own company's (company) or all (admin). */
    @GetMapping("/mine")
    public ResponseEntity<List<Interview>> mine() {
        Role role = guard.user().getRole();
        if (role == Role.STUDENT) return ResponseEntity.ok(interviewService.findForStudent(guard.student()));
        if (role == Role.COMPANY) return ResponseEntity.ok(interviewService.findForCompany(guard.company()));
        return ResponseEntity.ok(interviewService.findAll());
    }

    @PostMapping("/application/{applicationId}")
    public ResponseEntity<Interview> schedule(@PathVariable Long applicationId, @RequestBody InterviewRequest req) {
        Application application = applicationService.getById(applicationId);
        guard.requireCompanyOwnerOrAdmin(application.getJob().getCompany());
        return ResponseEntity.ok(interviewService.schedule(application, req));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<Interview> get(@PathVariable Long applicationId) {
        Application application = applicationService.getById(applicationId);
        Role role = guard.user().getRole();
        if (role == Role.STUDENT) {
            if (!application.getStudent().getId().equals(guard.student().getId())) {
                throw new AccessDeniedException("You do not own this application");
            }
        } else {
            guard.requireCompanyOwnerOrAdmin(application.getJob().getCompany());
        }
        return ResponseEntity.ok(interviewService.getByApplication(application));
    }

    @PatchMapping("/application/{applicationId}/feedback")
    public ResponseEntity<Interview> addFeedback(@PathVariable Long applicationId, @RequestBody FeedbackRequest req) {
        Application application = applicationService.getById(applicationId);
        guard.requireCompanyOwnerOrAdmin(application.getJob().getCompany());
        Interview interview = interviewService.getByApplication(application);
        return ResponseEntity.ok(interviewService.addFeedback(interview, req.getFeedback()));
    }
}
