package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);

    List<Animal> findByRescueCaseStatus(RescueStatus status);

    List<Animal> findByRescueCaseRescueCenterCode(String code);

    List<Animal> findByCommonNameContainingIgnoreCase(String text);
}
