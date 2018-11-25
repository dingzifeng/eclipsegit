package com.lzl.controller;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.VariableElement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lzl.annotation.AutoWired;
import com.lzl.annotation.Controller;
import com.lzl.annotation.RequestMapping;
import com.lzl.annotation.Service;

@SuppressWarnings("serial")
@WebServlet(urlPatterns="/",loadOnStartup=1)
public class DispatherServlet extends HttpServlet{
	
	// 装要扫描的包
	private List<String> packageNames = new ArrayList<String>();
	// 装实例的容器 键：实例名 值：实例对象
	private Map<String, Object> instanceMaps = new HashMap<>();
	// 装方法的容器 键：路径 值：方法
	private Map<String, Object> handlerMaps = new HashMap<>();
	
	// 通过正则表达式将所有"." 转换为"/"
	private String replacePath(String path){
		return path.replaceAll("\\.", "/");
	}
	
	// 自动包扫描
	private void scanBase(String basePakcage) {
		//  basePakcage:  com.lzl
		//  url:   file:/D:/apache-tomcat-8.5.33-windows-x64/apache-tomcat-8.5.33/wtpwebapps/MySpringMVC/WEB-INF/classes/com/lzl/
		URL url = this.getClass().getClassLoader().getResource("/" + replacePath(basePakcage));
		String basePath = this.getServletContext().getRealPath("/" + replacePath(basePakcage));
		System.out.println(basePath);
		// 将URL转换为String
		//pathFile:  /D:/apache-tomcat-8.5.33-windows-x64/apache-tomcat-8.5.33/wtpwebapps/MySpringMVC/WEB-INF/classes/com/lzl/
		String pathFile = url.getFile();
		
		
		// 根据文件名得到文件夹或文件
		// file当前情况下是com/lzl这个文件夹，里面还有文件夹
		File file = new File(pathFile);
		
		// 得到该文件下的所有路径,也就是.class文件路径
		String[] files = file.list();
		
		for (String path : files) {
			// UserService.class
			// System.out.println(path);
			// 根据全路径拿到文件
			File eachFile = new File(pathFile + path);
			
			// 递归，将要扫描的包下的所有文件都装入packageNames中
			if (eachFile.isDirectory()) {
				scanBase(basePakcage + "." + eachFile.getName());
			} else {
				packageNames.add(basePakcage + "." + eachFile.getName());
				// com.lzl.controller.UserController.class
				// System.out.println(basePakcage + "." + eachFile.getName());
			}
		}
		System.out.println("自动扫描包后得到的东西：" + packageNames);
	}
	
	// 根据自动扫描包，创建实例对象，并装入instanceMaps中，相当于IOC容器
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void filterAndInstance() throws Exception {
		// System.out.println("扫描的包中有" + packageNames.size() + "个类");
		if (packageNames.size() <= 0) {
			return;
		}  
		
		for (String className : packageNames) {
			// 根据类名获得要创建的类的Class,注意原来的有.class后缀，所有要去掉，就相当于是全类名
			// System.out.println("装在packageNames中的class文件：" + className);
			Class instanceClass = Class.forName(className.replace(".class", ""));
			boolean isController = instanceClass.isAnnotationPresent(Controller.class);
			boolean isService = instanceClass.isAnnotationPresent(Service.class);
			
			// 如果该类上有注解Controller和Service注解，那么我们就创建类
			if (isController || isService) {
				// 创建对象
				Object instance = instanceClass.newInstance();
				// {com lzl controller UserController}
				String[] names = className.replace(".class", "").split("\\.");
				if (names.length > 1) {
					System.out.println(1111);
					// 拿到类名 是大驼峰写法
					String name = names[names.length-1];
					// 对类名做处理，改为小驼峰写法，也就是把第一给字母变成小写
					String instanceName = name.substring(0,1).toLowerCase() + name.substring(1);
					// System.out.println("instanceMaps中的键：" + instanceName);
					instanceMaps.put(instanceName, instance);
				}
				
			} 
		}
	}
	
	// 自动注入
	private void springDI() throws IllegalAccessException {
		// System.out.println("创建了" + instanceMaps.size() + "个实例");
		
		if (instanceMaps.size() <= 0) {
			return;
		}
		
		// 循环遍历instanceMaps
		for (Map.Entry<String, Object> entry : instanceMaps.entrySet()) {
			// userController=com.lzl.controller.UserController@290f6275
			// // System.out.println(entry);
			
			// 拿到类（Controller 或 Service）
			Class clazz = entry.getValue().getClass();
			// System.out.println(clazz.getName() + ":" + clazz.hashCode());
			
			// 得到该类的所有字段
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				// // System.out.println(field);
				if (field.isAnnotationPresent(AutoWired.class)) {
					AutoWired autoWired = field.getAnnotation(AutoWired.class);
					String fullName = field.getType().toString();
					// fullName: com.lzl.service.UserService --> userServiceImpl
					String[] names = fullName.split("\\.");
					String name = names[names.length - 1];
					String realName = name.substring(0, 1).toLowerCase() + name.substring(1) + "Impl";
					
					// 让私有属性能被访问
					field.setAccessible(true);
					// 自动注入：键为属性名（userService）值为在instanceMaps中根据自动注入取的名字得到的实例
					field.set(entry.getValue(), instanceMaps.get(realName));
					// System.out.println(instanceMaps.get(autoWiredValue).getClass().hashCode());
				}
			}
		}
	}
	
	// 处理方法
	private void handlerMapping() {
		if (instanceMaps.size() < 0) {
			return;
		}
		
		for (Map.Entry<String, Object> entry : instanceMaps.entrySet()) {
			Class clazz = entry.getValue().getClass();
			String baseUrl = "";
			if (clazz.isAnnotationPresent(Controller.class)) {
				Method[] methods = clazz.getDeclaredMethods();
				for (Method method : methods) {
					if (method.isAnnotationPresent(RequestMapping.class)) {
						RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
						// 得到方法上面的路径
						String methodUrl = requestMapping.value();
						if (clazz.isAnnotationPresent(RequestMapping.class)) {
							RequestMapping requestMappingOnController = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
							baseUrl = requestMappingOnController.value();
							handlerMaps.put("/" + baseUrl + "/" + methodUrl, method);
						} else {
							// 将路径和相对应的方法放入handlerMaps中
							handlerMaps.put("/" + methodUrl, method);
						}
					}
				}
			}
		}
		
	}
	
	// 初始化的时候应该扫描包，并创建对象
	@Override
	public void init() throws ServletException {
		super.init();
		scanBase("com.lzl");
		try {
			filterAndInstance();
			springDI();
			handlerMapping();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getServletPath();
		System.out.println("请求路径：" + path);
		//  /userController/selectUser
		
		String controllerUrl = path.split("/")[1];
		// 确定哪个对象来执行方法
		for (Entry<String, Object> entry : instanceMaps.entrySet()) {
			Class controller = entry.getValue().getClass();
			boolean isController = controller.isAnnotationPresent(Controller.class);
			boolean isRequestMapping = controller.isAnnotationPresent(RequestMapping.class);
			
			if (isController && isRequestMapping) {
				String controllerRQ= ((RequestMapping) controller.getAnnotation(RequestMapping.class)).value();
				if (controllerUrl.equals(controllerRQ)) {
					Object controller2 = entry.getValue();
					// 找方法
					Method controllerMethod = (Method) handlerMaps.get(path);
					
					try {
						controllerMethod.invoke(controller2, req, resp);
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
}
