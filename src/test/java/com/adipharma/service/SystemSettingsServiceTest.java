package com.adipharma.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.adipharma.entity.AdiSystemSettings;
import com.adipharma.repository.AdiSystemSettingsRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceTest {

    @Mock
    private AdiSystemSettingsRepository repository;

    @Test
    void getSettingsPayloadCreatesDefaultWithoutManualIdAndAllowsNullOptionalFields() {
        AtomicReference<Long> idBeforeSave = new AtomicReference<>();
        when(repository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(repository.save(any(AdiSystemSettings.class))).thenAnswer(invocation -> {
            AdiSystemSettings settings = invocation.getArgument(0);
            idBeforeSave.set(settings.getId());
            settings.setId(1L);
            return settings;
        });

        SystemSettingsService service = new SystemSettingsService(repository);

        Map<String, Object> payload = service.getSettingsPayload();

        assertNull(idBeforeSave.get());
        assertEquals("AdiPharma Pharmacy", payload.get("pharmacyName"));
        assertTrue(payload.containsKey("pharmacyAddress"));
        assertNull(payload.get("pharmacyAddress"));
    }
}
