package com.ecommerce.address;

import com.ecommerce.address.dto.AddressRequest;
import com.ecommerce.address.dto.AddressResponse;
import com.ecommerce.address.model.Address;
import com.ecommerce.address.repository.AddressRepository;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.user.CurrentUserService;
import com.ecommerce.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<AddressResponse> findAll(String email) {
        User user = currentUserService.get(email);
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddressResponse create(String email, AddressRequest request) {
        User user = currentUserService.get(email);
        if (request.defaultAddress() || addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(user.getId()).isEmpty()) {
            clearDefault(user.getId());
        }
        Address address = new Address();
        address.setUser(user);
        apply(address, request);
        address.setDefaultAddress(request.defaultAddress() || addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(user.getId()).isEmpty());
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(String email, Long id, AddressRequest request) {
        User user = currentUserService.get(email);
        Address address = findOwned(id, user.getId());
        if (request.defaultAddress()) {
            clearDefault(user.getId());
        }
        apply(address, request);
        address.setDefaultAddress(request.defaultAddress());
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = currentUserService.get(email);
        addressRepository.delete(findOwned(id, user.getId()));
    }

    @Transactional
    public AddressResponse makeDefault(String email, Long id) {
        User user = currentUserService.get(email);
        Address address = findOwned(id, user.getId());
        clearDefault(user.getId());
        address.setDefaultAddress(true);
        return toResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public Address getOwnedEntity(Long id, Long userId) {
        return findOwned(id, userId);
    }

    private Address findOwned(Long id, Long userId) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address with ID " + id + " was not found."));
    }

    private void clearDefault(Long userId) {
        addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)
                .forEach(existing -> existing.setDefaultAddress(false));
    }

    private void apply(Address address, AddressRequest request) {
        address.setRecipientName(request.recipientName().trim());
        address.setLine1(request.line1().trim());
        address.setLine2(request.line2());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountry(request.country().trim());
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(address.getId(), address.getRecipientName(), address.getLine1(), address.getLine2(),
                address.getCity(), address.getState(), address.getPostalCode(), address.getCountry(), address.isDefaultAddress());
    }
}
