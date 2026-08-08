package com.ecommerce.address;

import com.ecommerce.address.dto.AddressRequest;
import com.ecommerce.address.dto.AddressResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressResponse> findAll(@AuthenticationPrincipal UserDetails user) {
        return addressService.findAll(user.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@AuthenticationPrincipal UserDetails user, @Valid @RequestBody AddressRequest request) {
        return addressService.create(user.getUsername(), request);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.update(user.getUsername(), id, request);
    }

    @PostMapping("/{id}/default")
    public AddressResponse makeDefault(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return addressService.makeDefault(user.getUsername(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        addressService.delete(user.getUsername(), id);
    }
}
