package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "specialists")
public class Specialist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_code", nullable = false, unique = true, length = 50)
    private String professionalCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToMany
    @JoinTable(
        name = "specialist_expertise",
        joinColumns = @JoinColumn(name = "specialist_id"),
        inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private List<Expertise> expertiseAreas = new ArrayList<>();

    @OneToMany(mappedBy = "specialist")
    private List<Treatment> treatments = new ArrayList<>();

    public Specialist() {
    }

    public Specialist(String professionalCode, String firstName, String lastName, String email, Boolean active) {
        this.professionalCode = professionalCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.active = active != null ? active : true;
    }

    public void addExpertise(Expertise expertise) {
        this.expertiseAreas.add(expertise);
        expertise.getSpecialists().add(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProfessionalCode() {
        return professionalCode;
    }

    public void setProfessionalCode(String professionalCode) {
        this.professionalCode = professionalCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<Expertise> getExpertiseAreas() {
        return expertiseAreas;
    }

    public void setExpertiseAreas(List<Expertise> expertiseAreas) {
        this.expertiseAreas = expertiseAreas;
    }

    public List<Treatment> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<Treatment> treatments) {
        this.treatments = treatments;
    }
}
