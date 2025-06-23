package cqrses.rest;

import cqrses.service.InduErrorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/indu-errors")
public class InduErrorController {

    private final InduErrorService induErrorService;

    @PostMapping("/init")
    public ResponseEntity<String> initGroup(@RequestBody Map<String, Object> body) {
        String groupId = body.get("groupId").toString();
        induErrorService.initializeGroup(groupId);
        return ResponseEntity.ok("✅ Group initialized: " + groupId);
    }

    @PostMapping("/add")
    public ResponseEntity<String> addError(@RequestBody Map<String, Object> body) {
        String groupId = body.get("groupId").toString();
        Double amount = Double.parseDouble(body.get("amount").toString());

        induErrorService.addInduError(groupId, amount);
        return ResponseEntity.ok("✅ Error event added for group: " + groupId);
    }

    @PostMapping("/compensation")
    public ResponseEntity<String> addCompensation(@RequestBody Map<String, Object> body) {
        String groupId = body.get("groupId").toString();
        Double amount = Double.parseDouble(body.get("amount").toString());

        induErrorService.addCompensation(groupId, amount);
        return ResponseEntity.ok("✅ Compensation event added for group: " + groupId);
    }

    @PostMapping("/process")
    public ResponseEntity<String> process(@RequestBody Map<String, Object> body) {
        String groupId = body.get("groupId").toString();
        try {
            double total = induErrorService.processErrors(groupId);
            return ResponseEntity.ok("🔁 Processing complete. Net total: " + total);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }
}
