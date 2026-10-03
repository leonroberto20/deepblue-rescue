package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(
            AnimalRepository animalRepository,
            SpecialistRepository specialistRepository,
            TreatmentRepository treatmentRepository,
            TreatmentMapper mapper) {
        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
        return treatmentRepository
                .findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {
        // Regla 1: Validar existencia de Animal
        Animal animal = animalRepository.findByAnimalCode(request.animalCode())
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + request.animalCode()));

        // Regla 2: Validar existencia de Specialist
        Specialist specialist = specialistRepository.findByProfessionalCode(request.specialistCode())
                .orElseThrow(() -> new ResourceNotFoundException("Specialist not found: " + request.specialistCode()));

        // Regla 3: Validar que el especialista esté activo
        if (Boolean.FALSE.equals(specialist.getActive())) {
            throw new BusinessRuleException("Specialist is not active: " + request.specialistCode());
        }

        // Regla 4: Validar estado del caso de rescate
        RescueCase rescueCase = animal.getRescueCase();
        if (rescueCase != null) {
            RescueStatus status = rescueCase.getStatus();
            if (status == RescueStatus.RELEASED || status == RescueStatus.CLOSED) {
                throw new BusinessRuleException("Cannot register treatment for animal in status: " + status);
            }

            // Regla 5: Validar que performedAt no sea anterior a rescueDate
            if (request.performedAt() != null && request.performedAt().toLocalDate().isBefore(rescueCase.getRescueDate())) {
                throw new BusinessRuleException("Treatment date cannot be before rescue date: " + rescueCase.getRescueDate());
            }
        }

        Treatment treatment = new Treatment(
                animal,
                specialist,
                request.performedAt(),
                request.type(),
                request.description()
        );

        Treatment savedTreatment = treatmentRepository.save(treatment);
        return mapper.toResponse(savedTreatment);
    }
}
