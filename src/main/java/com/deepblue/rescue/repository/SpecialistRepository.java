package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, Long> {

    Optional<Specialist> findByProfessionalCode(String professionalCode);

    Optional<Specialist> findByEmail(String email);

    @Query("""
        SELECT DISTINCT s
        FROM Specialist s
        JOIN s.expertiseAreas e
        WHERE LOWER(e.name) = LOWER(:expertiseName)
          AND s.active = true
        ORDER BY s.lastName ASC
    """)
    List<Specialist> findActiveByExpertise(@Param("expertiseName") String expertiseName);
}
