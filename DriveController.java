package com.placement.controller;

import com.placement.dto.DriveRequest;
import com.placement.model.Company;
import com.placement.model.PlacementDrive;
import com.placement.repository.PlacementDriveRepository;
import com.placement.service.CompanyService;
import com.placement.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drives")
@RequiredArgsConstructor
public class DriveController {

    private final PlacementDriveRepository driveRepository;
    private final CompanyService companyService;
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<PlacementDrive>> all() {
        return ResponseEntity.ok(driveRepository.findAllByOrderByDriveDateDesc());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlacementDrive> create(@Valid @RequestBody DriveRequest req) {
        Company company = companyService.getById(req.getCompanyId());
        PlacementDrive drive = new PlacementDrive();
        drive.setCompany(company);
        drive.setTitle(req.getTitle());
        drive.setDescription(req.getDescription());
        drive.setVenue(req.getVenue());
        drive.setDriveDate(req.getDriveDate());
        PlacementDrive saved = driveRepository.save(drive);

        notificationService.notifyAllStudents("Placement drive: " + saved.getTitle(),
                company.getCompanyName() + " drive on " + saved.getDriveDate().toString().replace('T', ' ')
                        + (saved.getVenue() == null || saved.getVenue().isBlank() ? "" : " at " + saved.getVenue()));
        notificationService.notify(company.getUser(), "Placement drive scheduled",
                "A drive \"" + saved.getTitle() + "\" has been scheduled for " + saved.getDriveDate().toString().replace('T', ' ') + ".");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!driveRepository.existsById(id)) {
            throw new IllegalArgumentException("Drive not found: " + id);
        }
        driveRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
