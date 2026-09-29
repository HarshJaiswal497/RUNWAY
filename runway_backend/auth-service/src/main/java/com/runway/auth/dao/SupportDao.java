package com.runway.auth.dao;

import com.runway.auth.model.Support;
import com.runway.auth.repository.SupportRepository;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class SupportDao {

    private final SupportRepository supportRepository;

    public boolean checkIfSupportExists(String username){
        try{
            Optional<Support> s = supportRepository.findByUsername(username);
            if(s.isPresent()){
                Support support = s.get();
                System.out.println(support);
                return true;
            }
            else{
                return false;
            }
        }
        catch(Exception e){
            System.out.println("Exception: "+e.getMessage());
        }
        return false;
    }
    public Support saveSupport(Support support){
        try{
            supportRepository.save(support);
            return support;
        }
        catch(Exception e){
            System.out.println("Exception: "+e.getMessage());
        }
        return null;
    }

    public void deleteByUserId(long userId) {
        try {
            if (supportRepository.existsById(userId)) {
                supportRepository.deleteById(userId);
                supportRepository.flush();
            }
        } catch (Exception e) {
            System.out.println("Unable to delete support: " + e.getMessage());
        }
    }
}

