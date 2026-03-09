package org.service_b.workflow.customer.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.service_b.workflow.customer.dto.CustomerDto;
import org.service_b.workflow.customer.persistence.Customer;
import org.service_b.workflow.customer.dto.CreateCustomerForm;
import org.service_b.workflow.customer.mapper.CustomerMapper;
import org.service_b.workflow.customer.persistence.CustomerRepository;
// import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final int CUSTOMER_NUMBER_START = 54321;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    // private final ZeebeClient zeebeClient;

    public CustomerDto saveCustomer(CreateCustomerForm createCustomerForm) {
        Customer customer = customerMapper.map(createCustomerForm);
        customer.setCustomerNumber(generateCustomerNumber());
        customer.setCreatedAt(LocalDateTime.now());
        return customerMapper.map(customerRepository.save(customer));
    }

    public List<CustomerDto> getCustomers() {
        return customerMapper.map(customerRepository.findAll());
    }

    @Transactional
    public CustomerDto getCustomer(UUID id) {
        final Customer customer = customerRepository.findById(id)
                                                    .orElseThrow(null);
        return customerMapper.map(customer);
    }

    private int generateCustomerNumber() {
        final long customersCount = customerRepository.count();
        return CUSTOMER_NUMBER_START + Long.valueOf(customersCount).intValue();
    }

}
