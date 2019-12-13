package com.yb.myspring.servlet;

import com.yb.myspring.annotation.MyAutowired;
import com.yb.myspring.annotation.MyController;
import com.yb.myspring.annotation.MyRequestMapping;
import com.yb.myspring.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 自定义DispatchServlet
 *
 * @author YB
 * @date 2019-01-01
 */
public class MyDispatchServlet extends HttpServlet {

    /**
     * 属性配置文件
     */
    private Properties contextConfig = new Properties();
    /**
     * 类集合
     */
    private List<String> classNameList = new ArrayList<>();
    /**
     * IOC容器
     */
    Map<String, Object> iocMap = new HashMap<>();
    /**
     * 请求URL
     */
    Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        //1、加载配置文件
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));

        //2、扫描相关的类
        doScanner(contextConfig.getProperty("scan-package"));

        //3、初始化IOC容器，将所有相关的类实例保存到IOC容器中
        doInstance();

        //4、依赖注入
        doAutowired();

        //5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("MySpring FrameWork Is Init");

        //6、打印数据
        doTestPrintData();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //7、运行阶段
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 1、加载配置文件
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            //保存在内存
            contextConfig.load(inputStream);
            System.out.println("[INFO-1] Property File Has Been Saved In ContextConfig");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 2、扫描相关的类
     */
    private void doScanner(String scanPackage) {
        URL resourcePath = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        if (resourcePath == null) {
            return;
        }
        File classPath = new File(resourcePath.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                System.out.println("[INFO-2] {" + file.getName() + "} Is A Directory");

                //子目录递归
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    System.out.println("[INFO-2] {" + file.getName() + "} Is Not A Class File");
                    continue;
                }
                String className = (scanPackage + "." + file.getName()).replace(".class", "");

                //保存在内存
                classNameList.add(className);
                System.out.println("[INFO-2] {" + className + "} Has Been Saved In ClassNameList");
            }
        }
    }

    /**
     * 3、初始化IOC容器，将所有相关的类实例保存到IOC容器中
     */
    private void doInstance() {
        if (classNameList.isEmpty()) {
            return;
        }
        try {
            for (String className : classNameList) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();

                    //保存在IOC容器
                    iocMap.put(beanName, instance);
                    System.out.println("[INFO-3] {" + beanName + "} Has Been Saved In IocMap");
                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());

                    //如果注解包含自定义名称
                    MyService xService = clazz.getAnnotation(MyService.class);
                    if (!"".equals(xService.value())) {
                        beanName = xService.value();
                    }
                    Object instance = clazz.newInstance();
                    iocMap.put(beanName, instance);
                    System.out.println("[INFO-3] {" + beanName + "} Has Been Saved In IocMap");

                    //找类的接口
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (iocMap.containsKey(i.getName())) {
                            throw new Exception("The Bean Name Is Exist");
                        }
                        iocMap.put(i.getName(), instance);
                        System.out.println("[INFO-3] {" + i.getName() + "} Has Been Saved In IocMap");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 4、依赖注入
     */
    private void doAutowired() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }
                System.out.println("[INFO-4] Existence MyAutowired");

                //获取注解对应的类
                MyAutowired xAutowired = field.getAnnotation(MyAutowired.class);
                String beanName = xAutowired.value().trim();

                //获取MyAutowired注解的值
                if ("".equals(beanName)) {
                    System.out.println("[INFO] MyAutowired Value() Is Null");
                    beanName = field.getType().getName();
                }

                //只要加了注解，都要加载，不管是private还是protect
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), iocMap.get(beanName));
                    System.out.println("[INFO-4] Field Set {" + entry.getValue() + "} - {" + iocMap.get(beanName) + "}");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 5、初始化 HandlerMapping
     */
    private void initHandlerMapping() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping xRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = xRequestMapping.value();
            }
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping xRequestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = ("/" + baseUrl + "/" + xRequestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("[INFO-5] HandlerMapping Put {" + url + "} - {" + method + "}");
            }
        }
    }

    /**
     * 6、打印数据
     */
    private void doTestPrintData() {
        System.out.println("[INFO-6]-----Data-----");
        System.out.println("ContextConfig.propertyNames()-->" + contextConfig.propertyNames());
        System.out.println("[ClassNameList]-->");
        for (String str : classNameList) {
            System.out.println(str);
        }

        System.out.println("[IocMap]-->");
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("[HandlerMapping]-->");
        for (Map.Entry<String, Method> entry : handlerMapping.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("[INFO-6]-----Done-----");
        System.out.println("=====启动成功=====");
        System.out.println("测试地址：http://localhost:9999/MySpring/test/query?username=yb");
        System.out.println("测试地址：http://localhost:9999/MySpring/test/listClassName");
    }

    /**
     * 7、运行阶段，进行拦截，匹配
     *
     * @param req  请求
     * @param resp 响应
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        System.out.println("[INFO-7] Request Url-->" + url);
        if (!this.handlerMapping.containsKey(url)) {
            try {
                resp.getWriter().write("404 NOT FOUND");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Method method = this.handlerMapping.get(url);
        System.out.println("[INFO-7] Method-->" + method);

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        System.out.println("[INFO-7] IocMap.get(beanName)->" + iocMap.get(beanName));

        //第一个参数是获取方法，后面是参数，多个参数直接加，按顺序对应
        method.invoke(iocMap.get(beanName), req, resp);
        System.out.println("[INFO-7] Method Invoke Put {" + iocMap.get(beanName) + "}.");
    }

    /**
     * 获取类的首字母小写的名称
     *
     * @param className ClassName
     * @return java.lang.String
     */
    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

}
