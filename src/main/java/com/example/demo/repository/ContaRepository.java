package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ContaBancaria;

@Repository
public interface ContaRepository extends JpaRepository<ContaBancaria, Long> {
}
