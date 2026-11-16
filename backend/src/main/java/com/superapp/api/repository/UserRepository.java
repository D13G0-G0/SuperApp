package com.superapp.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.superapp.api.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}