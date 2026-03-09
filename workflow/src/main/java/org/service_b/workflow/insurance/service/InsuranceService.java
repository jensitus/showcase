package org.service_b.workflow.insurance.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.service_b.workflow.customer.persistence.Customer;
import org.service_b.workflow.customer.persistence.CustomerRepository;
import org.service_b.workflow.insurance.dto.RequestedContractDto;
import org.service_b.workflow.insurance.persistence.Insurance;
import org.service_b.workflow.insurance.persistence.InsuranceRepository;
import org.service_b.workflow.insurance.persistence.InsuranceType;
import org.service_b.workflow.insurance.persistence.InsuranceState;
import org.service_b.workflow.sse.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InsuranceService {

    private final InsuranceRepository insuranceRepository;
    private final CustomerRepository customerRepository;
    private final EventService eventService;

    public Insurance saveInsurance(UUID customerId,
                                   String insuranceType,
                                   String insuranceCoverage,
                                   String insuranceSum,
                                   String paymentSchedule,
                                   Boolean floodRisk,
                                   Boolean mudslideRisk,
                                   int amount,
                                   InsuranceState insuranceState) {
        final Customer customer = customerRepository.findById(customerId).orElseThrow();
        Insurance insurance = new Insurance();
        insurance.setInsuranceType(InsuranceType.valueOf(insuranceType));
        insurance.setInsuranceNumber(generateInsuranceNumber());
        insurance.setName(insuranceType.toLowerCase() + "-" + insurance.getInsuranceNumber() + "-" + customer.getCustomerNumber());
        insurance.setSimpleName("simple");
        insurance.setCreatedAt(LocalDateTime.now());
        insurance.setInsuranceCoverage(insuranceCoverage);
        insurance.setInsuranceSum(insuranceSum);
        insurance.setPaymentSchedule(paymentSchedule);
        insurance.setFloodRisk(floodRisk);
        insurance.setMudslideRisk(mudslideRisk);
        insurance.setAmount(amount);
        insurance.setState(insuranceState);
        final Insurance saved = insuranceRepository.save(insurance);
        customer.getInsurances().add(insurance);
        customerRepository.save(customer);
        eventService.sendEvent(saved, "insurance");
        return saved;
    }

    public void updateInsurance(UUID insuranceId, Integer monthlyIncome) {
        final Insurance insurance = insuranceRepository.findById(insuranceId).orElseThrow();
        insurance.setMonthlyIncome(monthlyIncome);
        insuranceRepository.save(insurance);
    }

    public void updateInsurance(UUID insuranceId, String furtherInformation) {
        final Insurance insurance = insuranceRepository.findById(insuranceId).orElseThrow();
        insurance.setFurtherInformation(furtherInformation);
        insuranceRepository.save(insurance);
    }

    public void updateInsurance(UUID insuranceId, InsuranceState insuranceState) {
        final Insurance insurance = insuranceRepository.findById(insuranceId).orElseThrow();
        insurance.setState(insuranceState);
        insuranceRepository.save(insurance);
        eventService.sendEvent(insurance, "insurance");
    }

//    public RequestedContractDto getRequestedContractsDto(UUID aggregateId) {
//        final RequestedContractDto requestedContracts = insuranceRepository.getRequestedContracts(aggregateId.toString());
//        return requestedContracts;
//    }

    private int generateInsuranceNumber() {
        final long insuranceCount = insuranceRepository.count();
        return 11111 + Long.valueOf(insuranceCount + 1).intValue();
    }

}
