package com.wandrr.modules.expenses;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses/report")
@RequiredArgsConstructor
public class ExpenseReportController {

    private final ExpenseReportService reportService;

    @GetMapping("/{tripId}/download")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal UserDetails principal) {

        String report = reportService.generateReport(tripId, principal.getUsername());
        byte[] content = report.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=wandrr-expense-report-" + tripId + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(content.length)
                .body(content);
    }
}
