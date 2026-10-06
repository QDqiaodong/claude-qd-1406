package com.health.check.common;

import java.util.List;
import java.util.Map;

/**
 * 体检中心业务词典与规则参数表：科室状态流转、体检套餐适用人群 / 状态流转、
 * 体检人性别 / 状态流转、检查项名称 / 检查状态流转。规则集中在此，便于出题时被要求调整或扩展。
 */
public final class Dict {

    private Dict() {
    }

    // ---------------- 年龄边界 ----------------
    /** 体检人年龄下限 */
    public static final int AGE_MIN = 0;

    /** 体检人年龄上限 */
    public static final int AGE_MAX = 120;

    // ---------------- 科室 dept ----------------
    /** 开放 / 停诊 / 装修 */
    public static final List<String> DEPT_STATUS = List.of("开放", "停诊", "装修");

    /** 不可排检查项的科室状态：停诊、装修 */
    public static final List<String> DEPT_UNAVAILABLE = List.of("停诊", "装修");

    /** 科室状态机：开放→停诊/装修；停诊→开放/装修；装修完成只能恢复为开放 */
    public static final Map<String, List<String>> DEPT_FLOW = Map.of(
            "开放", List.of("停诊", "装修"),
            "停诊", List.of("开放", "装修"),
            "装修", List.of("开放"));

    // ---------------- 体检套餐 package ----------------
    /** 入职体检 / 年度体检 / 老年体检 / 女性专项 / 儿童体检 */
    public static final List<String> PKG_SUITABLE = List.of("入职体检", "年度体检", "老年体检", "女性专项", "儿童体检");

    /** 在售 / 停售 / 下架 */
    public static final List<String> PKG_STATUS = List.of("在售", "停售", "下架");

    /** 不可被预约的套餐状态：停售、下架 */
    public static final List<String> PKG_UNBOOKABLE = List.of("停售", "下架");

    /** 套餐状态机：在售→停售/下架；停售→在售/下架；下架为终态不可再改 */
    public static final Map<String, List<String>> PKG_FLOW = Map.of(
            "在售", List.of("停售", "下架"),
            "停售", List.of("在售", "下架"),
            "下架", List.of());

    // ---------------- 体检人 examinee ----------------
    /** 男 / 女 */
    public static final List<String> GENDER = List.of("男", "女");

    /** 已预约 / 体检中 / 已完成 / 已取消 */
    public static final List<String> EXAMINEE_STATUS = List.of("已预约", "体检中", "已完成", "已取消");

    /** 允许做检查的体检人状态：只有体检中 */
    public static final List<String> EXAMINEE_CHECKABLE = List.of("体检中");

    /** 体检人状态机：已预约→体检中/已取消；体检中→已完成/已取消；已完成、已取消为终态 */
    public static final Map<String, List<String>> EXAMINEE_FLOW = Map.of(
            "已预约", List.of("体检中", "已取消"),
            "体检中", List.of("已完成", "已取消"),
            "已完成", List.of(),
            "已取消", List.of());

    // ---------------- 检查项记录 exam ----------------
    /** 身高体重 / 血压 / 血常规 / 尿常规 / 心电图 / 胸片 / B超 / 肝功能 */
    public static final List<String> EXAM_ITEM = List.of("身高体重", "血压", "血常规", "尿常规", "心电图", "胸片", "B超", "肝功能");

    /** 待检 / 检查中 / 已完成 / 已弃检 */
    public static final List<String> EXAM_STATUS = List.of("待检", "检查中", "已完成", "已弃检");

    /** 检查项状态机：待检→检查中/已完成/已弃检；检查中→已完成/已弃检；已完成、已弃检为终态 */
    public static final Map<String, List<String>> EXAM_FLOW = Map.of(
            "待检", List.of("检查中", "已完成", "已弃检"),
            "检查中", List.of("已完成", "已弃检"),
            "已完成", List.of(),
            "已弃检", List.of());

    /** 未完成的检查项状态（用于判定体检人是否全部查完） */
    public static final List<String> EXAM_UNFINISHED = List.of("待检", "检查中");

    // ---------------- 取值方法 ----------------

    public static boolean isValidDeptStatus(String s) {
        return s != null && DEPT_STATUS.contains(s);
    }

    public static boolean isValidPkgSuitable(String s) {
        return s != null && PKG_SUITABLE.contains(s);
    }

    public static boolean isValidPkgStatus(String s) {
        return s != null && PKG_STATUS.contains(s);
    }

    public static boolean isValidGender(String g) {
        return g != null && GENDER.contains(g);
    }

    public static boolean isValidExamineeStatus(String s) {
        return s != null && EXAMINEE_STATUS.contains(s);
    }

    public static boolean isValidExamItem(String i) {
        return i != null && EXAM_ITEM.contains(i);
    }

    public static boolean isValidExamStatus(String s) {
        return s != null && EXAM_STATUS.contains(s);
    }

    /** 科室是否可排检查项：只有开放状态可以 */
    public static boolean isDeptAvailable(String status) {
        return status != null && !DEPT_UNAVAILABLE.contains(status);
    }

    /** 套餐是否可被预约：只有在售状态可以 */
    public static boolean isPkgBookable(String status) {
        return status != null && !PKG_UNBOOKABLE.contains(status);
    }

    /** 体检人是否可做检查：只有体检中可以 */
    public static boolean isExamineeCheckable(String status) {
        return status != null && EXAMINEE_CHECKABLE.contains(status);
    }

    /** 科室状态流转校验：装修完成只能恢复为开放，其余按 DEPT_FLOW 前进 */
    public static boolean canTransferDept(String from, String to) {
        if (!isValidDeptStatus(from) || !isValidDeptStatus(to)) {
            return false;
        }
        List<String> next = DEPT_FLOW.get(from);
        return next != null && next.contains(to);
    }

    /** 套餐状态流转校验：下架为终态不可再变更 */
    public static boolean canTransferPkg(String from, String to) {
        if (!isValidPkgStatus(from) || !isValidPkgStatus(to)) {
            return false;
        }
        List<String> next = PKG_FLOW.get(from);
        return next != null && next.contains(to);
    }

    /** 体检人状态流转校验：已取消、已已完成为终态不可变更 */
    public static boolean canTransferExaminee(String from, String to) {
        if (!isValidExamineeStatus(from) || !isValidExamineeStatus(to)) {
            return false;
        }
        List<String> next = EXAMINEE_FLOW.get(from);
        return next != null && next.contains(to);
    }

    /** 检查项状态流转校验：已完成、已弃检为终态不可变更 */
    public static boolean canTransferExam(String from, String to) {
        if (!isValidExamStatus(from) || !isValidExamStatus(to)) {
            return false;
        }
        List<String> next = EXAM_FLOW.get(from);
        return next != null && next.contains(to);
    }
}
