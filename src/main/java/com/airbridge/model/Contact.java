package com.airbridge.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contacts", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "contact_user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_user_id", nullable = false)
    private User contactUser;

    @Column(length = 100)
    private String nickname;

    @Builder.Default
    private boolean blocked = false;

    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
