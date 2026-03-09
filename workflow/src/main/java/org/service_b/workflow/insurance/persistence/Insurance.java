package org.service_b.workflow.insurance.persistence;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.service_b.workflow.customer.persistence.Customer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import org.service_b.workflow.workflow.domain.ResponsibleForType;
import org.service_b.workflow.workflow.persistence.entity.Processable;

@Entity
@Table(name = "INSURANCE")
public class Insurance implements Processable {

    @Id
    @Column(name = "ID", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "NAME")
    String name;

    @Column(name = "SIMPLE_NAME")
    String simpleName;

    @Column(name = "INSURANCE_TYPE")
    InsuranceType insuranceType;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "insurances")
    Set<Customer> customers;

    @Column(name = "insurance_number")
    Integer insuranceNumber;

    @Column(name = "insurance_coverage")
    String insuranceCoverage;

    @Column(name = "insurance_sum")
    String insuranceSum;

    @Column(name = "payment_schedule")
    String paymentSchedule;

    @Column(name = "amount")
    Integer amount;

    @Column(name = "state")
    InsuranceState insuranceState;

    @Getter
    @Column(name = "further_information")
    private String furtherInformation;

    @Getter
    @Column(name = "monthly_income")
    private Integer monthlyIncome;

    @Getter
    @Column(name = "mudslide_risk")
    private boolean mudslideRisk = false;

    @Getter
    @Column(name = "flood_risk")
    private boolean floodRisk = false;

    @Override
    public ResponsibleForType getType() {
        return ResponsibleForType.INSURANCE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(final String simpleName) {
        this.simpleName = simpleName;
    }

    public InsuranceType getInsuranceType() {
        return insuranceType;
    }

    public void setInsuranceType(final InsuranceType insuranceType) {
        this.insuranceType = insuranceType;
    }

    public Set<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(final Set<Customer> customers) {
        this.customers = customers;
    }

    public Integer getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(final Integer insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getInsuranceCoverage() {
        return insuranceCoverage;
    }

    public void setInsuranceCoverage(final String insuranceCoverage) {
        this.insuranceCoverage = insuranceCoverage;
    }

    public String getInsuranceSum() {
        return insuranceSum;
    }

    public void setInsuranceSum(final String insuranceSum) {
        this.insuranceSum = insuranceSum;
    }

    public String getPaymentSchedule() {
        return paymentSchedule;
    }

    public void setPaymentSchedule(final String paymentSchedule) {
        this.paymentSchedule = paymentSchedule;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(final Integer amount) {
        this.amount = amount;
    }

    public InsuranceState getState() {
        return insuranceState;
    }

    public void setState(final InsuranceState insuranceState) {
        this.insuranceState = insuranceState;
    }

    public void setFurtherInformation(final String furtherInformation) {
        this.furtherInformation = furtherInformation;
    }

    public void setMonthlyIncome(final Integer monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public void setMudslideRisk(final boolean mudslideRisk) {
        this.mudslideRisk = mudslideRisk;
    }

    public void setFloodRisk(final boolean floodRisk) {
        this.floodRisk = floodRisk;
    }
}
