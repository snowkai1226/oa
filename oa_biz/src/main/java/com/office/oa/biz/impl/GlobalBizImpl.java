package com.office.oa.biz.impl;

import com.office.oa.biz.GlobalBiz;
import com.office.oa.dao.EmployeeDao;
import com.office.oa.entity.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("globalBiz")
public class GlobalBizImpl implements GlobalBiz {
    @Resource
    private EmployeeDao employeeDao;

    public Employee login(String sn, String password) {
        Employee employee = employeeDao.select(sn);
        if (employee != null && employee.getPassword().equals(password)){
            return employee;
        }
        return null;
    }

    public void changePassword(Employee employee) {
        employeeDao.update(employee);
    }
}
