package cqrses.event;

import cqrses.entity.InduErrorStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InduErrorCreatedEvent {
    private String induErrorId;
    private Double amount;
    private InduErrorStatus status;
}