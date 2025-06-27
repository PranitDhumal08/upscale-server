package com.upscale.upscale.repository;

import com.upscale.upscale.entity.Section;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SectionRepo extends MongoRepository<Section, String> {
}
