
package com.ishanchhabra.loan_origination.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Customer {

    private Long customerId;
    private String name;
    private LocalDate dateOfBirth;
    private String mobile;
    private String address;
    private String kycDetails;
    private LocalDateTime createdAt;

    public Customer(String name,
                    LocalDate dateOfBirth,
                    String mobile,
                    String address,
                    String kycDetails) {

        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.mobile = mobile;
        this.address = address;
        this.kycDetails = kycDetails;
    }
}
