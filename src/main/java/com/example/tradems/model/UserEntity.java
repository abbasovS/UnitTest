package com.example.tradems.model;

import com.example.tradems.enums.UserRank;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEntity {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
Long id;
String username;
BigDecimal frozenBalance=BigDecimal.ZERO;
BigDecimal virtualBalance=BigDecimal.ZERO;
@Enumerated(EnumType.STRING)
UserRank userRank;
boolean isPremium=false;
LocalDateTime subscriptionEndDate;
}
