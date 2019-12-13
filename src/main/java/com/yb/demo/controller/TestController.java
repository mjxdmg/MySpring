package com.yb.demo.controller;

import com.yb.demo.service.ITestService;
import com.yb.myspring.annotation.MyAutowired;
import com.yb.myspring.annotation.MyController;
import com.yb.myspring.annotation.MyRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TestController
 *
 * @author YB
 * @date 2019-01-01
 */
@MyController
@MyRequestMapping("/test")
public class TestController {

    @MyAutowired
    ITestService testService;

    /**
     * http://localhost:9999/MySpring/test/queryByUserName?userName=aaa
     */
    @MyRequestMapping("/queryByUserName")
    public void queryByUserName(HttpServletRequest request, HttpServletResponse response) {
        String userName = request.getParameter("userName");
        try {
            if (userName == null) {
                response.getWriter().write("param userName is null");
            } else {
                response.getWriter().write("param userName is " + userName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * http://localhost:9999/MySpring/test/queryByService
     */
    @MyRequestMapping("/queryByService")
    public void queryByService(HttpServletRequest request, HttpServletResponse response) {
        String str = testService.queryByService();
        try {
            response.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
