package com.health.check.dept;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeptRepository extends JpaRepository<Dept, Long> {

    Optional<Dept> findByDeptNo(String deptNo);

    List<Dept> findByStatus(String status);
}
