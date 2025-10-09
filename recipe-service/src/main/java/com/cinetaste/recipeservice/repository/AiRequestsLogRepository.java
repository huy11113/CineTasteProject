package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.AiRequestsLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRequestsLogRepository extends JpaRepository<AiRequestsLog, Long> {
}