package com.wandrr.modules.expenses;

import com.wandrr.modules.expenses.dto.BreakdownDTO;
import com.wandrr.modules.expenses.dto.MemberBalanceDTO;
import com.wandrr.modules.trips.Trip;
import com.wandrr.modules.trips.TripRepository;
import com.wandrr.modules.user.User;
import com.wandrr.modules.user.UserRepository;
import com.wandrr.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseReportService {

    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    /**
     * Generates a plain-text expense report for PDF download.
     * Using simple text format that can be served as text/plain or converted.
     */
    public String generateReport(UUID tripId, String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found."));

        BreakdownDTO breakdown = expenseService.getFullBreakdown(tripId, requesterEmail);
        var summary = expenseService.getSummary(tripId, requesterEmail);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("═══════════════════════════════════════════════\n");
        sb.append("           WANDRR EXPENSE REPORT\n");
        sb.append("═══════════════════════════════════════════════\n\n");
        sb.append("Trip: ").append(trip.getName()).append("\n");
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            sb.append("Dates: ").append(trip.getStartDate().format(fmt))
                    .append(" — ").append(trip.getEndDate().format(fmt)).append("\n");
        }
        sb.append("Generated: ").append(LocalDate.now().format(fmt)).append("\n");
        sb.append("Generated for: ").append(user.getFullName()).append("\n\n");

        // Total
        sb.append("───────────────────────────────────────────────\n");
        sb.append("TOTAL EXPENSES: ₹").append(summary.getTotalExpenses()).append("\n");
        sb.append("───────────────────────────────────────────────\n\n");

        // Settlement Plan
        sb.append("SETTLEMENT PLAN\n");
        sb.append("───────────────────────────────────────────────\n");
        if (breakdown.isAllSettled()) {
            sb.append("🎉 All settled up! No pending transactions.\n");
        } else {
            for (var s : breakdown.getSettlements()) {
                sb.append(String.format("  %s pays ₹%s → %s\n",
                        s.getFrom().getFullName(),
                        s.getAmount().setScale(2).toPlainString(),
                        s.getTo().getFullName()));
            }
        }
        sb.append("\n");

        // Member Balances
        sb.append("INDIVIDUAL BALANCES\n");
        sb.append("───────────────────────────────────────────────\n");
        for (var m : summary.getMembers()) {
            String name = m.getUser().getId().equals(user.getId())
                    ? "You (" + m.getUser().getFullName() + ")"
                    : m.getUser().getFullName();
            sb.append(String.format("  %-25s  Paid: ₹%-10s  Share: ₹%-10s  Net: %s₹%s\n",
                    name,
                    m.getTotalPaid().setScale(2).toPlainString(),
                    m.getTotalOwed().setScale(2).toPlainString(),
                    m.getNetBalance().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "-",
                    m.getNetBalance().abs().setScale(2).toPlainString()));
        }
        sb.append("\n");

        // What you receive / What you owe
        sb.append("YOUR SUMMARY\n");
        sb.append("───────────────────────────────────────────────\n");
        var myBalance = summary.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .findFirst().orElse(null);
        if (myBalance != null) {
            sb.append("  Total you paid: ₹").append(myBalance.getTotalPaid().setScale(2).toPlainString()).append("\n");
            sb.append("  Your share:     ₹").append(myBalance.getTotalOwed().setScale(2).toPlainString()).append("\n");
            if (myBalance.getNetBalance().compareTo(BigDecimal.ZERO) >= 0) {
                sb.append("  Net balance:    +₹").append(myBalance.getNetBalance().setScale(2).toPlainString())
                        .append(" (owed to you)\n");
            } else {
                sb.append("  Net balance:    -₹").append(myBalance.getNetBalance().abs().setScale(2).toPlainString())
                        .append(" (you owe)\n");
            }
        }
        sb.append("\n");

        // Expense List
        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        sb.append("EXPENSE LIST\n");
        sb.append("───────────────────────────────────────────────\n");
        sb.append(String.format("  %-25s %-12s %-15s\n", "Expense", "Amount", "Paid By"));
        sb.append("  ─────────────────────────────────────────────\n");
        for (var e : expenses) {
            sb.append(String.format("  %-25s ₹%-10s %-15s\n",
                    truncate(e.getTitle(), 25),
                    e.getTotalAmount().setScale(2).toPlainString(),
                    e.getPaidBy().getFullName()));
        }
        sb.append("\n");

        // Footer
        sb.append("═══════════════════════════════════════════════\n");
        sb.append("  Generated by Wandrr · ").append(LocalDate.now().format(fmt)).append("\n");
        sb.append("  https://wandrr-production-app.web.app\n");
        sb.append("═══════════════════════════════════════════════\n");

        return sb.toString();
    }

    private String truncate(String s, int len) {
        if (s == null)
            return "";
        return s.length() > len ? s.substring(0, len - 2) + ".." : s;
    }
}
