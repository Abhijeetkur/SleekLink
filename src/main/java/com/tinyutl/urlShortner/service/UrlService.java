package com.tinyutl.urlShortner.service;

import com.tinyutl.urlShortner.repository.UrlRepository;
import com.tinyutl.urlShortner.encoder.Base62Encoder;
import com.tinyutl.urlShortner.entity.UrlMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UrlService {
    @Autowired
    private UrlRepository urlRepository;

    public String shortenUrl(String longUrl){
        UrlMapping entity = new UrlMapping();
        entity.setLongurl(longUrl);
        entity = urlRepository.save(entity);

        String shortCode = Base62Encoder.encode(entity.getId());
        entity.setShortCode(shortCode);

        return urlRepository.save(entity).getShortCode();
    }

    public String getLongUrl(String shortCode) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        return mapping.getLongurl();
    }
}
