package com.office.oa.biz.impl;

import com.office.oa.biz.ClaimVoucherBiz;
import com.office.oa.dao.ClaimVoucherDao;
import com.office.oa.dao.ClaimVoucherItemDao;
import com.office.oa.dao.DealRecordDao;
import com.office.oa.dao.EmployeeDao;
import com.office.oa.entity.ClaimVoucher;
import com.office.oa.entity.ClaimVoucherItem;
import com.office.oa.entity.DealRecord;
import com.office.oa.entity.Employee;
import com.office.oa.global.Contant;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service("claimVoucherBiz")
public class ClaimVoucherBizImpl implements ClaimVoucherBiz {

    @Resource
    private ClaimVoucherDao claimVoucherDao;
    @Resource
    private ClaimVoucherItemDao claimVoucherItemDao;
    @Resource
    private DealRecordDao dealRecordDao;

    @Resource
    private EmployeeDao employeeDao;

    public void save(ClaimVoucher claimVoucher, List<ClaimVoucherItem> items) {
        claimVoucher.setCreateTime(new Date());
        claimVoucher.setNextDealSn(claimVoucher.getCreateSn());
        claimVoucher.setStatus(Contant.CLAIMVOUCHER_CREATED);
        //insert后，会给claimVoucher的id赋值
        claimVoucherDao.insert(claimVoucher);
        for (ClaimVoucherItem item : items) {
            item.setClaimVoucherId(claimVoucher.getId());
            claimVoucherItemDao.insert(item);
        }

    }

    public ClaimVoucher get(int id) {
        return claimVoucherDao.select(id);
    }

    public List<ClaimVoucherItem> getItems(int cvId) {
        return claimVoucherItemDao.selectByClaimVoucher(cvId);
    }

    public List<DealRecord> getRecords(int cvId) {
        return dealRecordDao.selectByClaimVoucher(cvId);
    }

    public List<ClaimVoucher> getForSelf(String sn) {
        return claimVoucherDao.selectByCreator(sn);
    }

    public List<ClaimVoucher> getForDeal(String sn) {
        return claimVoucherDao.selectByDealer(sn);
    }

    public void update(ClaimVoucher claimVoucher, List<ClaimVoucherItem> items) {
        claimVoucher.setNextDealSn(claimVoucher.getCreateSn());
        claimVoucher.setStatus(Contant.CLAIMVOUCHER_CREATED);
        claimVoucherDao.update(claimVoucher);

        List<ClaimVoucherItem> olds = claimVoucherItemDao.selectByClaimVoucher(claimVoucher.getId());
//        从数据库中查找旧条目，并且验证旧条目是否被删除
        for (ClaimVoucherItem old : olds) {
            boolean isHave = false;
            for (ClaimVoucherItem item : items) {
//                该旧条目没有被删除，跳出第一层循环
                if (item.getId() == old.getId()) {
                    isHave = true;
                    break;
                }
            }
//            如果isHave为假，则说明旧条目被删除了，此时应在数据库中删除这条旧条目
            if (!isHave) {
                claimVoucherItemDao.delete(old.getId());
            }
        }
        for (ClaimVoucherItem item : items) {
            item.setClaimVoucherId(claimVoucher.getId());
//            如果id大于零，说明本身存在数据库中，只需要更新,因为数据库中的旧记录是有id的，而新生成的条目还没有插入数据库，是没有id的
            if (item.getId() != null && item.getId() > 0) {
                claimVoucherItemDao.update(item);
            }
//            否则说明不存在，需要插入
            else{
                claimVoucherItemDao.insert(item);
            }
        }
    }

    public void submit(int id) {
//        1.提交后状态和待处理人会变
        ClaimVoucher claimVoucher = claimVoucherDao.select(id);
        Employee creator = employeeDao.select(claimVoucher.getCreateSn());
        claimVoucher.setStatus(Contant.CLAIMVOUCHER_SUBMIT);

//        设置待处理人（部门经理）编号
        List<Employee> dealers = employeeDao.selectByDepartmentAndPost(creator.getDepartmentSn(), Contant.POST_DM);
////        如果本部门没有部门经理，则交给总经理处理
//        if (dealers.isEmpty()) {
//
//        }else {
//
//        }
        claimVoucher.setNextDealSn(dealers.get(0).getSn());
        claimVoucherDao.update(claimVoucher);

//        2.进行记录的保存
        DealRecord dealRecord = new DealRecord();
        dealRecord.setDealWay(Contant.DEAL_SUBMIT);
        dealRecord.setDealSn(creator.getSn());
        dealRecord.setClaimVoucherId(id);
        dealRecord.setDealResult(Contant.CLAIMVOUCHER_SUBMIT);
        dealRecord.setDealTime(new Date());
        dealRecord.setComment("无");
        dealRecordDao.insert(dealRecord);
    }

    public void deal(DealRecord dealRecord) {
        ClaimVoucher claimVoucher = claimVoucherDao.select(dealRecord.getClaimVoucherId());
        Employee employee = employeeDao.select(dealRecord.getDealSn());
//        如果当前处理记录为通过,审核
        if (dealRecord.getDealWay().equals(Contant.DEAL_PASS)) {
//            如果通过时金额少于限额或者通过处理人为总经理,不需要复审
            if (claimVoucher.getTotalAmount() <= Contant.LIMIT_CHECK || employee.getPost().equals(Contant.POST_GM)) {
                claimVoucher.setStatus(Contant.CLAIMVOUCHER_APPROVED);
//                设置待处理人为财务
                claimVoucher.setNextDealSn(employeeDao.selectByDepartmentAndPost(null, Contant.POST_CASHIER).get(0).getSn());

//                记录处理

                dealRecord.setDealResult(Contant.CLAIMVOUCHER_APPROVED);
            }else {
                claimVoucher.setStatus(Contant.CLAIMVOUCHER_RECHECK);
//                设置待处理人为总经理
                claimVoucher.setNextDealSn(employeeDao.selectByDepartmentAndPost(null, Contant.POST_GM).get(0).getSn());

//                记录处理

                dealRecord.setDealResult(Contant.CLAIMVOUCHER_RECHECK);
            }
        }
//        打回
        else if (dealRecord.getDealWay().equals(Contant.DEAL_BACK)){
            claimVoucher.setStatus(Contant.CLAIMVOUCHER_BACK);
//                设置待处理人为创建者
            claimVoucher.setNextDealSn(claimVoucher.getCreateSn());

//                记录处理

            dealRecord.setDealResult(Contant.CLAIMVOUCHER_BACK);

        }
//        拒绝
        else if (dealRecord.getDealWay().equals(Contant.DEAL_REJECT)) {
            claimVoucher.setStatus(Contant.CLAIMVOUCHER_TERMINATED);
//                设置待处理人为null
            claimVoucher.setNextDealSn(null);
//                记录处理

            dealRecord.setDealResult(Contant.CLAIMVOUCHER_TERMINATED);
        }
//        打款
        else if (dealRecord.getDealWay().equals(Contant.DEAL_PAID)){
            claimVoucher.setStatus(Contant.CLAIMVOUCHER_PAID);
//                设置待处理人为null
            claimVoucher.setNextDealSn(null);
//                记录处理

            dealRecord.setDealResult(Contant.CLAIMVOUCHER_PAID);
        }
        dealRecord.setDealTime(new Date());
        if (dealRecord.getComment() == null || dealRecord.getComment().equals("")) {
            dealRecord.setComment("无");
        }
        claimVoucherDao.update(claimVoucher);
        dealRecordDao.insert(dealRecord);
    }
}
