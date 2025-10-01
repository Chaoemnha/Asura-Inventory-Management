
package com.inventory.controller;

import com.inventory.service.impl.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class RAGController {
    private final RagService ragService;

    @GetMapping("/query")
    public String query(@RequestParam String question) {
        return ragService.query(question);
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            if (question == null || question.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Cau hoi khong duoc de trong. ");
                return ResponseEntity.badRequest().body(response);
            }
            String answer = ragService.query(question);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            Map<String, String> assistantMessage = new HashMap<>();
            assistantMessage.put("message", answer);
            response.put("assistantMessage", assistantMessage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
