package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Expertise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpertiseRepository extends JpaRepository<Expertise, Long> {

    Optional<Expertise> findByNameIgnoreCase(String name);
}
