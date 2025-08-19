package cqrses.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StreamEventDTO {
    private String eventType;
    private Object payload;
    private String createdAt;
}
