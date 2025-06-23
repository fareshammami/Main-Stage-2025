package cqrses.event;

import cqrses.entity.CompensationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompensationCreatedEvent {
    private String compensationId;
    private Double amount;
    private CompensationStatus status;
}