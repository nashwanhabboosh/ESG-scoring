package com.nashwanhabboosh.esgplatform.controller;

import com.nashwanhabboosh.esgplatform.model.ScoringMethodology;
import com.nashwanhabboosh.esgplatform.repository.ScoringMethodologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/methodologies")
@RequiredArgsConstructor
public class MethodologyController {

    private final ScoringMethodologyRepository methodologyRepository;

    @GetMapping
    public List<ScoringMethodology> getAllMethodologies() {
        return methodologyRepository.findAll();
    }

    @PostMapping
    public ScoringMethodology createMethodology(@RequestBody ScoringMethodology methodology) {
        methodology.setActive(false);
        return methodologyRepository.save(methodology);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ScoringMethodology> activateMethodology(@PathVariable String id) {
        // Deactivate all others first
        methodologyRepository.findAll().forEach(m -> {
            m.setActive(false);
            methodologyRepository.save(m);
        });

        return methodologyRepository.findById(id)
            .map(m -> {
                m.setActive(true);
                return ResponseEntity.ok(methodologyRepository.save(m));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}