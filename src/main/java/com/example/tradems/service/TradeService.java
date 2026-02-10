package com.example.tradems.service;

import com.example.tradems.client.PriceClient;
import com.example.tradems.dto.request.OpenTradeRequest;
import com.example.tradems.dto.response.OpenTradeResponse;
import com.example.tradems.dto.response.PendingTradeResponse;
import com.example.tradems.enums.PositionSide;
import com.example.tradems.enums.TradeStatus;
import com.example.tradems.exception.InsufficientFundsException;
import com.example.tradems.exception.InvalidTradeParameterException;
import com.example.tradems.exception.UserNotFoundException;
import com.example.tradems.model.TradeEntity;
import com.example.tradems.model.UserEntity;
import com.example.tradems.repository.TradeRepository;
import com.example.tradems.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final PriceClient priceClient;


    @Transactional
    public TradeEntity openTrade(OpenTradeRequest request) {
        UserEntity user = userRepository.findByIdWithLock(request.userId())
                .orElseThrow(() ->  new UserNotFoundException("İstifadəçi tapılmadı"));

        validateUserAndMargin(user, request.margin(), request.leverage());

        BigDecimal entryPrice = resolveEntryPrice(request);
        TradeStatus status = (request.targetPrice() != null) ? TradeStatus.PENDING : TradeStatus.OPEN;

        validateTPSL(request.side(), entryPrice, request.takeProfit(), request.stopLoss());
        updateUserBalanceForOpening(user, request.margin(), status);

        TradeEntity trade = createTradeEntity(request, entryPrice, status);

        userRepository.save(user);
        return tradeRepository.save(trade);
    }

    public List<OpenTradeResponse> getActiveTrades(Long userId) {
        return tradeRepository.findByUserIdAndStatus(userId, TradeStatus.OPEN)
                .stream()
                .map(this::mapToOpenTradeResponse)
                .toList();
    }

    public List<PendingTradeResponse> getUserPendingOrders(Long userId) {
        return tradeRepository.findByUserIdAndStatus(userId, TradeStatus.PENDING)
                .stream()
                .map(t -> new PendingTradeResponse(
                        t.getId(), t.getSymbol(), t.getSide(),
                        t.getEntryPrice(), t.getMargin(), t.getLeverage()
                )).toList();
    }

    @Transactional
    public void cancelPendingTrade(UUID tradeId) {
        TradeEntity trade = findTradeById(tradeId);
        validateStatus(trade, TradeStatus.PENDING, "Yalnız gözləyən sifarişlər ləğv edilə bilər");

        UserEntity user = userRepository.findByIdWithLock(trade.getUserId())
                .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı"));

        restoreBalance(user, trade.getMargin());
        finalizeTrade(trade, TradeStatus.CLOSED, null, null);

        userRepository.save(user);
        tradeRepository.save(trade);
    }

    @Transactional
    public void closeTradeManually(UUID tradeId) {
        TradeEntity trade = findTradeById(tradeId);
        validateStatus(trade, TradeStatus.OPEN, "Yalnız aktiv pozisiyalar bağlana bilər");

        UserEntity user = userRepository.findByIdWithLock(trade.getUserId())
                .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı"));

        BigDecimal closePrice = getRealtimePrice(trade.getSymbol());
        BigDecimal pnl = calculateInstantPnL(trade, closePrice);
        BigDecimal payout = trade.getMargin().add(pnl).max(BigDecimal.ZERO);

        user.setVirtualBalance(user.getVirtualBalance().add(payout));
        finalizeTrade(trade, TradeStatus.CLOSED, closePrice, pnl);

        userRepository.save(user);
        tradeRepository.save(trade);
    }


    private BigDecimal resolveEntryPrice(OpenTradeRequest request) {
        if (request.targetPrice() != null && request.targetPrice().compareTo(BigDecimal.ZERO) > 0) {
            return request.targetPrice();
        }
        BigDecimal marketPrice = getRealtimePrice(request.symbol());
        if (marketPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Qiymət servisi xətası: " + request.symbol());
        }
        return marketPrice;
    }

    private void updateUserBalanceForOpening(UserEntity user, BigDecimal margin, TradeStatus status) {
        user.setVirtualBalance(user.getVirtualBalance().subtract(margin));
        if (status == TradeStatus.PENDING) {
            user.setFrozenBalance(safeGet(user.getFrozenBalance()).add(margin));
        }
    }

    private void restoreBalance(UserEntity user, BigDecimal margin) {
        user.setFrozenBalance(safeGet(user.getFrozenBalance()).subtract(margin));
        user.setVirtualBalance(safeGet(user.getVirtualBalance()).add(margin));
    }

    private void finalizeTrade(TradeEntity trade, TradeStatus status, BigDecimal closePrice, BigDecimal pnl) {
        trade.setStatus(status);
        trade.setClosePrice(closePrice);
        trade.setPnl(pnl);
        trade.setCloseTime(LocalDateTime.now());
    }

    private BigDecimal calculateInstantPnL(TradeEntity trade, BigDecimal currentPrice) {
        BigDecimal diff = (trade.getSide() == PositionSide.LONG)
                ? currentPrice.subtract(trade.getEntryPrice())
                : trade.getEntryPrice().subtract(currentPrice);

        return diff.divide(trade.getEntryPrice(), 8, RoundingMode.HALF_UP)
                .multiply(trade.getMargin())
                .multiply(new BigDecimal(trade.getLeverage()));
    }

    private BigDecimal getRealtimePrice(String symbol) {
        try {
            String raw = priceClient.getRealtimePrice(symbol);
            String priceStr = raw.split(": ")[1].replace(" USD", "").trim();
            return new BigDecimal(priceStr);
        } catch (Exception e) {
            log.error("Qiymət alınarkən xəta: {}", symbol, e);
            return BigDecimal.ZERO;
        }
    }


    private OpenTradeResponse mapToOpenTradeResponse(TradeEntity trade) {
        BigDecimal current = getRealtimePrice(trade.getSymbol());
        BigDecimal pnl = calculateInstantPnL(trade, current);
        BigDecimal pnlPerc = pnl.divide(trade.getMargin(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        return new OpenTradeResponse(
                trade.getId(), trade.getSymbol(), trade.getSide(), trade.getEntryPrice(),
                current, trade.getMargin(), trade.getLeverage(), pnl, pnlPerc
        );
    }

    private TradeEntity createTradeEntity(OpenTradeRequest req, BigDecimal entry, TradeStatus status) {
        TradeEntity trade = new TradeEntity();
        trade.setUserId(req.userId());
        trade.setSymbol(req.symbol().toUpperCase());
        trade.setSide(req.side());
        trade.setMargin(req.margin());
        trade.setLeverage(req.leverage());
        trade.setEntryPrice(entry);
        trade.setTakeProfit(req.takeProfit());
        trade.setStopLoss(req.stopLoss());
        trade.setLiquidationPrice(calculateLiquidationPrice(entry, req.leverage(), req.side()));
        trade.setStatus(status);
        trade.setOpenTime(LocalDateTime.now());
        return trade;
    }

    private void validateUserAndMargin(UserEntity user, BigDecimal margin, int leverage) {
        if (!user.isPremium()) throw new RuntimeException("Premium status tələb olunur");
        if (leverage < 2 || leverage > 50) throw new InvalidTradeParameterException("Leverage xətası (2x-50x)");
        if (margin.compareTo(new BigDecimal("10")) < 0) throw new InsufficientFundsException("Minimum margin 10 USDT");
        if (user.getVirtualBalance().compareTo(margin) < 0) throw new InsufficientFundsException("Balans yetərsiz");
    }

    private void validateTPSL(PositionSide side, BigDecimal entry, BigDecimal tp, BigDecimal sl) {
        if (side == PositionSide.LONG) {
            if (tp != null && tp.compareTo(entry) <= 0) throw new InvalidTradeParameterException("TP girişdən yuxarı olmalıdır");
            if (sl != null && sl.compareTo(entry) >= 0) throw new InvalidTradeParameterException("SL girişdən aşağı olmalıdır");
        } else {
            if (tp != null && tp.compareTo(entry) >= 0) throw new InvalidTradeParameterException("TP girişdən aşağı olmalıdır");
            if (sl != null && sl.compareTo(entry) <= 0) throw new InvalidTradeParameterException("SL girişdən yuxarı olmalıdır");
        }
    }

    private BigDecimal calculateLiquidationPrice(BigDecimal entry, int leverage, PositionSide side) {
        BigDecimal factor = BigDecimal.ONE.divide(new BigDecimal(leverage), 8, RoundingMode.HALF_UP);
        BigDecimal maintenance = new BigDecimal("0.005");
        return (side == PositionSide.LONG)
                ? entry.multiply(BigDecimal.ONE.subtract(factor).add(maintenance)).setScale(4, RoundingMode.HALF_UP)
                : entry.multiply(BigDecimal.ONE.add(factor).subtract(maintenance)).setScale(4, RoundingMode.HALF_UP);
    }

    private TradeEntity findTradeById(UUID id) {
        return tradeRepository.findById(id).orElseThrow(() -> new RuntimeException("Trade tapılmadı"));
    }

    private void validateStatus(TradeEntity trade, TradeStatus expected, String msg) {
        if (trade.getStatus() != expected) throw new RuntimeException(msg);
    }

    private BigDecimal safeGet(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}