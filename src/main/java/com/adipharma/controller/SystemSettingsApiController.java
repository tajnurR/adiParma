package com.adipharma.controller;

import com.adipharma.entity.AdiSystemSettings;
import com.adipharma.service.SystemSettingsService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-settings")
public class SystemSettingsApiController {

    private final SystemSettingsService service;

    public SystemSettingsApiController(SystemSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> getSettings() {
        return service.getSettingsPayload();
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody AdiSystemSettings request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Settings payload is required."));
        }
        AdiSystemSettings saved = service.saveSettings(request);
        return ResponseEntity.ok(service.getSettingsPayload());
    }
}
