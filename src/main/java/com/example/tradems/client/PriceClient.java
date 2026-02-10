package com.example.tradems.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "price-ms",url = "http://localhost:8080")
public interface PriceClient {

    @GetMapping("/api/crypto/price/{symbol}")
    String getRealtimePrice(@PathVariable("symbol") String symbol);
}
