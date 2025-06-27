package com.upscale.upscale.service;

import com.upscale.upscale.entity.Section;
import com.upscale.upscale.repository.SectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SectionService {

    @Autowired
    private SectionRepo sectionRepo;

    public Section save(Section section) {
        return sectionRepo.save(section);
    }

    public Optional<Section> findById(String id) {
        return sectionRepo.findById(id);
    }
}
