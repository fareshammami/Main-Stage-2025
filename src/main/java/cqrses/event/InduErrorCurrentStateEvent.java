package cqrses.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InduErrorCurrentStateEvent {
    private String userId;
    private Double totalUntreatedAmount;
}
