package com.runway.auth.dao;
import com.runway.auth.model.User;
import com.runway.auth.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Repository
public class UserDao {

    private final UserRepository userRepository;

    public User getUserById(long userId){
        try{
            Optional<User> u = userRepository.findById(userId);
            if(u.isPresent()){
                return u.get();
            }
        }catch(Exception e){
            System.out.println("Unable to fetch user: "+ e.getMessage());
        }
        return null;
    }
    public User getUserByUsername(String username){
        try {
            Optional<User> u = userRepository.findByUsername(username);
            if (u.isPresent()) {
                return u.get();
            }
        }
        catch(Exception e){
            log.error("User not found.");
        }
        return null;
    }
    public User saveUser(User user){
        try{
            return userRepository.save(user);
        }
        catch(Exception e){
            System.out.println(e);
            log.error("Unable to save user");
        }
        return null;
    }

    public void deleteUser(User user) {
        try {
            user.getRoles().clear();
            userRepository.saveAndFlush(user);
            userRepository.delete(user);
            userRepository.flush();
        } catch (Exception e) {
            log.error("Unable to delete user: " + e.getMessage());
            throw new RuntimeException("Unable to delete user", e);
        }
    }

    public void deleteUserById(long userId) {
        try {
            userRepository.deleteById(userId);
        } catch (Exception e) {
            log.error("Unable to delete user by id: " + e.getMessage());
            throw new RuntimeException("Unable to delete user", e);
        }
    }
}

