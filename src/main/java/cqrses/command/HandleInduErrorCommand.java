package cqrses.command;

import lombok.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HandleInduErrorCommand {
    @TargetAggregateIdentifier
    private String groupId;
    private String induErrorId;
    private Double amount;
}
