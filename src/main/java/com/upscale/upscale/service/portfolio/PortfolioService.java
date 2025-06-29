package com.upscale.upscale.service.portfolio;

import com.upscale.upscale.dto.portfolio.CreatePortFolio;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.PortfolioRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PortfolioService {

    @Autowired
    private PortfolioRepo portfolioRepo;

    @Autowired
    @Lazy
    private UserService userService;

    public void save(Portfolio portfolio) {
        portfolioRepo.save(portfolio);
    }

    public boolean createPortfolio(String emailId, CreatePortFolio createPortFolio) {
        Portfolio portfolio = new Portfolio();

        if(createPortFolio != null) {

            User user = userService.getUser(emailId);

            portfolio.setOwnerId(user.getId());
            portfolio.setPortfolioName(createPortFolio.getPortfolioName());
            portfolio.setPrivacy(createPortFolio.getPrivacy());
            portfolio.setDefaultView(createPortFolio.getDefaultView());
            //portfolio.setTeammates(createPortFolio.getTeammates());

            portfolioRepo.save(portfolio);
            log.info("Portfolio created successfully {}", createPortFolio.getPortfolioName());
            return true;
        }
        return false;
    }

    public List<Portfolio> getPortFolio(String emailId) {

        String userId = userService.getUser(emailId).getId();

        return portfolioRepo.findByOwnerId(userId);
    }

    public Optional<Portfolio> getPortfolio(String id) {
        return portfolioRepo.findById(id);
    }
}
