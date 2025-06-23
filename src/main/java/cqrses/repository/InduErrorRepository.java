package cqrses.repository;

import cqrses.entity.InduError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InduErrorRepository extends JpaRepository<InduError, String> {
    List<InduError> findByStatus(String status);
}