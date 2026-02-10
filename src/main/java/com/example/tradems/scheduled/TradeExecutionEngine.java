package com.example.tradems.scheduled;
import com.example.tradems.client.PriceClient;
import com.example.tradems.enums.PositionSide;
import com.example.tradems.enums.TradeStatus;
import com.example.tradems.model.TradeEntity;
import com.example.tradems.model.UserEntity;
import com.example.tradems.repository.TradeRepository;
import com.example.tradems.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeExecutionEngine {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final PriceClient priceClient;

    @Scheduled(fixedDelayString = "${trading.engine.fixed-delay:1000}")
    public void runEngine() {
        processPendingOrders();
        processOpenPositions();
    }

    private void processPendingOrders() {
        tradeRepository.findAllByStatus(TradeStatus.PENDING)
                .forEach(this::tryToExecutePendingOrder);
    }

    private void processOpenPositions() {
        tradeRepository.findAllByStatus(TradeStatus.OPEN)
                .forEach(this::checkAndClosePosition);
    }

    @Transactional
    public void tryToExecutePendingOrder(TradeEntity trade) {
        try {
            BigDecimal currentPrice = getCurrentPrice(trade.getSymbol());
            if (isTargetPriceHit(trade, currentPrice)) {
                activateOrder(trade);
            }
        } catch (Exception e) {
            log.error("Pending trade aktivləşdirilərkən xəta (ID: {}): {}", trade.getId(), e.getMessage());
        }
    }

    @Transactional
    public void checkAndClosePosition(TradeEntity trade) {
        try {
            BigDecimal currentPrice = getCurrentPrice(trade.getSymbol());

            if (isLiquidationHit(trade, currentPrice)) {
                finalizePosition(trade, currentPrice, "LIQUIDATED");
            } else if (isStopLossHit(trade, currentPrice)) {
                finalizePosition(trade, currentPrice, "STOP_LOSS");
            } else if (isTakeProfitHit(trade, currentPrice)) {
                finalizePosition(trade, currentPrice, "TAKE_PROFIT");
            }
        } catch (Exception e) {
            log.error("Pozisiya yoxlanarkən xəta (ID: {}): {}", trade.getId(), e.getMessage());
        }
    }

    private void activateOrder(TradeEntity trade) {
        UserEntity user = userRepository.findByIdWithLock(trade.getUserId())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));

        user.setFrozenBalance(user.getFrozenBalance().subtract(trade.getMargin()));

        trade.setStatus(TradeStatus.OPEN);
        trade.setOpenTime(LocalDateTime.now());

        userRepository.save(user);
        tradeRepository.save(trade);
        log.info("Limit order aktivləşdi: {} {} @ {}", trade.getSymbol(), trade.getSide(), trade.getEntryPrice());
    }

    private void finalizePosition(TradeEntity trade, BigDecimal exitPrice, String reason) {
        UserEntity user = userRepository.findByIdWithLock(trade.getUserId())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));

        BigDecimal pnlValue = calculatePnL(trade, exitPrice);
        BigDecimal payout = trade.getMargin().add(pnlValue).max(BigDecimal.ZERO);

        user.setVirtualBalance(user.getVirtualBalance().add(payout));

        trade.setStatus(TradeStatus.CLOSED);
        trade.setClosePrice(exitPrice);
        trade.setCloseTime(LocalDateTime.now());
        trade.setPnl(pnlValue);

        userRepository.save(user);
        tradeRepository.save(trade);
        log.info("Pozisiya bağlandı ({}): {} PnL: {}", reason, trade.getId(), pnlValue);
    }


    private boolean isTargetPriceHit(TradeEntity t, BigDecimal price) {
        return (t.getSide() == PositionSide.LONG) ? price.compareTo(t.getEntryPrice()) <= 0
                : price.compareTo(t.getEntryPrice()) >= 0;
    }

    private boolean isTakeProfitHit(TradeEntity t, BigDecimal price) {
        if (t.getTakeProfit() == null) return false;
        return (t.getSide() == PositionSide.LONG) ? price.compareTo(t.getTakeProfit()) >= 0
                : price.compareTo(t.getTakeProfit()) <= 0;
    }

    private boolean isStopLossHit(TradeEntity t, BigDecimal price) {
        if (t.getStopLoss() == null) return false;
        return (t.getSide() == PositionSide.LONG) ? price.compareTo(t.getStopLoss()) <= 0
                : price.compareTo(t.getStopLoss()) >= 0;
    }

    private boolean isLiquidationHit(TradeEntity t, BigDecimal price) {
        return (t.getSide() == PositionSide.LONG) ? price.compareTo(t.getLiquidationPrice()) <= 0
                : price.compareTo(t.getLiquidationPrice()) >= 0;
    }

    private BigDecimal calculatePnL(TradeEntity trade, BigDecimal exitPrice) {
        BigDecimal entryPrice = trade.getEntryPrice();
        BigDecimal leverage = new BigDecimal(trade.getLeverage());

        BigDecimal diff = (trade.getSide() == PositionSide.LONG) ? exitPrice.subtract(entryPrice)
                : entryPrice.subtract(exitPrice);

        return diff.divide(entryPrice, 8, RoundingMode.HALF_UP)
                .multiply(trade.getMargin())
                .multiply(leverage);
    }

    private BigDecimal getCurrentPrice(String symbol) {
        try {
            String raw = priceClient.getRealtimePrice(symbol);
            return new BigDecimal(raw.split(": ")[1].replace(" USD", "").trim());
        } catch (Exception e) {
            log.error("Qiymət oxunarkən xəta ({}): {}", symbol, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}