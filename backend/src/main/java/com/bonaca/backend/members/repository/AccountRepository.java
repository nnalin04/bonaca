package com.bonaca.backend.members.repository;

import com.bonaca.backend.members.model.Account;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
