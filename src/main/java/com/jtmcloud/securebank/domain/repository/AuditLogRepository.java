package com.jtmcloud.securebank.domain.repository;

import com.jtmcloud.securebank.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
