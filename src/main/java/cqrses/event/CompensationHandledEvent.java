package cqrses.event;

import cqrses.entity.CompensationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompensationHandledEvent {
    private String compensationId;
    private String userId;
    private CompensationStatus status;
}