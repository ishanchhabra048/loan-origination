
package com.ishanchhabra.loan_origination.service;

import com.ishanchhabra.loan_origination.dao.CustomerDAO;
import com.ishanchhabra.loan_origination.model.Customer;
import com.ishanchhabra.loan_origination.exception.DuplicateCustomerException;
import com.ishanchhabra.loan_origination.utility.BusinessClock;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerDAO customerDAO;
    private final BusinessClock businessClock;

    public CustomerService(
            CustomerDAO customerDAO,
            BusinessClock businessClock) {

        this.customerDAO = customerDAO;
        this.businessClock = businessClock;
    }

    // 1. Register a new customer
    public long registerCustomer(Customer customer)
            throws SQLException {

        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer details are required"
            );
        }

        // Validate customer name
        if (customer.getName() == null ||
                customer.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Customer name is required"
            );
        }

        // Validate mobile number
        if (customer.getMobile() == null ||
                !customer.getMobile().matches("[0-9]{10}")) {
            throw new IllegalArgumentException(
                    "Mobile number must contain exactly 10 digits"
            );
        }

        // Validate date of birth using BusinessClock
        if (customer.getDateOfBirth() == null ||
                customer.getDateOfBirth().isAfter(businessClock.today())) {
            throw new IllegalArgumentException(
                    "Invalid date of birth"
            );
        }

        // Validate address
        if (customer.getAddress() == null ||
                customer.getAddress().isBlank()) {
            throw new IllegalArgumentException(
                    "Customer address is required"
            );
        }

        // Validate KYC details
        if (customer.getKycDetails() == null ||
                customer.getKycDetails().isBlank()) {
            throw new IllegalArgumentException(
                    "KYC details are required"
            );
        }

        // Check duplicate mobile number
        Customer existingCustomer =
                customerDAO.findByMobile(customer.getMobile());

        if (existingCustomer != null) {
            throw new DuplicateCustomerException(
                    "Customer with this mobile number already exists"
            );
        }

        // Save customer in MySQL
        return customerDAO.save(customer);
    }

    // 2. Find customer by ID
    public Customer getCustomerById(long customerId)
            throws SQLException {

        if (customerId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid customer ID"
            );
        }

        return customerDAO.findById(customerId);
    }

    // 3. Search customers by name
    public List<Customer> searchByName(String name)
            throws SQLException {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer name is required for search"
            );
        }

        return customerDAO.findByName(name.trim());
    }

    // 4. Search customer by mobile number
    public Customer searchByMobile(String mobile)
            throws SQLException {

        if (mobile == null ||
                !mobile.matches("[0-9]{10}")) {
            throw new IllegalArgumentException(
                    "Mobile number must contain exactly 10 digits"
            );
        }

        return customerDAO.findByMobile(mobile);
    }
}
