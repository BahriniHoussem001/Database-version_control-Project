package com.dbvc.controller;

import com.dbvc.dto.EnvironmentPromotionStatusResponse;
import com.dbvc.service.EnvironmentPromotionPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EnvironmentPromotionController {

    private final EnvironmentPromotionPolicyService environmentPromotionPolicyService;

    @GetMapping("/api/environments/promotion-status")
    public List<EnvironmentPromotionStatusResponse> getPromotionStatus() {
        return environmentPromotionPolicyService.getPromotionStatuses();
    }
}