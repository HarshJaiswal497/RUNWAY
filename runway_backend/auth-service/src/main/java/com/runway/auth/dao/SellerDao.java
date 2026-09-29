package com.runway.auth.dao;

import com.runway.auth.model.Seller;
import com.runway.auth.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class SellerDao {

    private final SellerRepository sellerRepository;

    public boolean checkIfSellerExists(String username){
        try{
            Optional<Seller> s = sellerRepository.findByUsername(username);
            if(s.isPresent()){
                Seller seller = s.get();
                System.out.println(seller);
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
    public Seller saveSeller(Seller seller){
        try{
            sellerRepository.save(seller);
            return seller;
        }
        catch(Exception e){
            System.out.println("Exception: "+e.getMessage());
        }
        return null;
    }

    public void deleteByUserId(long userId) {
        try {
            if (sellerRepository.existsById(userId)) {
                sellerRepository.deleteById(userId);
                sellerRepository.flush();
            }
        } catch (Exception e) {
            System.out.println("Unable to delete seller: " + e.getMessage());
        }
    }
}

