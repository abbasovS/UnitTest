package com.example.tradems;

import com.example.tradems.client.PriceClient;
import com.example.tradems.dto.request.OpenTradeRequest;
import com.example.tradems.enums.PositionSide;
import com.example.tradems.enums.TradeStatus;
import com.example.tradems.exception.InsufficientFundsException;
import com.example.tradems.model.TradeEntity;
import com.example.tradems.model.UserEntity;
import com.example.tradems.repository.TradeRepository;
import com.example.tradems.repository.UserRepository;
import com.example.tradems.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private PriceClient priceClient;

    @InjectMocks
    private TradeService tradeService;

    private UserEntity mockUser;
    private OpenTradeRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setVirtualBalance(new BigDecimal("1000"));
        mockUser.setPremium(true);

        mockRequest = new OpenTradeRequest(
                1L, "BTCUSDT", PositionSide.LONG,
                new BigDecimal("100"), 10,
                null, null, null
        );
    }


    @Test
    void openTrade_ShouldThrowException_WhenBalanceInsufficient() {

        mockUser.setVirtualBalance(new BigDecimal("50"));
        when(userRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(mockUser));


        assertThrows(InsufficientFundsException.class, () -> tradeService.openTrade(mockRequest));
    }

    @Test
    void openTrade_ShouldOpenMarketTrade_WhenRequestIsValid() {

        when(userRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(mockUser));
        when(priceClient.getRealtimePrice("BTCUSDT")).thenReturn("Price: 50000.0 USD");
        when(tradeRepository.save(any(TradeEntity.class))).thenAnswer(i -> i.getArguments()[0]);


        TradeEntity result = tradeService.openTrade(mockRequest);


        assertNotNull(result);
        assertEquals(TradeStatus.OPEN, result.getStatus());
        assertEquals(new BigDecimal("50000.0"), result.getEntryPrice());

        assertEquals(new BigDecimal("900"), mockUser.getVirtualBalance());

        verify(tradeRepository).save(any(TradeEntity.class));
    }

    @Test
    void closeTradeManually_ShouldCalculateCorrectPnL_ForLongPosition() {

        TradeEntity trade = new TradeEntity();
        trade.setSide(PositionSide.LONG);
        trade.setEntryPrice(new BigDecimal("100"));
        trade.setMargin(new BigDecimal("100"));
        trade.setLeverage(10);
        trade.setStatus(TradeStatus.OPEN);
        trade.setUserId(1L);

        when(tradeRepository.findById(any())).thenReturn(Optional.of(trade));
        when(userRepository.findByIdWithLock(any())).thenReturn(Optional.of(mockUser));
        when(priceClient.getRealtimePrice(any())).thenReturn("Price: 110.0 USD"); // %10 artım

        tradeService.closeTradeManually(UUID.randomUUID());

        assertEquals(new BigDecimal("1200.00000000"), mockUser.getVirtualBalance());
        assertEquals(new BigDecimal("100.00000000"), trade.getPnl());
    }
}