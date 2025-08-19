package cqrses.projection;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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

    private Double totalUntreatedInduErrors;
    private Double totalUntreatedCompensations;
    private Double netTotal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InduErrorInfo {
        private String induErrorId;
        private Double amount;
        private String status; // e.g., "NOT_TRAITED", "TRAITED"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompensationInfo {
        private String compensationId;
        private Double amount;
        private String status; // e.g., "NOT_TRAITED", "TRAITED"
    }
}
