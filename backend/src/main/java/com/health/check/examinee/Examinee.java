package com.health.check.examinee;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/** 体检人：只能预约在售套餐；年龄 0~120；已取消与已完成为终态；开始体检后置体检中。 */
@Data
@Entity
@Table(name = "examinee")
public class Examinee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 证件号 */
    private String idCard;

    /** 性别：男 / 女 */
    private String gender;

    /** 年龄，0~120 */
    private Integer age;

    /** 单位，可空 */
    private String company;

    /** 体检套餐 id（外键 → package.id），必须在售 */
    private Long packageId;

    /** 状态：已预约 / 体检中 / 已完成 / 已取消 */
    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
