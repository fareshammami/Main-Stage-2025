package cqrses.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventCommand {
    @TargetAggregateIdentifier
    private String id;
    private String newMessage;
}
