package com.placement.controller;

import com.placement.model.Company;
import com.placement.model.Resume;
import com.placement.model.Student;
import com.placement.model.enums.Role;
import com.placement.repository.ApplicationRepository;
import com.placement.security.AccessGuard;
import com.placement.service.ResumeService;
import com.placement.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final StudentService studentService;
    private final ApplicationRepository applicationRepository;
    private final AccessGuard guard;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resume> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(resumeService.store(guard.student(), file));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Resume>> mine() {
        return ResponseEntity.ok(resumeService.list(guard.student()));
    }

    /** Resume list for a student - admin, or a company the student has applied to. */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Resume>> forStudent(@PathVariable Long studentId) {
        Student student = studentService.getById(studentId);
        requireResumeAccess(student);
        return ResponseEntity.ok(resumeService.list(student));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) throws IOException {
        Resume resume = resumeService.get(id);
        requireResumeAccess(resume.getStudent());
        byte[] data = resumeService.read(resume);

        String contentType = resume.getContentType() != null ? resume.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(resume.getFileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IOException {
        Resume resume = resumeService.get(id);
        if (!resume.getStudent().getId().equals(guard.student().getId())) {
            throw new AccessDeniedException("You do not own this resume");
        }
        resumeService.delete(resume);
        return ResponseEntity.noContent().build();
    }

    private void requireResumeAccess(Student owner) {
        Role role = guard.user().getRole();
        if (role == Role.ADMIN) return;
        if (role == Role.STUDENT && guard.student().getId().equals(owner.getId())) return;
        if (role == Role.COMPANY) {
            Company company = guard.company();
            if (applicationRepository.existsByStudentAndJobCompany(owner, company)) return;
        }
        throw new AccessDeniedException("You are not allowed to access this resume");
    }
}
