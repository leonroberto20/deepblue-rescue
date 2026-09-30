package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RescueCenterRepository extends JpaRepository<RescueCenter, Long> {

    Optional<RescueCenter> findByCode(String code);

    List<RescueCenter> findByCityIgnoreCase(String city);

    boolean existsByCode(String code);
}
