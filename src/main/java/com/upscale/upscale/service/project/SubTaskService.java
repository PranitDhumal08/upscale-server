package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.task.SubTaskData;
import com.upscale.upscale.entity.project.SubTask;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.SubTaskRepo;
import com.upscale.upscale.service.UserLookupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SubTaskService {

    @Autowired
    private SubTaskRepo subTaskRepo;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserLookupService userLookupService;

    public SubTask save(SubTask subTask) {
        return subTaskRepo.save(subTask);
    }

    public SubTask createSubTask(SubTaskData data, String creatorEmail) {
        User creator = userLookupService.getUserByEmail(creatorEmail);
        String creatorId = creator != null ? creator.getId() : null;

        SubTask st = new SubTask();
        st.setTaskName(data.getTaskName());
        st.setDate(data.getDate());
        st.setStartDate(data.getStartDate());
        st.setEndDate(data.getEndDate());
        st.setPriority(data.getPriority());
        st.setStatus(data.getStatus());
        st.setDescription(data.getDescription());
        st.setCompleted(data.isCompleted());
        // Map project from DTO: support single projectId (from path) or list projectIds
        List<String> pids = new ArrayList<>();
        if (data.getProjectId() != null && !data.getProjectId().isBlank()) {

            pids.add(data.getProjectId());
            st.setProjectIds(data.getProjectId());
        }
        st.setCreatedId(creatorId);

        List<String> assigneeIds = new ArrayList<>();
        if (data.getAssignId() != null && !data.getAssignId().isEmpty()) {
            for (String assigneeEmail : data.getAssignId()) {
                User u = userLookupService.getUserByEmail(assigneeEmail);
                if (u != null) assigneeIds.add(u.getId());
            }
        } else if (creatorId != null) {
            // default assign to creator
            assigneeIds.add(creatorId);
        }
        st.setAssignId(assigneeIds);

        SubTask saved = subTaskRepo.save(st);
        log.info("Created SubTask {} for creator {}", saved.getId(), creatorEmail);

        // If parent task provided, link
        if (data.getParentTaskId() != null && !data.getParentTaskId().isBlank()) {
            linkSubTaskToParent(saved.getId(), data.getParentTaskId());
        }
        return saved;
    }

    public void linkSubTaskToParent(String subTaskId, String parentTaskId) {
        Task parent = taskService.getTask(parentTaskId);
        if (parent == null) {
            log.warn("Parent task {} not found; subtask {} created without linkage", parentTaskId, subTaskId);
            return;
        }
        List<String> subTaskIds = parent.getSubTaskIds();
        if (subTaskIds == null) {
            subTaskIds = new ArrayList<>();
            parent.setSubTaskIds(subTaskIds);
        }
        if (!subTaskIds.contains(subTaskId)) {
            subTaskIds.add(subTaskId);
            taskService.save(parent);
            log.info("Linked SubTask {} to parent Task {}", subTaskId, parentTaskId);
        }
    }

    public SubTask updateCompleted(String subTaskId, boolean completed) {
        return subTaskRepo.findById(subTaskId).map(st -> {
            st.setCompleted(completed);
            SubTask saved = subTaskRepo.save(st);
            log.info("Updated SubTask {} completed={} ", subTaskId, completed);
            return saved;
        }).orElse(null);
    }
}
