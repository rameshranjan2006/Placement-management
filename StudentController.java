package com.placement.controller;

import com.placement.model.Student;
import com.placement.security.AccessGuard;
import com.placement.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final AccessGuard guard;

    @GetMapping("/me")
    public ResponseEntity<Student> myProfile() {
        return ResponseEntity.ok(guard.student());
    }

    @PutMapping("/me")
    public ResponseEntity<Student> updateMyProfile(@Valid @RequestBody Student updated) {
        return ResponseEntity.ok(studentService.updateProfile(guard.student(), updated));
    }
}
