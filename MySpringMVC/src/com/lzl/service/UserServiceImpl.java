package com.lzl.service;

import java.util.List;

import com.lzl.annotation.Service;
import com.lzl.pojo.User;

@Service
public class UserServiceImpl implements UserService{

	@Override
	public int insertUser(User user) {
		System.out.println("调用了UserServiceImpl的insertUser()方法");
		return 0;
	}

	@Override
	public int deleteUser(User user) {
		System.out.println("调用了UserServiceImpl的deleteUser()方法");
		return 0;
	}

	@Override
	public int updateUser(User user) {
		System.out.println("调用了UserServiceImpl的updateUser()方法");
		return 0;
	}

	@Override
	public List<User> selectUser() {
		System.out.println("调用了UserServiceImpl的selectUser()方法");
		return null;
	}

}
