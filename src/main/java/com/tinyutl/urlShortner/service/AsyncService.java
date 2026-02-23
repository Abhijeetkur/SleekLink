package com.tinyutl.urlShortner.service;

import com.tinyutl.urlShortner.entity.UrlMapping;
import com.tinyutl.urlShortner.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

    @Autowired
    private UrlRepository urlRepository;

    @Async
    public void incrementClick(String shortCode) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        mapping.setClickCount(mapping.getClickCount() + 1);
        urlRepository.save(mapping);
    }
}
