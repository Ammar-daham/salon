package com.example.salon.controller;

import com.example.salon.model.Address;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/addresses")
public class AddressController
{
	private final AddressService addressService;

	@Autowired
	public AddressController(AddressService addressService)
	{
		this.addressService = addressService;
	}

	@GetMapping
	public List<Address> getAllAddresses()
	{
		return addressService.getAllAddresses();
	}

	@GetMapping("/{id}")
	public Address getAddress(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return addressService.getAddressById(id, principal);
	}

	@PutMapping("/{id}")
	public void updateAddress(@PathVariable int id, @RequestBody Address address, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		addressService.updateAddressById(id, address, principal);
	}

	@DeleteMapping("/{id}")
	public void deleteAddress(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		addressService.deleteAddressById(id, principal);
	}


}
