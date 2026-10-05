package com.example.salon.controller;

import com.example.salon.dto.CustomerRequest;
import com.example.salon.model.Customer;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("api/v1/businesses")
public class CustomerController
{
	private final CustomerService customerService;

	@Autowired
	public CustomerController(CustomerService customerService)
	{
		this.customerService = customerService;
	}

	@PostMapping("/{businessId}/customers")
	public ResponseEntity<Customer> addCustomer(@PathVariable long businessId,
			@Valid @RequestBody CustomerRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		Customer customer = customerService.addCustomer(businessId, request.toCustomer(), principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(customer.getId())
				.toUri();

		return ResponseEntity.created(location).body(customer);
	}

	@GetMapping("/{businessId}/customers")
	public List<Customer> getCustomersForBusiness(@PathVariable long businessId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return customerService.getCustomersForBusiness(businessId, principal);
	}

	@GetMapping("/{businessId}/customers/{customerId}")
	public Customer getCustomerById(@PathVariable long businessId, @PathVariable long customerId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return customerService.getCustomerById(businessId, customerId, principal);
	}

	@PutMapping("/{businessId}/customers/{customerId}")
	public void updateCustomer(@PathVariable long businessId, @PathVariable long customerId,
			@Valid @RequestBody CustomerRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		customerService.updateCustomerById(businessId, customerId, request.toCustomer(), principal);
	}

	@DeleteMapping("/{businessId}/customers/{customerId}")
	public void deleteCustomer(@PathVariable long businessId, @PathVariable long customerId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		customerService.deleteCustomerById(businessId, customerId, principal);
	}
}
