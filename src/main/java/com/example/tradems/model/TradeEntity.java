package com.example.tradems.model;

import com.example.tradems.enums.PositionSide;
import com.example.tradems.enums.TradeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "traders")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
     UUID id;

     Long userId;

     String symbol;

    @Enumerated(EnumType.STRING)
    PositionSide side;

     BigDecimal entryPrice;

     BigDecimal margin;

     Integer leverage;

     BigDecimal liquidationPrice;

     BigDecimal takeProfit;

     BigDecimal stopLoss;

    @Enumerated(EnumType.STRING)
    TradeStatus status;


    @JsonSerialize(using = ToStringSerializer.class)
     BigDecimal pnl;

     LocalDateTime openTime = LocalDateTime.now();

     BigDecimal closePrice;

     LocalDateTime closeTime;

}
