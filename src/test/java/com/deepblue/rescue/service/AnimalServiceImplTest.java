package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
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
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalMapper animalMapper;

    @InjectMocks
    private AnimalServiceImpl animalService;

    @Test
    void shouldFindAnimalByCode() {
        // Arrange
        Animal animal = new Animal("AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        AnimalResponse response = new AnimalResponse(
                1L, "AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE, "RES-001", RescueStatus.ADMITTED
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(animalMapper.toResponse(animal)).thenReturn(response);

        // Act
        AnimalResponse result = animalService.findByCode("AN-001");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.animalCode()).isEqualTo("AN-001");
        verify(animalRepository).findByAnimalCode("AN-001");
        verify(animalMapper).toResponse(animal);
    }

    @Test
    void shouldThrowWhenAnimalNotFound() {
        // Arrange
        when(animalRepository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> animalService.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Animal not found: AN-999");

        verify(animalRepository).findByAnimalCode("AN-999");
        verify(animalMapper, never()).toResponse(any());
    }

    @Test
    void shouldFindAnimalsInRehabilitation() {
        // Arrange
        Animal animal = new Animal("AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        AnimalResponse response = new AnimalResponse(
                1L, "AN-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE, "RES-001", RescueStatus.IN_REHABILITATION
        );

        when(animalRepository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION)).thenReturn(List.of(animal));
        when(animalMapper.toResponse(animal)).thenReturn(response);

        // Act
        List<AnimalResponse> results = animalService.findAnimalsInRehabilitation();

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(response);
        verify(animalRepository).findByRescueCaseStatus(RescueStatus.IN_REHABILITATION);
        verify(animalMapper).toResponse(animal);
    }

    @Test
    void shouldReturnTrueWhenCanReceiveTreatment() {
        // Arrange - UNDER_EVALUATION
        Animal animal1 = new Animal("AN-001", "Delfín", "Delphinus delphis", AnimalSex.MALE);
        RescueCase case1 = new RescueCase("RES-001", LocalDate.now(), "Bahía", RescueStatus.UNDER_EVALUATION);
        case1.assignAnimal(animal1);

        // Arrange - IN_REHABILITATION
        Animal animal2 = new Animal("AN-002", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        RescueCase case2 = new RescueCase("RES-002", LocalDate.now(), "Playa Blanca", RescueStatus.IN_REHABILITATION);
        case2.assignAnimal(animal2);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal1));
        when(animalRepository.findByAnimalCode("AN-002")).thenReturn(Optional.of(animal2));

        // Act & Assert
        assertThat(animalService.canReceiveTreatment("AN-001")).isTrue();
        assertThat(animalService.canReceiveTreatment("AN-002")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCannotReceiveTreatment() {
        // Arrange - ADMITTED
        Animal animal1 = new Animal("AN-001", "Delfín", "Delphinus delphis", AnimalSex.MALE);
        RescueCase case1 = new RescueCase("RES-001", LocalDate.now(), "Bahía", RescueStatus.ADMITTED);
        case1.assignAnimal(animal1);

        // Arrange - RELEASED
        Animal animal2 = new Animal("AN-002", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        RescueCase case2 = new RescueCase("RES-002", LocalDate.now(), "Playa Blanca", RescueStatus.RELEASED);
        case2.assignAnimal(animal2);

        // Arrange - Sin RescueCase asociado
        Animal animal3 = new Animal("AN-003", "Pingüino", "Spheniscus humboldti", AnimalSex.MALE);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal1));
        when(animalRepository.findByAnimalCode("AN-002")).thenReturn(Optional.of(animal2));
        when(animalRepository.findByAnimalCode("AN-003")).thenReturn(Optional.of(animal3));

        // Act & Assert
        assertThat(animalService.canReceiveTreatment("AN-001")).isFalse();
        assertThat(animalService.canReceiveTreatment("AN-002")).isFalse();
        assertThat(animalService.canReceiveTreatment("AN-003")).isFalse();
    }
}
