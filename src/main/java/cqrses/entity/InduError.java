package cqrses.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InduError {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private InduErrorStatus status;

    private Double amount;
}
