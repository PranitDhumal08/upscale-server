package com.upscale.upscale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        // Keep only narrowly-scoped converters. Do NOT convert ArrayList to String globally,
        // otherwise fields like List<String> (e.g., receiverIds) may be persisted as a single String.
        converters.add(new ArrayListToHashMapConverter());
        return new MongoCustomConversions(converters);
    }

    /**
     * Converter to handle ArrayList to HashMap conversion
     * This handles cases where MongoDB stores data as ArrayList but Java expects HashMap
     */
    public static class ArrayListToHashMapConverter implements Converter<ArrayList, HashMap> {
        @Override
        public HashMap convert(ArrayList source) {
            HashMap<String, Object> result = new HashMap<>();
            if (source != null && !source.isEmpty()) {
                // If ArrayList contains a single email string, treat it as userEmailid
                if (source.size() == 1 && source.get(0) instanceof String) {
                    result.put("userEmailid", source.get(0));
                } else {
                    // For other cases, convert ArrayList to indexed HashMap
                    for (int i = 0; i < source.size(); i++) {
                        result.put(String.valueOf(i), source.get(i));
                    }
                }
            }
            return result;
        }
    }

    // Removed global ArrayList->String conversion to avoid unintended write-time coercion of lists into strings.
}
