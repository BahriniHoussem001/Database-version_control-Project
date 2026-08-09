package com.dbvc.controller;

import com.dbvc.dto.EnvironmentComparisonResponse;
import com.dbvc.service.EnvironmentComparisonService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentComparisonController {

    private final EnvironmentComparisonService environmentComparisonService;

    public EnvironmentComparisonController(EnvironmentComparisonService environmentComparisonService) {
        this.environmentComparisonService = environmentComparisonService;
    }
    ///compare?source=DEV&target=TEST
    @GetMapping("/api/environments/compare")
    public EnvironmentComparisonResponse compareEnvironments(
            @RequestParam("source") String source,
            @RequestParam("target") String target
    ) {
        return environmentComparisonService.compare(source, target);
    }
}