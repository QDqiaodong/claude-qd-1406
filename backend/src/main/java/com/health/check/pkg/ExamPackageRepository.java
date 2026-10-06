package com.health.check.pkg;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamPackageRepository extends JpaRepository<ExamPackage, Long> {

    List<ExamPackage> findByStatus(String status);
}
