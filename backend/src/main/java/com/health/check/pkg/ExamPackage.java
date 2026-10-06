package com.health.check.pkg;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/** 体检套餐：停售与下架的套餐不可被预约；价格与项目数必须大于 0；下架为终态不可再改。 */
@Data
@Entity
@Table(name = "`package`")
public class ExamPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 套餐名 */
    private String packageName;

    /** 价格，必须大于 0 */
    private BigDecimal price;

    /** 包含项目数，必须大于 0 */
    private Integer itemCount;

    /** 适用人群：入职体检 / 年度体检 / 老年体检 / 女性专项 / 儿童体检 */
    private String suitable;

    /** 状态：在售 / 停售 / 下架 */
    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
