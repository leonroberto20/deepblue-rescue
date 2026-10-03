package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    Optional<RescueCase> findByCaseCode(String caseCode);

    List<RescueCase> findByStatus(RescueStatus status);

    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    List<RescueCase> findByRescueCenterId(Long rescueCenterId);

    List<RescueCase> findByRescueDateBetween(LocalDate start, LocalDate end);

    List<RescueCase> findByRescueCenterCodeAndStatus(String centerCode, RescueStatus status);

    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);
}
