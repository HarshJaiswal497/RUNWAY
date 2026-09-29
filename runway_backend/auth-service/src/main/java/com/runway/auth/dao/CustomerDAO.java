package com.runway.auth.dao;

import com.runway.auth.model.Customer;
import com.runway.auth.repository.CustomerRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class CustomerDAO {
    private final CustomerRepository customerRepository;

    public boolean saveCustomer(Customer customer){
        try{
            customerRepository.save(customer);
            return true;
        }
        catch(Exception e){
            System.out.println("Unable to save customer: "+e);
            return false;
        }
    }

    public boolean checkIfCustomerExists(String username) {
        try {
            return customerRepository.findByUsername(username).isPresent();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return false;
    }

    public void deleteByUserId(long userId) {
        try {
            if (customerRepository.existsById(userId)) {
                customerRepository.deleteById(userId);
                customerRepository.flush();
            }
        } catch (Exception e) {
            System.out.println("Unable to delete customer: " + e.getMessage());
        }
    }
}

