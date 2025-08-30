package cqrses.rest;

import cqrses.dto.StreamEventDTO;
import cqrses.entity.EventFilter;
import cqrses.projection.UserInduState;
import cqrses.service.InduErrorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/indu-errors")
@CrossOrigin("*")
public class InduErrorController {

    private final InduErrorService induErrorService;

    @PostMapping("/init")
    public ResponseEntity<?> initUser(@RequestBody Map<String, Object> body) {
        try {
            String userId = body.get("userId").toString();
            induErrorService.initializeUser(userId);
            return ResponseEntity.ok("✅ Group initialized: " + userId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("❌ Erreur d'initialisation: " + e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addError(@RequestBody Map<String, Object> body) {
        try {
            String userId = body.get("userId").toString();
            Double amount = Double.parseDouble(body.get("amount").toString());
            induErrorService.addInduError(userId, amount);
            return ResponseEntity.ok("✅ Error event added for user: " + userId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Erreur lors de l'ajout d'une erreur Indu: " + e.getMessage());
        }
    }

    @PostMapping("/compensation")
    public ResponseEntity<?> addCompensation(@RequestBody Map<String, Object> body) {
        try {
            String userId = body.get("userId").toString();
            Double amount = Double.parseDouble(body.get("amount").toString());
            induErrorService.addCompensation(userId, amount);
            return ResponseEntity.ok("✅ Compensation event added for user: " + userId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Erreur lors de l'ajout de la compensation: " + e.getMessage());
        }
    }

    @PostMapping("/process")
    public ResponseEntity<?> process(@RequestBody Map<String, Object> body) {
        try {
            String userId = body.get("userId").toString();
            double total = induErrorService.processErrors(userId);
            return ResponseEntity.ok("🔁 Traitement terminé. Total net: " + total);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Erreur de traitement: " + e.getMessage());
        }
    }

    @GetMapping("/projection/{userId}")
    public ResponseEntity<List<StreamEventDTO>> getProjectionEvents(
            @PathVariable String userId,
            @RequestParam(defaultValue = "ALL") EventFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate
    ) {
        try {
            List<StreamEventDTO> events = induErrorService.getProjectionEvents(userId, filter, page, size, eventType, fromDate, toDate);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/totals/{userId}")
    public ResponseEntity<Map<String, Object>> getUserTotals(@PathVariable String userId) {
        try {
            UserInduState state = induErrorService.getUserState(userId);
            Map<String, Object> totals = Map.of(
                    "totalHandledInduErrors", state.getTotalHandledInduErrors(),
                    "totalHandledCompensations", state.getTotalHandledCompensations(),
                    "totalUntreatedInduErrors", state.getTotalUntreatedInduErrors(),
                    "totalUntreatedCompensations", state.getTotalUntreatedCompensations(),
                    "netTotal", state.getNetTotal()
            );
            return ResponseEntity.ok(totals);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
