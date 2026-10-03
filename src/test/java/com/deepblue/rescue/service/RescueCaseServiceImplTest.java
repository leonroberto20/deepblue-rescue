package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    @Test
    void shouldFindRescueCaseByCode() {
        // Arrange
        RescueCase rescueCase = new RescueCase("RES-001", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED);
        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED, "CEN-01", "AN-001"
        );

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        // Act
        RescueCaseResponse result = service.findByCode("RES-001");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(response);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowWhenNotFound() {
        // Arrange
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rescue case not found: RES-999");

        verify(repository).findByCaseCode("RES-999");
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindByStatus() {
        // Arrange
        RescueCase rescueCase = new RescueCase("RES-001", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED);
        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED, "CEN-01", "AN-001"
        );

        when(repository.findByStatusOrderByRescueDateAsc(RescueStatus.ADMITTED)).thenReturn(List.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        // Act
        List<RescueCaseResponse> results = service.findByStatus(RescueStatus.ADMITTED);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(response);
        verify(repository).findByStatusOrderByRescueDateAsc(RescueStatus.ADMITTED);
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldChangeStatusSuccessfullyOnValidTransition() {
        // Arrange
        RescueCase rescueCase = new RescueCase("RES-001", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED);
        ChangeRescueStatusRequest request = new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION);
        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.now(), "Playa Blanca", RescueStatus.UNDER_EVALUATION, "CEN-01", "AN-001"
        );

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(repository.save(any(RescueCase.class))).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        // Act
        RescueCaseResponse result = service.changeStatus("RES-001", request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowBusinessRuleExceptionOnInvalidTransition() {
        // Arrange
        RescueCase rescueCase = new RescueCase("RES-001", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED);
        ChangeRescueStatusRequest request = new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));

        // Act & Assert
        assertThatThrownBy(() -> service.changeStatus("RES-001", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid status transition from ADMITTED to READY_FOR_RELEASE");

        verify(repository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }
}
