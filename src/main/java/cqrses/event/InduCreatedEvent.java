package cqrses.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InduCreatedEvent {
    private String induErrorId;
    private Double amount;
}