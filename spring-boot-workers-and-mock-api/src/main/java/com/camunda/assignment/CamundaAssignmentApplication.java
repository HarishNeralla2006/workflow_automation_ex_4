package com.camunda.assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CamundaAssignmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamundaAssignmentApplication.class, args);
        System.out.println("==================================================================");
        System.out.println("Camunda 8 Labs 1-3 Workers & Mock REST API Started Successfully!");
        System.out.println("Mock REST Endpoints:");
        System.out.println(" - Lab 1 Credit API:     POST http://localhost:8080/api/credit-assessment");
        System.out.println(" - Lab 2 Payment API:    POST http://localhost:8080/api/payment/charge");
        System.out.println(" - Lab 3 Scheduling API: POST http://localhost:8080/api/appointments/check-slots");
        System.out.println("==================================================================");
    }
}

