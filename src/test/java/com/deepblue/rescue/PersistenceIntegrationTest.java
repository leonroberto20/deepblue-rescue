package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ========================================================================
    // 52. Paso 47 — Test de Flyway
    // ========================================================================
    @Test
    @DisplayName("Paso 47: Validar que Flyway ejecutó al menos las migraciones V1 y V2")
    void testFlywayMigrationsApplied() {
        List<String> versions = jdbcTemplate.query(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
                (rs, rowNum) -> rs.getString("version")
        );

        assertThat(versions).contains("1", "2");
    }

    // ========================================================================
    // 53. Paso 48 — Test de métodos heredados
    // ========================================================================
    @Test
    @DisplayName("Paso 48: Probar métodos CRUD heredados (save, findById, existsById, count)")
    void testInheritedCrudMethods() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();
        assertThat(rescueCenterRepository.findById(saved.getId())).isPresent();
        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();
        assertThat(rescueCenterRepository.count()).isGreaterThanOrEqualTo(1);
    }

    // ========================================================================
    // 54. Paso 49 — Test relación 1:N (RescueCenter -> RescueCase)
    // ========================================================================
    @Test
    @DisplayName("Paso 49: Persistir relación 1:N entre RescueCenter y RescueCase")
    void testOneToManyRescueCenterCases() {
        RescueCenter center = new RescueCenter("DB-PAC", "DeepBlue Pacific Center", "Tumaco");
        RescueCase case1 = new RescueCase("CASE-PAC-001", LocalDate.now(), "Playa El Morro", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("CASE-PAC-002", LocalDate.now(), "Boca Grande", RescueStatus.UNDER_EVALUATION);

        center.addCase(case1);
        center.addCase(case2);

        RescueCenter savedCenter = rescueCenterRepository.save(center);
        rescueCenterRepository.flush();

        assertThat(savedCenter.getCases()).hasSize(2);
        assertThat(case1.getRescueCenter().getCode()).isEqualTo("DB-PAC");
        assertThat(case2.getRescueCenter().getCode()).isEqualTo("DB-PAC");
    }

    // ========================================================================
    // 55. Paso 50 — Test RescueCase 1:1 Animal
    // ========================================================================
    @Test
    @DisplayName("Paso 50: Persistir relación 1:1 entre RescueCase y Animal")
    void testOneToOneRescueCaseAnimal() {
        RescueCenter center = new RescueCenter("DB-TEST", "Center Test", "Cartagena");
        RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.of(2026, 8, 1), "Isla Baru", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        RescueCenter savedCenter = rescueCenterRepository.saveAndFlush(center);
        RescueCase savedCase = savedCenter.getCases().getFirst();

        assertThat(savedCase.getAnimal()).isNotNull();
        assertThat(savedCase.getAnimal().getId()).isNotNull();
        assertThat(savedCase.getAnimal().getRescueCase()).isEqualTo(savedCase);
        assertThat(savedCase.getAnimal().getCommonName()).isEqualTo("Green Sea Turtle");
    }

    // ========================================================================
    // 56. Paso 51 — Test Animal 1:1 MedicalRecord
    // ========================================================================
    @Test
    @DisplayName("Paso 51: Persistir Animal 1:1 MedicalRecord mediante cascade")
    void testOneToOneAnimalMedicalRecordCascade() {
        RescueCenter center = new RescueCenter("DB-MED", "Center Med", "Santa Marta");
        RescueCase rescueCase = new RescueCase("RES-2026-002", LocalDate.now(), "Rodadero", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-002", "Hawksbill Turtle", "Eretmochelys imbricata", AnimalSex.MALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"),
                "STABLE",
                "Left front flipper injury",
                "Ingested hooks suspected"
        );
        animal.assignMedicalRecord(record);

        RescueCenter savedCenter = rescueCenterRepository.saveAndFlush(center);
        Animal savedAnimal = savedCenter.getCases().getFirst().getAnimal();

        assertThat(savedAnimal.getId()).isNotNull();
        assertThat(savedAnimal.getMedicalRecord().getId()).isNotNull();
        assertThat(savedAnimal.getMedicalRecord().getInitialWeight()).isEqualByComparingTo("28.40");
    }

    // ========================================================================
    // 57. Paso 52 — Test N:M (Specialist <-> Expertise)
    // ========================================================================
    @Test
    @DisplayName("Paso 52: Persistir y verificar relación N:M entre Specialist y Expertise")
    void testManyToManySpecialistExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseGet(() -> expertiseRepository.save(new Expertise("Trauma")));
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseGet(() -> expertiseRepository.save(new Expertise("Rehabilitation")));

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena.vargas@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        specialistRepository.saveAndFlush(elena);

        Specialist found = specialistRepository.findById(elena.getId()).orElseThrow();
        assertThat(found.getExpertiseAreas()).hasSize(2);
    }

    // ========================================================================
    // 58. Paso 53 — Test Query Method simple (por status)
    // ========================================================================
    @Test
    @DisplayName("Paso 53: Test Query Method simple findByStatus")
    void testFindByStatus() {
        RescueCenter center = rescueCenterRepository.save(new RescueCenter("DB-STAT", "Center Status", "San Andres"));

        RescueCase c1 = new RescueCase("RES-STAT-001", LocalDate.now(), "Playa 1", RescueStatus.IN_REHABILITATION);
        RescueCase c2 = new RescueCase("RES-STAT-002", LocalDate.now(), "Playa 2", RescueStatus.READY_FOR_RELEASE);
        RescueCase c3 = new RescueCase("RES-STAT-003", LocalDate.now(), "Playa 3", RescueStatus.IN_REHABILITATION);

        center.addCase(c1);
        center.addCase(c2);
        center.addCase(c3);
        rescueCenterRepository.saveAndFlush(center);

        List<RescueCase> rehabCases = rescueCaseRepository.findByStatus(RescueStatus.IN_REHABILITATION);
        assertThat(rehabCases).extracting(RescueCase::getCaseCode).contains("RES-STAT-001", "RES-STAT-003");
    }

    // ========================================================================
    // 59. Paso 54 — Test Query Method navegando relaciones
    // ========================================================================
    @Test
    @DisplayName("Paso 54: Buscar animales navegando hasta el código del centro")
    void testFindAnimalsByRescueCenterCode() {
        RescueCenter cCar = new RescueCenter("DB-CAR", "Caribbean Center", "Santa Marta");
        RescueCase caseCar = new RescueCase("RC-CAR-01", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        cCar.addCase(caseCar);
        Animal aCar = new Animal("AN-CAR-01", "Delfin", "Tursiops truncatus", AnimalSex.MALE);
        caseCar.assignAnimal(aCar);
        rescueCenterRepository.save(cCar);

        RescueCenter cPac = new RescueCenter("DB-PAC", "Pacific Center", "Tumaco");
        RescueCase casePac = new RescueCase("RC-PAC-01", LocalDate.now(), "Gorgona", RescueStatus.ADMITTED);
        cPac.addCase(casePac);
        Animal aPac = new Animal("AN-PAC-01", "Ballena", "Megaptera novaeangliae", AnimalSex.FEMALE);
        casePac.assignAnimal(aPac);
        rescueCenterRepository.save(cPac);

        rescueCenterRepository.flush();

        List<Animal> carAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(carAnimals).hasSize(1);
        assertThat(carAnimals.getFirst().getAnimalCode()).isEqualTo("AN-CAR-01");
    }

    // ========================================================================
    // 60. Paso 55 — Test JPQL de especialistas
    // ========================================================================
    @Test
    @DisplayName("Paso 55: Test JPQL findActiveByExpertise")
    void testFindActiveByExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise birds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SP-E", "Elena", "Vargas", "elena@db.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        Specialist mateo = new Specialist("SP-M", "Mateo", "Lopez", "mateo@db.org", true);
        mateo.addExpertise(rehab);

        Specialist sofia = new Specialist("SP-S", "Sofia", "Alvarez", "sofia@db.org", true);
        sofia.addExpertise(birds);
        sofia.addExpertise(trauma);

        specialistRepository.saveAllAndFlush(List.of(elena, mateo, sofia));

        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(traumaSpecialists).extracting(Specialist::getFirstName).containsExactlyInAnyOrder("Elena", "Sofia");
    }

    // ========================================================================
    // 61 y 62. Pasos 56 y 57 — Test Query Method de tratamientos ordenados
    // ========================================================================
    @Test
    @DisplayName("Pasos 56 y 57: Test findByAnimalIdOrderByPerformedAtAsc")
    void testFindTreatmentsOrderedChronologically() {
        RescueCenter center = new RescueCenter("DB-TR", "Treatment Center", "Santa Marta");
        RescueCase rCase = new RescueCase("CASE-TR", LocalDate.now(), "Playa Brava", RescueStatus.IN_REHABILITATION);
        center.addCase(rCase);
        Animal animal = new Animal("AN-TR-01", "Pelicano", "Pelecanus occidentalis", AnimalSex.MALE);
        rCase.assignAnimal(animal);
        rescueCenterRepository.saveAndFlush(center);

        Specialist spec = specialistRepository.save(new Specialist("SPEC-TR", "Carlos", "Perez", "carlos@db.org", true));

        LocalDateTime t1Time = LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime t2Time = LocalDateTime.of(2026, 8, 2, 10, 0);
        LocalDateTime t3Time = LocalDateTime.of(2026, 8, 3, 11, 0);

        treatmentRepository.save(new Treatment(animal, spec, t2Time, TreatmentType.HYDRATION, "T2"));
        treatmentRepository.save(new Treatment(animal, spec, t1Time, TreatmentType.WOUND_CARE, "T1"));
        treatmentRepository.save(new Treatment(animal, spec, t3Time, TreatmentType.OBSERVATION, "T3"));
        treatmentRepository.flush();

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(treatments).hasSize(3);
        assertThat(treatments.get(0).getType()).isEqualTo(TreatmentType.WOUND_CARE);
        assertThat(treatments.get(1).getType()).isEqualTo(TreatmentType.HYDRATION);
        assertThat(treatments.get(2).getType()).isEqualTo(TreatmentType.OBSERVATION);
    }

    // ========================================================================
    // 63. Paso 58 — Test JPQL por intervalo de fechas
    // ========================================================================
    @Test
    @DisplayName("Paso 58: Test findTreatmentsBetweenDates")
    void testFindTreatmentsBetweenDates() {
        RescueCenter center = new RescueCenter("DB-INT", "Interval Center", "Santa Marta");
        RescueCase rCase = new RescueCase("CASE-INT", LocalDate.now(), "Playa Cristal", RescueStatus.IN_REHABILITATION);
        center.addCase(rCase);
        Animal animal = new Animal("AN-INT-01", "Tortuga Carey", "Eretmochelys imbricata", AnimalSex.FEMALE);
        rCase.assignAnimal(animal);
        rescueCenterRepository.saveAndFlush(center);

        Specialist spec = specialistRepository.save(new Specialist("SPEC-INT", "Andres", "Rios", "andres@db.org", true));

        treatmentRepository.save(new Treatment(animal, spec, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Antes"));
        treatmentRepository.save(new Treatment(animal, spec, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.MEDICATION, "En rango"));
        treatmentRepository.save(new Treatment(animal, spec, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.NUTRITION, "Despues"));
        treatmentRepository.flush();

        List<Treatment> inRange = treatmentRepository.findTreatmentsBetweenDates(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59)
        );

        assertThat(inRange).hasSize(1);
        assertThat(inRange.getFirst().getType()).isEqualTo(TreatmentType.MEDICATION);
    }

    // ========================================================================
    // 64. Paso 59 — Probar UNIQUE
    // ========================================================================
    @Test
    @DisplayName("Paso 59: Probar constraint UNIQUE con DataIntegrityViolationException")
    void testUniqueConstraintViolation() {
        RescueCenter center = new RescueCenter("DB-UQ", "Unique Center", "Santa Marta");
        RescueCase case1 = new RescueCase("CASE-UQ-1", LocalDate.now(), "Playa", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("CASE-UQ-2", LocalDate.now(), "Playa", RescueStatus.ADMITTED);
        center.addCase(case1);
        center.addCase(case2);

        Animal a1 = new Animal("AN-100", "Lobo Marino", "Otaria flavescens", AnimalSex.MALE);
        Animal a2 = new Animal("AN-100", "Lobo Marino Duplicado", "Otaria flavescens", AnimalSex.MALE);
        case1.assignAnimal(a1);
        case2.assignAnimal(a2);

        assertThatThrownBy(() -> rescueCenterRepository.saveAndFlush(center))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ========================================================================
    // 72 y 73. Pasos 65 y 66 — Reto Integrador (Persistir escenario y resolver consultas)
    // ========================================================================
    @Test
    @DisplayName("Pasos 65 y 66: Reto Integrador completo (Bahía Concha y 8 consultas)")
    void testIntegratorScenarioAndQueries() {
        // Centro
        RescueCenter dbCar = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");

        // Caso
        RescueCase rescueCase = new RescueCase("RES-2026-100", LocalDate.of(2026, 8, 18), "Bahía Concha", RescueStatus.IN_REHABILITATION);
        dbCar.addCase(rescueCase);

        // Animal
        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        // Expediente
        MedicalRecord record = new MedicalRecord(
                new BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion"
        );
        animal.assignMedicalRecord(record);

        rescueCenterRepository.saveAndFlush(dbCar);

        // Especialista
        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);
        specialistRepository.saveAndFlush(elena);

        // Tratamientos
        Treatment t1 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 18, 14, 0), TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        Treatment t2 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 19, 9, 30), TreatmentType.HYDRATION, "Subcutaneous fluid therapy");
        treatmentRepository.save(t1);
        treatmentRepository.save(t2);
        treatmentRepository.flush();

        // Consulta 1: ¿Existe el caso RES-2026-100?
        Optional<RescueCase> foundCase = rescueCaseRepository.findByCaseCode("RES-2026-100");
        assertThat(foundCase).isPresent();

        // Consulta 2: Obtener todos los casos IN_REHABILITATION
        List<RescueCase> rehabCases = rescueCaseRepository.findByStatus(RescueStatus.IN_REHABILITATION);
        assertThat(rehabCases).extracting(RescueCase::getCaseCode).contains("RES-2026-100");

        // Consulta 3: Obtener animales pertenecientes a DB-CAR
        List<Animal> carAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(carAnimals).extracting(Animal::getAnimalCode).contains("AN-2026-100");

        // Consulta 4: Buscar animales cuyo nombre común contenga "turtle" ignorando mayúsculas
        List<Animal> turtles = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
        assertThat(turtles).extracting(Animal::getAnimalCode).contains("AN-2026-100");

        // Consulta 5: Obtener especialistas con experiencia "Trauma"
        List<Specialist> traumaSpecs = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(traumaSpecs).extracting(Specialist::getFirstName).contains("Elena");

        // Consulta 6: Obtener todos los tratamientos de AN-2026-100 ordenados cronológicamente
        List<Treatment> animalTreatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(animalTreatments).hasSize(2);
        assertThat(animalTreatments.get(0).getType()).isEqualTo(TreatmentType.WOUND_CARE);
        assertThat(animalTreatments.get(1).getType()).isEqualTo(TreatmentType.HYDRATION);

        // Consulta 7: Tratamientos realizados por especialistas con experiencia Rehabilitation
        List<Treatment> rehabTreatments = treatmentRepository.findBySpecialistExpertise("Rehabilitation");
        assertThat(rehabTreatments).hasSize(2);

        // Consulta 8: Tratamientos realizados entre dos fechas
        List<Treatment> dateTreatments = treatmentRepository.findTreatmentsBetweenDates(
                LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 18, 23, 59)
        );
        assertThat(dateTreatments).hasSize(1);
    }

    // ========================================================================
    // 75-77. Reto sin guía — Animales en rehabilitación con tratamiento por especialista en Trauma
    // ========================================================================
    @Test
    @DisplayName("Pasos 75-77: Reto sin guía - Animales en rehabilitación tratados por especialista en Trauma")
    void testUnguidedChallenge() {
        RescueCenter center = new RescueCenter("DB-CHALL", "Challenge Center", "Santa Marta");

        RescueCase c1 = new RescueCase("RC-CH-01", LocalDate.now(), "Playa A", RescueStatus.IN_REHABILITATION);
        RescueCase c2 = new RescueCase("RC-CH-02", LocalDate.now(), "Playa B", RescueStatus.RELEASED);
        center.addCase(c1);
        center.addCase(c2);

        Animal a1 = new Animal("AN-CH-01", "Tortuga Golfina", "Lepidochelys olivacea", AnimalSex.FEMALE);
        Animal a2 = new Animal("AN-CH-02", "Delfin Manchado", "Stenella attenuata", AnimalSex.MALE);
        c1.assignAnimal(a1);
        c2.assignAnimal(a2);

        rescueCenterRepository.saveAndFlush(center);

        Specialist traumaDoc = new Specialist("SP-CH-1", "Lucia", "Gomez", "lucia@db.org", true);
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        traumaDoc.addExpertise(trauma);
        specialistRepository.saveAndFlush(traumaDoc);

        treatmentRepository.save(new Treatment(a1, traumaDoc, LocalDateTime.now(), TreatmentType.WOUND_CARE, "Tratamiento Trauma"));
        treatmentRepository.save(new Treatment(a2, traumaDoc, LocalDateTime.now(), TreatmentType.WOUND_CARE, "Tratamiento en animal liberado"));
        treatmentRepository.flush();

        List<Animal> results = animalRepository.findAnimalsInStatusWithSpecialistExpertise(RescueStatus.IN_REHABILITATION, "Trauma");

        assertThat(results).extracting(Animal::getAnimalCode).containsExactly("AN-CH-01");
    }
}
