package org.service_b.workflow.customer.mapper;

import java.util.List;

import org.service_b.workflow.customer.dto.CustomerDto;
import org.service_b.workflow.customer.persistence.Customer;
import org.service_b.workflow.customer.dto.CreateCustomerForm;
import org.mapstruct.Mapper;

@Mapper
public interface CustomerMapper {

    Customer map(CreateCustomerForm createCustomerForm);

    CustomerDto map(Customer customer);

    List<CustomerDto> map(List<Customer> customers);

}
