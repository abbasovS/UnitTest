package com.example.tradems.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Entity
@Table(name = "cotest")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

public class CotestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id=1L;
    boolean active=false;
    int currentParticipants = 0;
    final int targetParticipants = 100;
}
