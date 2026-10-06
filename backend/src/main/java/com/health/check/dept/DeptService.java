package com.health.check.dept;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.health.check.common.BizException;
import com.health.check.common.Dict;

@Service
public class DeptService {

    private final DeptRepository repo;

    public DeptService(DeptRepository repo) {
        this.repo = repo;
    }

    public List<Dept> list() {
        return repo.findAll();
    }

    /** 可排检查项的科室（仅开放），供检查项下单时选择。 */
    public List<Dept> listAvailable() {
        return repo.findByStatus("开放");
    }

    public Dept get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("科室不存在"));
    }

    public Dept create(Dept in) {
        String deptNo = trimToNull(in.getDeptNo());
        if (deptNo == null) {
            throw new BizException("科室编号不可为空");
        }
        if (repo.findByDeptNo(deptNo).isPresent()) {
            throw new BizException("科室编号 " + deptNo + " 已存在，请更换");
        }
        String deptName = trimToNull(in.getDeptName());
        if (deptName == null) {
            throw new BizException("科室名不可为空");
        }

        String status = trimToNull(in.getStatus());
        if (status == null) {
            status = "开放";
        }
        if (!Dict.isValidDeptStatus(status)) {
            throw new BizException("科室状态必须是：开放 / 停诊 / 装修");
        }

        String location = trimToNull(in.getLocation());
        String doctor = trimToNull(in.getDoctor());

        Dept d = new Dept();
        d.setDeptNo(deptNo);
        d.setDeptName(deptName);
        d.setLocation(location == null ? "待定位置" : location);
        d.setDoctor(doctor == null ? "待安排医师" : doctor);
        d.setStatus(status);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return repo.save(d);
    }

    public Dept update(Long id, Dept in) {
        Dept d = repo.findById(id).orElseThrow(() -> new BizException("科室不存在"));

        String deptNo = trimToNull(in.getDeptNo());
        if (deptNo != null && !deptNo.equals(d.getDeptNo())) {
            Optional<Dept> dup = repo.findByDeptNo(deptNo);
            if (dup.isPresent() && !dup.get().getId().equals(id)) {
                throw new BizException("科室编号 " + deptNo + " 已存在，请更换");
            }
            d.setDeptNo(deptNo);
        }

        String deptName = trimToNull(in.getDeptName());
        if (deptName != null) {
            d.setDeptName(deptName);
        } else if (d.getDeptName() == null || d.getDeptName().isBlank()) {
            throw new BizException("科室名不可为空");
        }

        String location = trimToNull(in.getLocation());
        if (location != null) {
            d.setLocation(location);
        }
        String doctor = trimToNull(in.getDoctor());
        if (doctor != null) {
            d.setDoctor(doctor);
        }

        String status = trimToNull(in.getStatus());
        if (status != null && !status.equals(d.getStatus())) {
            changeStatus(d, status);
        }
        d.setUpdatedAt(LocalDateTime.now());
        return repo.save(d);
    }

    /** 科室状态流转：停诊 / 装修可互转，装修完成后只能恢复为开放。 */
    public Dept changeStatus(Long id, String status) {
        Dept d = repo.findById(id).orElseThrow(() -> new BizException("科室不存在"));
        String target = trimToNull(status);
        if (target == null) {
            throw new BizException("科室状态不可为空");
        }
        changeStatus(d, target);
        d.setUpdatedAt(LocalDateTime.now());
        return repo.save(d);
    }

    private void changeStatus(Dept d, String target) {
        if (!Dict.isValidDeptStatus(target)) {
            throw new BizException("科室状态必须是：开放 / 停诊 / 装修");
        }
        String from = d.getStatus();
        if (from != null && from.equals(target)) {
            return;
        }
        if (!Dict.canTransferDept(from, target)) {
            if ("装修".equals(from)) {
                throw new BizException("装修中的科室装修完成后只能恢复为开放，不能直接置为停诊");
            }
            throw new BizException("科室状态不能由「" + from + "」直接流转到「" + target + "」");
        }
        d.setStatus(target);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
