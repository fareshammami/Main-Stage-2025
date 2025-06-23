package cqrses.event;

import cqrses.entity.InduErrorStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InduErrorHandledEvent {
    private String induErrorId;
    private InduErrorStatus status;
}
