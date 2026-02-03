package com.pos.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/heartbeat")
    public Map<String, Object> heartbeat() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ALIVE");
        status.put("timestamp", System.currentTimeMillis());
        status.put("message", "High-priority diagnostic endpoint active.");
        return status;
    }
}
