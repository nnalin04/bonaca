package com.bonaca.backend.members.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "gender")
    private String gender;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Member() {
    }

    public Member(
            UUID accountId,
            UUID userId,
            MemberRole role,
            String name,
            String gender,
            LocalDate dob,
            Integer heightCm,
            Integer weightKg) {
        this.accountId = accountId;
        this.userId = userId;
        this.role = role;
        this.name = name;
        this.gender = gender;
        this.dob = dob;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MemberRole getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getDob() {
        return dob;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public Integer getWeightKg() {
        return weightKg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
