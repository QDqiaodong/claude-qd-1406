package com.health.check.examinee;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.health.check.common.BizException;
import com.health.check.common.Dict;
import com.health.check.pkg.ExamPackage;
import com.health.check.pkg.ExamPackageRepository;

@Service
public class ExamineeService {

    private final ExamineeRepository repo;
    private final ExamPackageRepository packageRepo;

    public ExamineeService(ExamineeRepository repo, ExamPackageRepository packageRepo) {
        this.repo = repo;
        this.packageRepo = packageRepo;
    }

    public List<Examinee> list() {
        return repo.findAll();
    }

    public List<Examinee> listByPackage(Long packageId) {
        return repo.findByPackageId(packageId);
    }

    public Examinee get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("体检人不存在"));
    }

    public Examinee create(Examinee in) {
        String name = trimToNull(in.getName());
        if (name == null) {
            throw new BizException("体检人姓名不可为空");
        }
        String phone = trimToNull(in.getPhone());
        if (phone == null) {
            throw new BizException("手机号不可为空");
        }
        String idCard = trimToNull(in.getIdCard());
        if (idCard == null) {
            throw new BizException("证件号不可为空");
        }

        String gender = trimToNull(in.getGender());
        if (gender == null) {
            throw new BizException("性别不可为空");
        }
        if (!Dict.isValidGender(gender)) {
            throw new BizException("性别必须是：男 / 女");
        }

        Integer age = in.getAge();
        if (age == null) {
            throw new BizException("年龄不可为空");
        }
        if (age < Dict.AGE_MIN || age > Dict.AGE_MAX) {
            throw new BizException("年龄必须在 " + Dict.AGE_MIN + "~" + Dict.AGE_MAX + " 之间");
        }

        ExamPackage pkg = requireBookablePackage(in.getPackageId());

        String status = trimToNull(in.getStatus());
        if (status == null) {
            status = "已预约";
        }
        if (!Dict.isValidExamineeStatus(status)) {
            throw new BizException("体检人状态必须是：已预约 / 体检中 / 已完成 / 已取消");
        }

        Examinee e = new Examinee();
        e.setName(name);
        e.setPhone(phone);
        e.setIdCard(idCard);
        e.setGender(gender);
        e.setAge(age);
        e.setCompany(trimToNull(in.getCompany()));
        e.setPackageId(pkg.getId());
        e.setStatus(status);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return repo.save(e);
    }

    public Examinee update(Long id, Examinee in) {
        Examinee e = repo.findById(id).orElseThrow(() -> new BizException("体检人不存在"));
        checkMutable(e);

        String name = trimToNull(in.getName());
        if (name != null) {
            e.setName(name);
        }
        String phone = trimToNull(in.getPhone());
        if (phone != null) {
            e.setPhone(phone);
        }
        String idCard = trimToNull(in.getIdCard());
        if (idCard != null) {
            e.setIdCard(idCard);
        }

        String gender = trimToNull(in.getGender());
        if (gender != null) {
            if (!Dict.isValidGender(gender)) {
                throw new BizException("性别必须是：男 / 女");
            }
            e.setGender(gender);
        }

        Integer age = in.getAge();
        if (age != null) {
            if (age < Dict.AGE_MIN || age > Dict.AGE_MAX) {
                throw new BizException("年龄必须在 " + Dict.AGE_MIN + "~" + Dict.AGE_MAX + " 之间");
            }
            e.setAge(age);
        }

        if (in.getCompany() != null) {
            e.setCompany(trimToNull(in.getCompany()));
        }

        if (in.getPackageId() != null) {
            ExamPackage pkg = requireBookablePackage(in.getPackageId());
            e.setPackageId(pkg.getId());
        }

        String status = trimToNull(in.getStatus());
        if (status != null) {
            changeStatus(e, status);
        }
        e.setUpdatedAt(LocalDateTime.now());
        return repo.save(e);
    }

    /** 开始体检：只有已预约的体检人可以开始，置为体检中。 */
    public Examinee start(Long id) {
        Examinee e = repo.findById(id).orElseThrow(() -> new BizException("体检人不存在"));
        if (!"已预约".equals(e.getStatus())) {
            throw new BizException("只有已预约的体检人可以开始体检，当前状态为「" + e.getStatus() + "」");
        }
        ExamPackage pkg = packageRepo.findById(e.getPackageId())
                .orElseThrow(() -> new BizException("体检套餐不存在，无法开始体检"));
        if (!Dict.isPkgBookable(pkg.getStatus())) {
            throw new BizException("套餐「" + pkg.getPackageName() + "」已" + pkg.getStatus() + "，不可开始体检");
        }
        e.setStatus("体检中");
        e.setUpdatedAt(LocalDateTime.now());
        return repo.save(e);
    }

    /** 体检人状态流转：已预约→体检中/已取消；体检中→已完成/已取消；已完成、已取消为终态。 */
    public Examinee changeStatus(Long id, String status) {
        Examinee e = repo.findById(id).orElseThrow(() -> new BizException("体检人不存在"));
        String target = trimToNull(status);
        if (target == null) {
            throw new BizException("目标状态不可为空");
        }
        changeStatus(e, target);
        e.setUpdatedAt(LocalDateTime.now());
        return repo.save(e);
    }

    private void changeStatus(Examinee e, String target) {
        if (!Dict.isValidExamineeStatus(target)) {
            throw new BizException("体检人状态必须是：已预约 / 体检中 / 已完成 / 已取消");
        }
        checkMutable(e);
        String from = e.getStatus();
        if (from != null && from.equals(target)) {
            return;
        }
        if (!Dict.canTransferExaminee(from, target)) {
            throw new BizException("体检人状态不能由「" + from + "」直接流转到「" + target + "」");
        }
        e.setStatus(target);
    }

    /** 已完成与已取消为终态，不可再修改。 */
    private void checkMutable(Examinee e) {
        if ("已取消".equals(e.getStatus())) {
            throw new BizException("该体检人已取消，为终态不可再修改");
        }
        if ("已完成".equals(e.getStatus())) {
            throw new BizException("该体检人已完成体检，为终态不可再修改");
        }
    }

    /** 校验套餐存在且处于在售状态。 */
    private ExamPackage requireBookablePackage(Long packageId) {
        if (packageId == null) {
            throw new BizException("请选择体检套餐");
        }
        ExamPackage pkg = packageRepo.findById(packageId)
                .orElseThrow(() -> new BizException("体检套餐不存在"));
        if (!Dict.isPkgBookable(pkg.getStatus())) {
            throw new BizException("套餐「" + pkg.getPackageName() + "」当前为" + pkg.getStatus() + "，不可被预约");
        }
        return pkg;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
