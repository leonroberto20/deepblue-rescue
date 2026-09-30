package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expertise")
public class Expertise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToMany(mappedBy = "expertiseAreas")
    private List<Specialist> specialists = new ArrayList<>();

    public Expertise() {
    }

    public Expertise(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Specialist> getSpecialists() {
        return specialists;
    }

    public void setSpecialists(List<Specialist> specialists) {
        this.specialists = specialists;
    }
}
