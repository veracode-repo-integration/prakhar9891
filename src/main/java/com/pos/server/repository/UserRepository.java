package com.pos.server.repository;

import com.pos.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndRestaurantId(String username, String restaurantId);

    List<User> findAllByRestaurantId(String restaurantId);

    Optional<User> findByEmail(String email);

    Optional<User> findByContact(String contact);

    Optional<User> findFirstByContact(String contact);

    Optional<User> findFirstByContactAndRestaurantId(String contact, String restaurantId);
}
