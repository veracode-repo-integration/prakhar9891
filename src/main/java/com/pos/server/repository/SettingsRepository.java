package com.pos.server.repository;

import com.pos.server.model.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, String> {
    // findFirst is resilient to duplicate rows — prevents NonUniqueResultException
    java.util.Optional<Settings> findFirstByRestaurantId(String restaurantId);

    // Keep the list version for cleanup utilities
    java.util.List<Settings> findAllByRestaurantId(String restaurantId);
}
