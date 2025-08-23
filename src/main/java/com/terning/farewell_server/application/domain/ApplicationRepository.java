package com.terning.farewell_server.application.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByEmail(String email);

    Optional<Application> findByEmail(String email);
}
