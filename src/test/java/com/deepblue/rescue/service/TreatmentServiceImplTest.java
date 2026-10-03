package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    @Test
    void shouldRegisterTreatmentSuccessfully() {
        // Arrange
        Animal animal = new Animal("AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        RescueCase rescueCase = new RescueCase("RES-001", LocalDate.of(2026, 8, 20), "Playa Blanca", RescueStatus.IN_REHABILITATION);
        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);

        LocalDateTime performedAt = LocalDateTime.of(2026, 8, 21, 9, 0);
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", performedAt, TreatmentType.WOUND_CARE, "Cleaning of left flipper"
        );

        TreatmentResponse response = new TreatmentResponse(
                1L, "AN-001", "SPEC-001", performedAt, TreatmentType.WOUND_CARE, "Cleaning of left flipper"
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        // Act
        TreatmentResponse result = service.register(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.animalCode()).isEqualTo("AN-001");
        assertThat(result.specialistCode()).isEqualTo("SPEC-001");
        verify(treatmentRepository).save(any(Treatment.class));
        verify(mapper).toResponse(any(Treatment.class));
    }

    @Test
    void shouldThrowWhenAnimalNotFound() {
        // Arrange
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-999", "SPEC-001", LocalDateTime.now(), TreatmentType.OBSERVATION, "General observation"
        );
        when(animalRepository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Animal not found: AN-999");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSpecialistNotFound() {
        // Arrange
        Animal animal = new Animal("AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-999", LocalDateTime.now(), TreatmentType.OBSERVATION, "General observation"
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Specialist not found: SPEC-999");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSpecialistInactive() {
        // Arrange
        Animal animal = new Animal("AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", false);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.now(), TreatmentType.OBSERVATION, "General observation"
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        // Act & Assert
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Specialist is not active: SPEC-001");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRescueCaseAlreadyReleased() {
        // Arrange
        Animal animal = new Animal("AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        RescueCase rescueCase = new RescueCase("RES-001", LocalDate.of(2026, 8, 20), "Playa Blanca", RescueStatus.RELEASED);
        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 25, 10, 0), TreatmentType.MEDICATION, "Vitamins"
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        // Act & Assert
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot register treatment for animal in status: RELEASED");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenTreatmentDateBeforeRescueDate() {
        // Arrange
        Animal animal = new Animal("AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        RescueCase rescueCase = new RescueCase("RES-001", LocalDate.of(2026, 8, 20), "Playa Blanca", RescueStatus.IN_REHABILITATION);
        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);

        // Date before rescue: 2026-08-15 is before 2026-08-20
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 15, 10, 0), TreatmentType.WOUND_CARE, "Cleaning"
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        // Act & Assert
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Treatment date cannot be before rescue date: 2026-08-20");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldFindByAnimalCode() {
        // Arrange
        Animal animal = new Animal("AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        LocalDateTime performedAt = LocalDateTime.of(2026, 8, 21, 9, 0);
        Treatment treatment = new Treatment(animal, specialist, performedAt, TreatmentType.WOUND_CARE, "Cleaning");

        TreatmentResponse response = new TreatmentResponse(
                1L, "AN-001", "SPEC-001", performedAt, TreatmentType.WOUND_CARE, "Cleaning"
        );

        when(treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-001")).thenReturn(List.of(treatment));
        when(mapper.toResponse(treatment)).thenReturn(response);

        // Act
        List<TreatmentResponse> results = service.findByAnimalCode("AN-001");

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(response);
        verify(treatmentRepository).findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-001");
        verify(mapper).toResponse(treatment);
    }
}
