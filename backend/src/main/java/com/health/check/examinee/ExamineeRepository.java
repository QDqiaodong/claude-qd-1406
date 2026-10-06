package com.health.check.examinee;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamineeRepository extends JpaRepository<Examinee, Long> {

    List<Examinee> findByPackageId(Long packageId);

    List<Examinee> findByStatus(String status);
}
