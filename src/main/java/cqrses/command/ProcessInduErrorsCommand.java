package cqrses.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessInduErrorsCommand {
    @TargetAggregateIdentifier
    private String userId;
    private Double totalUntreatedAmount;
}