package com.syndicati.controllers.user.user;

import com.syndicati.models.user.User;
import com.syndicati.services.user.user.UserService;

import java.util.List;
import java.util.Optional;

/**
 * User-focused controller for Java-side CRUD orchestration.
 */
public class UserController {

    private final UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    public List<User> users() {
        return userService.listUsers();
    }

    public Optional<User> userById(int idUser) {
        return userService.findById(idUser);
    }

    public Optional<User> findById(int idUser) {
        return userService.findById(idUser);
    }

    public Optional<User> userByEmail(String emailUser) {
        return userService.findByEmail(emailUser);
    }

    public Optional<User> findByEmail(String emailUser) {
        return userService.findByEmail(emailUser);
    }

    public List<User> searchByName(String query, int excludeUserId, int limit) {
        return userService.searchByName(query, excludeUserId, limit);
    }

    public int userAdd(User user) {
        return userService.createUser(user);
    }

    public boolean userEdit(User user) {
        return userService.updateUser(user);
    }

    public boolean userDelete(int idUser) {
        return userService.deleteUser(idUser);
    }
}
