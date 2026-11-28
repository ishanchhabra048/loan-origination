
package com.ishanchhabra.loan_origination.utility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class BusinessClock {

    private final String businessDate;

    public BusinessClock(
            @Value("${app.business-date:}") String businessDate) {

        this.businessDate = businessDate;
    }

    // Return business date
    public LocalDate today() {

        if (businessDate.isBlank()) {
            return LocalDate.now();
        }

        return LocalDate.parse(businessDate);
    }

    // Return business date with current time
    public LocalDateTime now() {

        return LocalDateTime.of(
                today(),
                LocalTime.now()
        );
    }
}
