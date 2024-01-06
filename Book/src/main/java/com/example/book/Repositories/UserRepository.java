package com.example.book.Repositories;
import com.example.book.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);


    Optional<User> findByEmail(String email);
}

