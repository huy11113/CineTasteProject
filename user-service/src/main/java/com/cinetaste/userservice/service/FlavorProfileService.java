package com.cinetaste.userservice.service;

import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.entity.UserFlavorProfile;
import com.cinetaste.userservice.repository.UserFlavorProfileRepository;
import com.cinetaste.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlavorProfileService {

    private final UserFlavorProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getProfileByUserId(UUID userId) {
        return profileRepository.findById(userId)
                .map(UserFlavorProfile::getPreferences)
                .orElseGet(Map::of); // Trả về map rỗng nếu chưa có profile
    }

    @Transactional
    public Map<String, Object> updateProfile(UUID userId, Map<String, Object> preferences) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserFlavorProfile profile = profileRepository.findById(userId)
                .orElse(new UserFlavorProfile());

        profile.setUser(user);
        profile.setPreferences(preferences);

        UserFlavorProfile savedProfile = profileRepository.save(profile);
        return savedProfile.getPreferences();
    }
}