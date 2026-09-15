package br.com.espacosinapse.services;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ClinicServiceRepository
    extends JpaRepository<ClinicService, UUID>, JpaSpecificationExecutor<ClinicService> {
}
