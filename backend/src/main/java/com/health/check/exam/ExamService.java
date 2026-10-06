package com.health.check.exam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.health.check.common.BizException;
import com.health.check.common.Dict;
import com.health.check.dept.Dept;
import com.health.check.dept.DeptRepository;
import com.health.check.examinee.Examinee;
import com.health.check.examinee.ExamineeRepository;

@Service
public class ExamService {

    private final ExamRepository repo;
    private final ExamineeRepository examineeRepo;
    private final DeptRepository deptRepo;

    public ExamService(ExamRepository repo, ExamineeRepository examineeRepo, DeptRepository deptRepo) {
        this.repo = repo;
        this.examineeRepo = examineeRepo;
        this.deptRepo = deptRepo;
    }

    public List<Exam> list() {
        return repo.findAll();
    }

    public List<Exam> listByExaminee(Long examineeId) {
        return repo.findByExamineeId(examineeId);
    }

    public List<Exam> listByDept(Long deptId) {
        return repo.findByDeptId(deptId);
    }

    public Exam get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("检查项记录不存在"));
    }

    /** 检查项进度：总数、已完成数、异常数、百分比等，供前端检查进度条使用。 */
    public Map<String, Object> progress(Long examineeId) {
        List<Exam> items = repo.findByExamineeId(examineeId);
        int total = items.size();
        int done = 0;
        int checking = 0;
        int waiting = 0;
        int skipped = 0;
        int abnormal = 0;
        for (Exam e : items) {
            switch (e.getStatus()) {
                case "已完成" -> done++;
                case "检查中" -> checking++;
                case "待检" -> waiting++;
                case "已弃检" -> skipped++;
                default -> {
                }
            }
            if (Boolean.TRUE.equals(e.getAbnormal())) {
                abnormal++;
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("examineeId", examineeId);
        m.put("total", total);
        m.put("done", done);
        m.put("checking", checking);
        m.put("waiting", waiting);
        m.put("skipped", skipped);
        m.put("abnormal", abnormal);
        m.put("finished", done + skipped);
        m.put("percent", total == 0 ? 0 : Math.round((done + skipped) * 100.0 / total));
        return m;
    }

    /** 排检查项：科室必须开放，体检人必须体检中。 */
    public Exam create(Exam in) {
        Long examineeId = in.getExamineeId();
        if (examineeId == null) {
            throw new BizException("请选择体检人");
        }
        Long deptId = in.getDeptId();
        if (deptId == null) {
            throw new BizException("请选择检查科室");
        }

        Examinee e = examineeRepo.findById(examineeId).orElseThrow(() -> new BizException("体检人不存在"));
        Dept d = deptRepo.findById(deptId).orElseThrow(() -> new BizException("检查科室不存在"));
        requireCheckable(e);
        requireAvailable(d);

        String itemName = trimToNull(in.getItemName());
        if (itemName == null) {
            throw new BizException("检查项不可为空");
        }
        if (!Dict.isValidExamItem(itemName)) {
            throw new BizException("检查项必须是：身高体重 / 血压 / 血常规 / 尿常规 / 心电图 / 胸片 / B超 / 肝功能");
        }
        if (!repo.findByExamineeIdAndItemName(examineeId, itemName).isEmpty()) {
            throw new BizException("该体检人已排过检查项「" + itemName + "」，请勿重复排检");
        }

        String status = trimToNull(in.getStatus());
        if (status == null) {
            status = "待检";
        }
        if (!Dict.isValidExamStatus(status)) {
            throw new BizException("检查项状态必须是：待检 / 检查中 / 已完成 / 已弃检");
        }

        Exam x = new Exam();
        x.setExamineeId(e.getId());
        x.setDeptId(d.getId());
        x.setItemName(itemName);
        x.setStatus(status);
        x.setResult(trimToNull(in.getResult()));
        x.setAbnormal(in.getAbnormal() != null && in.getAbnormal());
        x.setCheckTime(in.getCheckTime());
        x.setCreatedAt(LocalDateTime.now());
        x.setUpdatedAt(LocalDateTime.now());
        return repo.save(x);
    }

    public Exam update(Long id, Exam in) {
        Exam x = repo.findById(id).orElseThrow(() -> new BizException("检查项记录不存在"));
        checkMutable(x);

        String itemName = trimToNull(in.getItemName());
        if (itemName != null && !itemName.equals(x.getItemName())) {
            if (!Dict.isValidExamItem(itemName)) {
                throw new BizException("检查项必须是：身高体重 / 血压 / 血常规 / 尿常规 / 心电图 / 胸片 / B超 / 肝功能");
            }
            x.setItemName(itemName);
        }

        if (in.getDeptId() != null && !in.getDeptId().equals(x.getDeptId())) {
            Dept d = deptRepo.findById(in.getDeptId()).orElseThrow(() -> new BizException("检查科室不存在"));
            requireAvailable(d);
            x.setDeptId(d.getId());
        }

        String result = trimToNull(in.getResult());
        if (result != null) {
            x.setResult(result);
        }
        if (in.getAbnormal() != null) {
            if (in.getAbnormal() && (result == null && trimToNull(x.getResult()) == null)) {
                throw new BizException("标记异常时必须填写结果描述");
            }
            x.setAbnormal(in.getAbnormal());
        }
        x.setUpdatedAt(LocalDateTime.now());
        return repo.save(x);
    }

    /** 检查项状态流转：完成后自动写 checkTime，全部查完后自动把体检人置为已完成。 */
    public Exam changeStatus(Long id, String status, String result, Boolean abnormal) {
        Exam x = repo.findById(id).orElseThrow(() -> new BizException("检查项记录不存在"));
        String target = trimToNull(status);
        if (target == null) {
            throw new BizException("目标状态不可为空");
        }
        if (!Dict.isValidExamStatus(target)) {
            throw new BizException("检查项状态必须是：待检 / 检查中 / 已完成 / 已弃检");
        }
        checkMutable(x);

        Examinee e = examineeRepo.findById(x.getExamineeId()).orElseThrow(() -> new BizException("体检人不存在"));
        Dept d = deptRepo.findById(x.getDeptId()).orElseThrow(() -> new BizException("检查科室不存在"));
        requireCheckable(e);
        requireAvailable(d);

        String from = x.getStatus();
        if (from != null && from.equals(target)) {
            return x;
        }
        if (!Dict.canTransferExam(from, target)) {
            throw new BizException("检查项状态不能由「" + from + "」直接流转到「" + target + "」");
        }

        String rst = trimToNull(result);
        boolean ab = Boolean.TRUE.equals(abnormal);

        if ("已完成".equals(target)) {
            if (rst == null && trimToNull(x.getResult()) == null) {
                throw new BizException("标记已完成时必须填写检查结果");
            }
            if (rst != null) {
                x.setResult(rst);
            }
            x.setCheckTime(LocalDateTime.now());
        } else if ("已弃检".equals(target)) {
            if (rst == null && trimToNull(x.getResult()) == null) {
                throw new BizException("弃检必须填写理由");
            }
            if (rst != null) {
                x.setResult(rst);
            }
            x.setCheckTime(null);
        } else {
            if (rst != null) {
                x.setResult(rst);
            }
        }

        if (ab && trimToNull(x.getResult()) == null) {
            throw new BizException("标记异常时必须填写结果描述");
        }
        x.setAbnormal(ab);
        x.setStatus(target);
        x.setUpdatedAt(LocalDateTime.now());
        Exam saved = repo.save(x);

        autoFinishExaminee(saved.getExamineeId());
        return saved;
    }

    /** 删除待检的检查项（检查中及终态不可删除）。 */
    public void delete(Long id) {
        Exam x = repo.findById(id).orElseThrow(() -> new BizException("检查项记录不存在"));
        checkMutable(x);
        if (!"待检".equals(x.getStatus())) {
            throw new BizException("只有待检的检查项可以撤销，当前状态为「" + x.getStatus() + "」");
        }
        repo.deleteById(id);
    }

    /** 全部检查项查完后（无待检/检查中），服务端自动把体检人置为已完成。 */
    private void autoFinishExaminee(Long examineeId) {
        Examinee e = examineeRepo.findById(examineeId).orElse(null);
        if (e == null || !"体检中".equals(e.getStatus())) {
            return;
        }
        List<Exam> items = repo.findByExamineeId(examineeId);
        if (items.isEmpty()) {
            return;
        }
        boolean allFinished = true;
        for (Exam item : items) {
            if (Dict.EXAM_UNFINISHED.contains(item.getStatus())) {
                allFinished = false;
                break;
            }
        }
        if (allFinished && Dict.canTransferExaminee(e.getStatus(), "已完成")) {
            e.setStatus("已完成");
            e.setUpdatedAt(LocalDateTime.now());
            examineeRepo.save(e);
        }
    }

    /** 只有体检中的体检人可以做检查。 */
    private void requireCheckable(Examinee e) {
        if (!Dict.isExamineeCheckable(e.getStatus())) {
            throw new BizException("体检人「" + e.getName() + "」当前为" + e.getStatus() + "，不可做检查");
        }
    }

    /** 停诊与装修的科室不可排检查项。 */
    private void requireAvailable(Dept d) {
        if (!Dict.isDeptAvailable(d.getStatus())) {
            throw new BizException("科室「" + d.getDeptName() + "」当前为" + d.getStatus() + "，不可排检查项");
        }
    }

    /** 已完成、已弃检为终态，不可再修改。 */
    private void checkMutable(Exam x) {
        if ("已完成".equals(x.getStatus())) {
            throw new BizException("该检查项已完成，为终态不可再修改");
        }
        if ("已弃检".equals(x.getStatus())) {
            throw new BizException("该检查项已弃检，为终态不可再修改");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
