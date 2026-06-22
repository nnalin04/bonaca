package com.bonaca.backend.members.repository;

import com.bonaca.backend.members.model.Member;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByUserId(UUID userId);

    List<Member> findByAccountId(UUID accountId);

    List<Member> findByIdIn(List<UUID> ids);
}
