package com.upscale.upscale.repository;

import com.upscale.upscale.entity.portfolio.Portfolio;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepo extends MongoRepository<Portfolio, String> {
    List<Portfolio> findByOwnerId(String userId);

    Optional<Portfolio> findById(String id);
}
