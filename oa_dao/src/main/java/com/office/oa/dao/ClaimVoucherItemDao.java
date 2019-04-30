package com.office.oa.dao;

import com.office.oa.entity.ClaimVoucherItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("claimVoucherItemDao")
public interface ClaimVoucherItemDao {
    void insert(ClaimVoucherItem item);

    void update(ClaimVoucherItem item);

    void delete(int id);

    List<ClaimVoucherItem> selectByClaimVoucher(int cvId);
}
