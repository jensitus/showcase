package org.service_b.workflow.customer.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.service_b.workflow.insurance.persistence.Insurance;
import org.service_b.workflow.shared.entity.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "CUSTOMER")
public class Customer {

    @Id
    @Column(name = "ID", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "GENDER")
    Gender gender;

    @Column(name = "FIRSTNAME")
    String firstname;

    @Column(name = "LASTNAME")
    String lastname;

    @Column(name = "DATE_OF_BIRTH")
    LocalDate dateOfBirth;

    @Column(name = "PHONE_NUMBER")
    String phoneNumber;

    @Column(name = "EMAIL_ADDRESS")
    String email;

    @Column(name = "street")
    String street;

    @Column(name = "zip_code")
    String zipCode;

    @Column(name = "city")
    String city;

    @Column(name = "country")
    String country;

    @Column(name = "customer_number")
    Integer customerNumber;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "CUSTOMER_INSURANCE",
               joinColumns = @JoinColumn(name = "CUSTOMER_ID"),
               inverseJoinColumns = @JoinColumn(name = "INSURANCE_ID"))
    Set<Insurance> insurances;


    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;


    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(final String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(final String lastname) {
        this.lastname = lastname;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(final String street) {
        this.street = street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(final String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public Integer getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(final Integer customerNumber) {
        this.customerNumber = customerNumber;
    }

    public Set<Insurance> getInsurances() {
        return insurances;
    }

    public void setInsurances(final Set<Insurance> insurances) {
        this.insurances = insurances;
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
}
