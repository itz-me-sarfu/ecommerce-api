package com.ecommerce.address.repository;

import com.ecommerce.address.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}
