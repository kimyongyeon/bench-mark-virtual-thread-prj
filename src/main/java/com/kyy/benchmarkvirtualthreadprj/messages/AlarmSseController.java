package com.kyy.benchmarkvirtualthreadprj.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
@CrossOrigin("*")
public class AlarmSseController {

    private final AlarmSseService sseService;

    @GetMapping(value = "/alarm", produces = "text/event-stream")
    public SseEmitter connect(@RequestParam String userId) {
        return sseService.connect(userId);
    }

    /** ⭐ sendBeacon으로 호출되는 API */
    @PostMapping("/disconnect")
    public void disconnect(@RequestBody String body) throws JsonProcessingException {
        System.out.println("BEACON BODY = " + body);

        DisconnectReq req = new ObjectMapper().readValue(body, DisconnectReq.class);
        sseService.disconnect(req.getUserId());
    }


}
