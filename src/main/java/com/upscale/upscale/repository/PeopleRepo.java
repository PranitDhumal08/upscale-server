package com.upscale.upscale.repository;

import com.upscale.upscale.entity.People;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PeopleRepo extends MongoRepository<People, String> {

}
