package cqrses.projection;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "userInduStates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInduState {

    @Id
    private String userId;

    private List<InduErrorInfo> induErrors;
    private List<CompensationInfo> compensations;

    // Total of handled InduErrors
    private Double totalHandledInduErrors = 0.0;

    // Total of handled Compensations
    private Double totalHandledCompensations = 0.0;

    // Total of untreated InduErrors (optional)
    private Double totalUntreatedInduErrors = 0.0;

    // Total of untreated Compensations (optional)
    private Double totalUntreatedCompensations = 0.0;

    // Net total = all handled InduErrors - all handled Compensations
    private Double netTotal = 0.0;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InduErrorInfo {
        private String induErrorId;
        private Double amount;
        private String status; // NOT_TRAITED or TRAITED
        private Instant createdAt;
        private Instant handledAt; // null if not handled yet
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompensationInfo {
        private String compensationId;
        private Double amount;
        private String status; // NOT_TRAITED or TRAITED
        private Instant createdAt;
        private Instant handledAt; // null if not handled yet
    }

    /**
     * Recalculates totals for the current state:
     * - totalHandledInduErrors
     * - totalHandledCompensations
     * - totalUntreatedInduErrors
     * - totalUntreatedCompensations
     * - netTotal
     */
    public void recalcTotals() {
        double untreatedIndu = induErrors.stream()
                .filter(e -> "NOT_TRAITED".equals(e.getStatus()))
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();

        double untreatedComp = compensations.stream()
                .filter(c -> "NOT_TRAITED".equals(c.getStatus()))
                .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0.0)
                .sum();

        double handledIndu = induErrors.stream()
                .filter(e -> "TRAITED".equals(e.getStatus()))
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();

        double handledComp = compensations.stream()
                .filter(c -> "TRAITED".equals(c.getStatus()))
                .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0.0)
                .sum();

        this.totalUntreatedInduErrors = untreatedIndu;
        this.totalUntreatedCompensations = untreatedComp;
        this.totalHandledInduErrors = handledIndu;
        this.totalHandledCompensations = handledComp;
        this.netTotal = handledIndu - handledComp;
    }
}
