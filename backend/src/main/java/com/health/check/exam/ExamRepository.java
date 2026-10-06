package com.health.check.exam;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByExamineeId(Long examineeId);

    List<Exam> findByDeptId(Long deptId);

    List<Exam> findByExamineeIdAndItemName(Long examineeId, String itemName);
}
