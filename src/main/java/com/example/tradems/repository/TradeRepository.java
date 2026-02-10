package com.example.tradems.repository;
import com.example.tradems.enums.TradeStatus;
import com.example.tradems.model.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<TradeEntity, UUID> {
    List<TradeEntity> findByUserIdAndStatus(Long userId, TradeStatus status);

    List<TradeEntity> findAllByStatus(TradeStatus tradeStatus);
}
