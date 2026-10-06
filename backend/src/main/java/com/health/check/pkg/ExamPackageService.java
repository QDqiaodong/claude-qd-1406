package com.health.check.pkg;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.health.check.common.BizException;
import com.health.check.common.Dict;

@Service
public class ExamPackageService {

    private final ExamPackageRepository repo;

    public ExamPackageService(ExamPackageRepository repo) {
        this.repo = repo;
    }

    public List<ExamPackage> list() {
        return repo.findAll();
    }

    /** 可被预约的套餐（仅在售），供体检人预约时选择。 */
    public List<ExamPackage> listBookable() {
        return repo.findByStatus("在售");
    }

    public ExamPackage get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("体检套餐不存在"));
    }

    public ExamPackage create(ExamPackage in) {
        String packageName = trimToNull(in.getPackageName());
        if (packageName == null) {
            throw new BizException("套餐名不可为空");
        }

        BigDecimal price = in.getPrice();
        if (price == null) {
            throw new BizException("套餐价格不可为空");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("套餐价格必须大于 0");
        }

        Integer itemCount = in.getItemCount();
        if (itemCount == null) {
            throw new BizException("套餐项目数不可为空");
        }
        if (itemCount <= 0) {
            throw new BizException("套餐项目数必须大于 0");
        }

        String suitable = trimToNull(in.getSuitable());
        if (suitable == null) {
            throw new BizException("适用人群不可为空");
        }
        if (!Dict.isValidPkgSuitable(suitable)) {
            throw new BizException("适用人群必须是：入职体检 / 年度体检 / 老年体检 / 女性专项 / 儿童体检");
        }

        String status = trimToNull(in.getStatus());
        if (status == null) {
            status = "在售";
        }
        if (!Dict.isValidPkgStatus(status)) {
            throw new BizException("套餐状态必须是：在售 / 停售 / 下架");
        }
        ExamPackage p = new ExamPackage();
        p.setPackageName(packageName);
        p.setPrice(price);
        p.setItemCount(itemCount);
        p.setSuitable(suitable);
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    public ExamPackage update(Long id, ExamPackage in) {
        ExamPackage p = repo.findById(id).orElseThrow(() -> new BizException("体检套餐不存在"));
        if ("下架".equals(p.getStatus())) {
            throw new BizException("套餐已下架，为终态不可再修改");
        }

        String packageName = trimToNull(in.getPackageName());
        if (packageName != null) {
            p.setPackageName(packageName);
        }

        BigDecimal price = in.getPrice();
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("套餐价格必须大于 0");
            }
            p.setPrice(price);
        }

        Integer itemCount = in.getItemCount();
        if (itemCount != null) {
            if (itemCount <= 0) {
                throw new BizException("套餐项目数必须大于 0");
            }
            p.setItemCount(itemCount);
        }

        String suitable = trimToNull(in.getSuitable());
        if (suitable != null) {
            if (!Dict.isValidPkgSuitable(suitable)) {
                throw new BizException("适用人群必须是：入职体检 / 年度体检 / 老年体检 / 女性专项 / 儿童体检");
            }
            p.setSuitable(suitable);
        }

        String status = trimToNull(in.getStatus());
        if (status != null) {
            changeStatus(p, status);
        }
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    /** 套餐状态流转：在售↔停售，二者可下架；下架为终态。 */
    public ExamPackage changeStatus(Long id, String status) {
        ExamPackage p = repo.findById(id).orElseThrow(() -> new BizException("体检套餐不存在"));
        String target = trimToNull(status);
        if (target == null) {
            throw new BizException("目标状态不可为空");
        }
        changeStatus(p, target);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    private void changeStatus(ExamPackage p, String target) {
        if (!Dict.isValidPkgStatus(target)) {
            throw new BizException("套餐状态必须是：在售 / 停售 / 下架");
        }
        String from = p.getStatus();
        if (from != null && from.equals(target)) {
            return;
        }
        if (!Dict.canTransferPkg(from, target)) {
            if ("下架".equals(from)) {
                throw new BizException("套餐已下架，为终态不可再变更状态");
            }
            throw new BizException("套餐状态不能由「" + from + "」直接流转到「" + target + "」");
        }
        p.setStatus(target);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
