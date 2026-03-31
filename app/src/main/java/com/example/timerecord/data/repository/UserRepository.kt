package com.example.timerecord.data.repository

import com.example.timerecord.dao.UserDao
import com.example.timerecord.entity.User

class UserRepository(
    private val userDao: UserDao
) {
    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun getUserById(id: String) = userDao.getUserById(id)

    suspend fun getUserByName(username: String) = userDao.getUserByName(username)

    suspend fun getAllUsers() = userDao.getAllUsers()

    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    suspend fun deleteUserById(id: String) = userDao.deleteUserById(id)
}