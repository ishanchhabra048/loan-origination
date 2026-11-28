
package com.ishanchhabra.loan_origination.controller;

import com.ishanchhabra.loan_origination.model.Customer;
import com.ishanchhabra.loan_origination.service.CustomerService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    // Constructor Dependency Injection
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // 1. Register a new customer
    @PostMapping
    public ResponseEntity<?> registerCustomer(
            @RequestBody Customer customer) throws SQLException {

        long customerId = customerService.registerCustomer(customer);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Customer registered successfully",
                        "customerId", customerId
                ));
    }

    // 2. Get customer by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(
            @PathVariable long id) throws SQLException {

        Customer customer = customerService.getCustomerById(id);

        if (customer == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(customer);
    }

    // 3. Search customers by name or mobile
    @GetMapping("/search")
    public ResponseEntity<?> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mobile
    ) throws SQLException {

        // Search by name
        if (name != null && mobile == null) {

            List<Customer> customers =
                    customerService.searchByName(name);

            return ResponseEntity.ok(customers);
        }

        // Search by mobile
        if (mobile != null && name == null) {

            Customer customer =
                    customerService.searchByMobile(mobile);

            if (customer == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(customer);
        }

        // Missing or multiple search parameters
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "message", "Provide either name or mobile"
                ));
    }
}
