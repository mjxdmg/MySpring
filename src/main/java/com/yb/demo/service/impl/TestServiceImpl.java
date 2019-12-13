package com.yb.demo.service.impl;

import com.yb.demo.service.ITestService;
import com.yb.myspring.annotation.MyService;

/**
 * TestServiceImpl
 *
 * @author YB
 * @date 2019-01-01
 */
@MyService
public class TestServiceImpl implements ITestService {

    @Override
    public String queryByService() {
        return "queryByTestServiceImpl";
    }

}
