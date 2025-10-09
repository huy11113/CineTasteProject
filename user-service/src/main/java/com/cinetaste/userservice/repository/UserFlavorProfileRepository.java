package com.cinetaste.userservice.repository;

import com.cinetaste.userservice.entity.UserFlavorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserFlavorProfileRepository extends JpaRepository<UserFlavorProfile, UUID> {
}