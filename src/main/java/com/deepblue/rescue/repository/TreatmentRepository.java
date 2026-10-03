package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    // Query Method cronológico por código de animal (para TreatmentService)
    List<Treatment> findByAnimalAnimalCodeOrderByPerformedAtAsc(String animalCode);

    // Paso 41: Query Method cronológico por animal
    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    // Paso 42: Consulta JPQL por intervalo de fechas
    @Query("""
        SELECT t
        FROM Treatment t
        WHERE t.performedAt BETWEEN :start AND :end
        ORDER BY t.performedAt ASC
    """)
    List<Treatment> findTreatmentsBetweenDates(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    // Paso 43: JPQL navegando Treatment -> Animal -> RescueCase -> RescueCenter
    @Query("""
        SELECT t
        FROM Treatment t
        JOIN t.animal a
        JOIN a.rescueCase c
        JOIN c.rescueCenter rc
        WHERE rc.code = :centerCode
        ORDER BY t.performedAt DESC
    """)
    List<Treatment> findByRescueCenterCode(@Param("centerCode") String centerCode);

    // Paso 44: JPQL con relación N:M (Treatment -> Specialist -> Expertise)
    @Query("""
        SELECT DISTINCT t
        FROM Treatment t
        JOIN t.specialist s
        JOIN s.expertiseAreas e
        WHERE LOWER(e.name) = LOWER(:expertiseName)
        ORDER BY t.performedAt DESC
    """)
    List<Treatment> findBySpecialistExpertise(@Param("expertiseName") String expertiseName);
}
