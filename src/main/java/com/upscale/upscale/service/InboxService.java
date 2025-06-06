package com.upscale.upscale.service;

import com.upscale.upscale.dto.InboxData;
import com.upscale.upscale.entity.Inbox;
import com.upscale.upscale.entity.People;
import com.upscale.upscale.repository.InboxRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InboxService {

    @Autowired
    private InboxRepo inboxRepo;

    public void saveInbox(Inbox inbox){
        inboxRepo.save(inbox);
    }

    public void updateInbox(String emailId, Inbox inbox){
        inboxRepo.save(inbox);
    }

    public void sendInviteInbox(String senderEmailId, String receiverEmailId, People people){
        Inbox inbox = new Inbox();

        inbox.setSenderId(senderEmailId);
        inbox.setReceiverId(receiverEmailId);

        String context = "You have invite for the projects"+people.getProjectsName();

        inbox.setContent(context);

        saveInbox(inbox);
    }

    public InboxData getInbox(String emailId){
        Inbox inbox = inboxRepo.findByReceiverId(emailId);

        if(inbox != null){
            InboxData inboxData = new InboxData();

            inboxData.setSenderId(inbox.getSenderId());
            inboxData.setReceiverId(inboxData.getReceiverId());
            inboxData.setContent(inbox.getContent());

            return inboxData;
        }
        return null;

    }
}
