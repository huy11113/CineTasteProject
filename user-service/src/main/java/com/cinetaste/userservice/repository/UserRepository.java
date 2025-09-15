package com.cinetaste.userservice.repository;

import com.cinetaste.userservice.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Spring Data JPA sẽ tự động tạo ra câu lệnh query
     * "SELECT * FROM users WHERE username = ?" cho chúng ta.
     * @param username Tên người dùng cần tìm.
     * @return một Optional chứa User nếu tìm thấy.
     */
    Optional<User> findByUsername(String username);

    /**
     * Tương tự, tự động tạo query "SELECT * FROM users WHERE email = ?".
     * @param email Email cần tìm.
     * @return một Optional chứa User nếu tìm thấy.
     */
    Optional<User> findByEmail(String email);
}