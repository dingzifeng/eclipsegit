package com.lzl.service;

import java.util.List;

import com.lzl.pojo.User;

public interface UserService {
	int insertUser(User user);
	int deleteUser(User user);
	int updateUser(User user);
	List<User> selectUser();
}
