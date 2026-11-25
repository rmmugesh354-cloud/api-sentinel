package com.sentinel.apisentinel.settings;

import com.sentinel.apisentinel.auth.security.UserPrincipal;
import com.sentinel.apisentinel.common.ApiResponse;
import com.sentinel.apisentinel.settings.dto.UserSettingsRequest;
import com.sentinel.apisentinel.settings.dto.UserSettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "User alert configuration and default thresholds")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get notification and monitoring preferences for current user")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserSettingsResponse response = settingsService.getSettings(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    @Operation(summary = "Update monitoring and notification preferences")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            @Valid @RequestBody UserSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserSettingsResponse response = settingsService.updateSettings(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Settings updated successfully", response));
    }
}
