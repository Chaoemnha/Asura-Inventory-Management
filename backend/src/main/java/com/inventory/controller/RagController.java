package com.inventory.controller;

import com.inventory.service.impl.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    @Autowired
    private RagService ragService;
    //Endpoint hien co
    @GetMapping("/query")
    public String query(@RequestParam String question){
        return ragService.query(question);
    }
    //Endpoint moi cho chat.js
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request){
        try{
            //Lay question tu body
            String question = (String) request.get("question");

            if(question == null || question.trim().isEmpty()){
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Cau hoi khong duoc de trong. ");
                return ResponseEntity.badRequest().body(response);
            }
            //Goi service de xu ly cau hoi
            String answer = ragService.query(question);
            //Tao response theo dinh dang chi dinh
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            Map<String, String> assistantMessage = new HashMap<>();
            assistantMessage.put("message", answer);
            response.put("assistantMessage", assistantMessage);

            return ResponseEntity.ok(response);
        }
        catch(Exception e){
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
