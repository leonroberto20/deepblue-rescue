package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);

    List<Animal> findByRescueCaseStatus(RescueStatus status);

    List<Animal> findByRescueCaseRescueCenterCode(String code);

    List<Animal> findByCommonNameContainingIgnoreCase(String text);

    // Reto sin guía (Pasos 75-77): Animales por estado y tratamientos con especialista de cierta especialidad
    @Query("""
        SELECT DISTINCT a
        FROM Animal a
        JOIN a.rescueCase rc
        JOIN a.treatments t
        JOIN t.specialist s
        JOIN s.expertiseAreas e
        WHERE rc.status = :status
          AND LOWER(e.name) = LOWER(:expertiseName)
    """)
    List<Animal> findAnimalsInStatusWithSpecialistExpertise(
            @Param("status") RescueStatus status,
            @Param("expertiseName") String expertiseName
    );
}
