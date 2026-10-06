package com.health.check.exam;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/** 检查项记录：只能排到开放的科室；只有体检中的体检人可做检查；已完成与已弃检为终态。 */
@Data
@Entity
@Table(name = "exam")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 体检人 id（外键 → examinee.id） */
    private Long examineeId;

    /** 科室 id（外键 → dept.id），必须处于开放状态 */
    private Long deptId;

    /** 检查项名称：身高体重 / 血压 / 血常规 / 尿常规 / 心电图 / 胸片 / B超 / 肝功能 */
    private String itemName;

    /** 状态：待检 / 检查中 / 已完成 / 已弃检 */
    private String status;

    /** 结果描述，可空；标记已完成、异常或弃检时必填 */
    private String result;

    /** 是否异常 */
    private Boolean abnormal;

    /** 检查完成时间，可空；标记已完成时写入 */
    private LocalDateTime checkTime;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
