package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {
}