package com.example.tradems.controller;

import com.example.tradems.dto.request.OpenTradeRequest;
import com.example.tradems.dto.response.OpenTradeResponse;
import com.example.tradems.dto.response.PendingTradeResponse;
import com.example.tradems.model.TradeEntity;
import com.example.tradems.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/trades/user")
@RequiredArgsConstructor

public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/open")
    public ResponseEntity<TradeEntity> openTrade(@Valid @RequestBody OpenTradeRequest request) {
        return ResponseEntity.ok(tradeService.openTrade(request));
    }

    @GetMapping("/active/{userId}")
    public ResponseEntity<List<OpenTradeResponse>> getActiveTrades(@PathVariable Long userId) {
        return ResponseEntity.ok(tradeService.getActiveTrades(userId));
    }

    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<PendingTradeResponse>> getPendingOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(tradeService.getUserPendingOrders(userId));
    }

    @DeleteMapping("/cancel/{tradeId}")
    public ResponseEntity<String> cancelTrade(@PathVariable UUID tradeId) {
        tradeService.cancelPendingTrade(tradeId);
        return ResponseEntity.ok("Sifariş uğurla ləğv edildi və balans bərpa olundu.");
    }

    @DeleteMapping("/close/{tradeId}")
    public ResponseEntity<String> closeTrade(@PathVariable UUID tradeId) {
        tradeService.closeTradeManually(tradeId);
        return ResponseEntity.ok("Pozisiya bazar qiyməti ilə bağlandı və mənfəət/zərər balansa köçürüldü.");
    }
}


