package org.service_b.workflow.insurance.persistence;

import java.util.UUID;

import org.service_b.workflow.insurance.dto.RequestedContractDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InsuranceRepository extends JpaRepository<Insurance, UUID> {

//    @Query(name = "getInsuranceDto",
//            value = """
//                    select new at.phactum.demo.insurance.dto.RequestedContractDto(i.paymentSchedule, i.amount, i.insuranceSum, i.insuranceCoverage, i.insuranceNumber, i.name)
//                    from Insurance i, InsuranceAggregate ia where ia.insuranceId = i.id and ia.id = :aggregateId
//                    """
//    )
    // public RequestedContractDto getRequestedContracts(String aggregateId);

}
