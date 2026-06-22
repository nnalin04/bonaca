package com.bonaca.backend.members.repository;

import com.bonaca.backend.members.model.Invite;
import com.bonaca.backend.members.model.InviteStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    Optional<Invite> findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(String phoneNumber, InviteStatus status);

    List<Invite> findByInviterAccountId(UUID accountId);
}
