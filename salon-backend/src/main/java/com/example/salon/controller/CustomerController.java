package com.example.salon.controller;

import com.example.salon.dao.CustomerDao;
import com.example.salon.dto.CustomerRequest;
import com.example.salon.model.Customer;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("api/v1")
public class CustomerController
{
	private final CustomerService customerService;

	@Autowired
	public CustomerController(CustomerService customerService)
	{
		this.customerService = customerService;
	}

	@PostMapping("/businesses/{businessId}/customers")
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

	/** A page of one salon's customers (BE-15). q searches the name, email and phone; sort is name or created_at. */
	@GetMapping("/businesses/{businessId}/customers")
	public Page<Customer> getCustomersForBusiness(@PathVariable long businessId,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "name") String sort,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return customerService.getCustomersForBusiness(businessId, q, Sort.parse(sort, CustomerDao.SortBy.class),
				PageQuery.of(page, size), principal);
	}

	/** A page of customers across salons, or of one with business_id; sort can also be business_name. */
	@GetMapping("/customers")
	public Page<Customer> getCustomers(
			@RequestParam(name = "business_id", required = false) Long businessId,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "name") String sort,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return customerService.getCustomers(businessId, q, Sort.parse(sort, CustomerDao.SortBy.class),
				PageQuery.of(page, size), principal);
	}

	@GetMapping("/customers/{customerId}")
	public Customer getCustomer(@PathVariable long customerId, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return customerService.getCustomer(customerId, principal);
	}

	@GetMapping("/businesses/{businessId}/customers/{customerId}")
	public Customer getCustomerById(@PathVariable long businessId, @PathVariable long customerId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return customerService.getCustomerById(businessId, customerId, principal);
	}

	@PutMapping("/businesses/{businessId}/customers/{customerId}")
	public void updateCustomer(@PathVariable long businessId, @PathVariable long customerId,
			@Valid @RequestBody CustomerRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		customerService.updateCustomerById(businessId, customerId, request.toCustomer(), principal);
	}

	@DeleteMapping("/businesses/{businessId}/customers/{customerId}")
	public void deleteCustomer(@PathVariable long businessId, @PathVariable long customerId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		customerService.deleteCustomerById(businessId, customerId, principal);
	}
}
