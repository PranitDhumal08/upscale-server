package com.upscale.upscale.service;

import com.upscale.upscale.dto.GoalData;
import com.upscale.upscale.entity.Goal;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.repository.GoalRepo;
import com.upscale.upscale.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoalService {

    @Autowired
    private GoalRepo goalRepo;
    @Autowired
    private UserRepo userRepo;

    public void save(Goal goal) {
        goalRepo.save(goal);
    }
    public String userId(String emailId){
        User user = userRepo.findByEmailId(emailId);
        return user.getId();
    }

    public boolean setGoal(String emailId, GoalData goalData){
        if(goalData != null){

            Goal goal = new Goal();

            goal.setUserId(userId(emailId));
            goal.setGoalTitle(goalData.getGoalTitle());
            goal.setGoalOwner(userId(emailId));
            goal.setTimePeriod(goalData.getTimePeriod());
            goal.setPrivacy(goalData.getPrivacy());
            goal.setMembers(goalData.getMembers());

            save(goal);
            return true;
        }
        return false;
    }

    public GoalData getGoal(String emailId){
        String userId = userId(emailId);
        Goal goal = goalRepo.findByUserId(userId);

        if(goal != null){

            GoalData goalData = new GoalData();
            goalData.setGoalTitle(goal.getGoalTitle());
            goalData.setTimePeriod(goal.getTimePeriod());
            goalData.setPrivacy(goal.getPrivacy());
            goalData.setMembers(goal.getMembers());
            goalData.setGoalOwner(userId);
            return goalData;
        }
        return null;
    }

}
