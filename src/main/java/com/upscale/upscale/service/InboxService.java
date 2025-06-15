package com.upscale.upscale.service;

import com.upscale.upscale.dto.InboxData;
import com.upscale.upscale.entity.Inbox;
import com.upscale.upscale.entity.People;
import com.upscale.upscale.entity.Task;
import com.upscale.upscale.repository.InboxRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    public List<InboxData> getInbox(String emailId){
        List<Inbox> inboxes = inboxRepo.findByReceiverId(emailId);

        List<InboxData> inboxDataList = new ArrayList<>();

        if(inboxes != null && !inboxes.isEmpty()){
            for (Inbox inbox : inboxes) {
                InboxData inboxData = new InboxData();

                inboxData.setSenderId(inbox.getSenderId());
                inboxData.setReceiverId(inbox.getReceiverId());
                inboxData.setContent(inbox.getContent());

                inboxDataList.add(inboxData);
            }
            return inboxDataList;
        }
        return new ArrayList<>();
    }

    public void sendTaskDetails(Task task,String senderEmailId,String receiverEmailId){
        Inbox inbox = new Inbox();

        inbox.setSenderId(senderEmailId);
        inbox.setReceiverId(receiverEmailId);

        String context = "You have given a task "+task.getTaskName();

        inbox.setContent(context);

        saveInbox(inbox);
    }
}
