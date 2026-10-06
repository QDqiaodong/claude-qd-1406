package com.health.check.dept;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import lombok.Data;

/** 科室：停诊与装修的科室不可排检查项；装修完成后按流转表只能恢复为开放。 */
@Data
@Entity
@Table(name = "dept")
public class Dept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 科室编号，全局唯一 */
    @Column(unique = true, length = 32)
    private String deptNo;

    /** 科室名，不可为空 */
    private String deptName;

    /** 位置 */
    private String location;

    /** 负责医师 */
    private String doctor;

    /** 状态：开放 / 停诊 / 装修 */
    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
