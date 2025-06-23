package cqrses.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreatInduErrorCommand {
    private String induErrorId;
    private Double amount;
}