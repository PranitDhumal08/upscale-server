package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.MessageData;
import com.upscale.upscale.entity.project.Message;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.MessageRepo;
import com.upscale.upscale.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepo messageRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private InboxService inboxService;

    public boolean sendMessage(String emailId, MessageData messageData) {

        if (messageData == null || messageData.getRecipients().isEmpty()) {
            return false;
        }

        User user = userService.getUser(emailId);

        if (user == null) {
            return false;
        }

        Message message = new Message();
        message.setSender(user.getId());
        message.setSubject(messageData.getSubject());
        message.setBody(messageData.getBody());
        message.setRecipients(messageData.getRecipients());

        messageRepo.save(message);

        for (String recipientEmail : messageData.getRecipients()) {
            if (recipientEmail.contains("@")) {
                inboxService.sendProjectMessage(message, emailId, recipientEmail);
            }
        }

        return true;
    }

    public List<Message> getMessagesForUser(String projectId) {
        //User user = userService.getUser(email);
       return messageRepo.findByRecipientsContaining(projectId);

    }
}
