package cqrses.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compensation {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private CompensationStatus status;

    private Double amount;
}