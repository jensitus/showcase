package org.service_b.workflow.customer.controller;

import java.util.List;
import java.util.UUID;

import org.service_b.workflow.customer.dto.CustomerDto;
import org.service_b.workflow.customer.service.CustomerService;
import org.service_b.workflow.customer.dto.CreateCustomerForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@RequestBody CreateCustomerForm createCustomerForm) {
        return ResponseEntity.ok(customerService.saveCustomer(createCustomerForm));
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getCustomers() {
        return ResponseEntity.ok(customerService.getCustomers());
    }

    @GetMapping("{id}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable("id")UUID id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

}
