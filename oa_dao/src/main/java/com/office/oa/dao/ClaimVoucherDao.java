package com.office.oa.dao;

import com.office.oa.entity.ClaimVoucher;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("claimVoucherDao")
public interface ClaimVoucherDao {
    void insert(ClaimVoucher claimVoucher);

    void update(ClaimVoucher claimVoucher);

    void delete(int id);

    ClaimVoucher select(int id);

    List<ClaimVoucher> selectByCreator(String cSn);

    List<ClaimVoucher> selectByDealer(String dSn);
}
