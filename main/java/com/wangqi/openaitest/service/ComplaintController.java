package com.wangqi.openaitest.service;

import com.alibaba.fastjson.JSONObject;
import com.wangqi.openaitest.controller.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/api/complaint")
@CrossOrigin(origins = "*")
public class ComplaintController {
    @Autowired
    private ComplaintService complaintService;
    // 使用ConcurrentHashMap保证线程安全
    private static final ConcurrentHashMap<String, List<JSONObject>> sessionHistories = new ConcurrentHashMap<>();
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startComplaint() {
        String initialComplaint = complaintService.generateRandomComplaint();
        List<JSONObject> history = complaintService.initConversation(initialComplaint);
        String sessionId = UUID.randomUUID().toString();
        sessionHistories.put(sessionId, history);
        Map<String, Object> response = new HashMap<>();
        response.put("complaint", initialComplaint);
        response.put("sessionId",sessionId);
        response.put("history", history);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reply")
    public ResponseEntity<Map<String, Object>> handleReply(
            @RequestBody Map<String, String> request) throws IOException {

        String sessionId = request.get("sessionId");
        String message = request.get("message");
        // 从存储中获取历史对话
        List<JSONObject> history = sessionHistories.get(sessionId);
        history= complaintService.processReply(history,
                message);
        String aiResponse = complaintService.getGPT4Response(history);
// 更新存储
        history.add(createMessage("assistant", aiResponse));
        sessionHistories.put(sessionId, history);
        Map<String, Object> response = new HashMap<>();
        response.put("reply", aiResponse);
        response.put("history", history);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/feedback")
    public ResponseEntity<Map<String, String>> getFeedback(
            @RequestBody Map<String, Object> request) throws IOException {

        String sessionId = (String) request.get("sessionId");
        List<JSONObject> history = sessionHistories.get(sessionId);

        String feedback = complaintService.getGPT4Feedback(history);

        Map<String, String> response = new HashMap<>();
        response.put("feedback", feedback);

        return ResponseEntity.ok(response);
    }
    private JSONObject createMessage(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
}
