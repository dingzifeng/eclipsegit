package com.lzl.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lzl.annotation.AutoWired;
import com.lzl.annotation.Controller;
import com.lzl.annotation.RequestMapping;
import com.lzl.pojo.User;
import com.lzl.service.UserService;

@Controller
@RequestMapping("userController")
public class UserController {
	
	@AutoWired
	private UserService userService;
	
	User user = new User(1, "张三", "xxx");
	
	@RequestMapping("insertUser")
	public int insertUser(HttpServletRequest request, HttpServletResponse response) {
		System.out.println(11);
		System.out.println(userService);
		return userService.insertUser(user);
	}
	
	@RequestMapping("deleteUser")
	public int deleteUser(HttpServletRequest request, HttpServletResponse response) {
		return userService.insertUser(user);
	}
	
	@RequestMapping("updateUser")
	public int updateUser(HttpServletRequest request, HttpServletResponse response) {
		return userService.updateUser(user);
	}
	@RequestMapping("selectUser")
	public List<User> selectUser(HttpServletRequest request, HttpServletResponse response) {
		return userService.selectUser();
	}
}
