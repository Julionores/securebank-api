package com.jtmcloud.securebank.domain.repository;

import com.jtmcloud.securebank.domain.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByIban(String iban);

    List<Account> findByOwnerId(UUID ownerId);

    /**
     * Verrou pessimiste utilisé lors des virements pour sérialiser les accès concurrents
     * au même compte et empêcher tout dépassement de solde (double dépense).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);
}
