package com.example.coders.controllers;

import com.example.coders.dtos.CodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class RealTimeController {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/edit/{projectSlug}")
    @SendTo("/topic/editor/{projectSlug}")
    public void sendCodeUpdate(CodeDto codeUpdate, @DestinationVariable String projectSlug) {
        logger.warn(codeUpdate.getFile());
        messagingTemplate.convertAndSend("/topic/editor/" + projectSlug, codeUpdate);
    }
}