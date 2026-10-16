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

/**
 * A salon's customer records, under {@code /api/v1/businesses/{businessId}/customers}, and across salons under
 * {@code /api/v1/customers}.
 *
 * <p>Customer records are personal data, so only the salon's ADMIN and EMPLOYEE users and a SUPER_ADMIN may call
 * these; a CUSTOMER never may. Staff can list, read and add customers, but only an ADMIN or a SUPER_ADMIN can
 * change or remove one.
 */
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

	/**
	 * Adds a customer to a salon.
	 *
	 * <p>{@code POST /api/v1/businesses/{businessId}/customers}. ADMIN and EMPLOYEE of the salon, or a
	 * SUPER_ADMIN. The server fills in {@code id}, {@code business_id} and {@code created_at}. Nothing checks for
	 * a duplicate, so the same person can be added twice.
	 *
	 * <p>Errors: 400 if the body fails validation; 403 for another salon; 404 if the salon doesn't exist.
	 *
	 * @param businessId the salon
	 * @param request the name, email, phone, notes and marketing consent: see {@link CustomerRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the customer, and its address in {@code Location}
	 */
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

	/**
	 * Lists one salon's customers, a page at a time.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/customers}. ADMIN and EMPLOYEE of the salon, or a SUPER_ADMIN.
	 * Removed customers are left out. {@code ?q} keeps those whose name (first, then last), email or phone
	 * contains it, ignoring case. {@code ?sort} is {@code name} (the default: by last name, then first name) or
	 * {@code created_at}.
	 *
	 * <p>Errors: 400 for a bad {@code page}, {@code size} or {@code sort}; 403 for another salon; 404 if the salon
	 * doesn't exist.
	 *
	 * @param businessId the salon
	 * @param q text to search for, or nothing for every customer
	 * @param sort the order: a key, or the key with a leading {@code -} for descending
	 * @param page the page, counting from 1 (default 1)
	 * @param size customers per page, 1 to 100 (default 20)
	 * @param principal the signed-in caller
	 * @return 200 with a page of customers
	 */
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

	/**
	 * Lists customers across salons, a page at a time.
	 *
	 * <p>{@code GET /api/v1/customers}. A SUPER_ADMIN gets every salon's customers, or one salon's with
	 * {@code ?business_id}. An ADMIN or EMPLOYEE gets their own salon's, whether they name it or not, and naming
	 * another is refused. {@code ?q} and {@code ?sort} work as on a salon's own list, and {@code ?sort} can also
	 * be {@code business_name} (then by customer name).
	 *
	 * <p>Errors: 400 for a bad {@code business_id}, {@code page}, {@code size} or {@code sort}; 403 for a CUSTOMER,
	 * or for another salon's {@code business_id}; 404 if the {@code business_id} salon doesn't exist.
	 *
	 * @param businessId only this salon's customers, or nothing for every salon the caller may see
	 * @param q text to search for, or nothing for every customer
	 * @param sort the order: a key, or the key with a leading {@code -} for descending
	 * @param page the page, counting from 1 (default 1)
	 * @param size customers per page, 1 to 100 (default 20)
	 * @param principal the signed-in caller
	 * @return 200 with a page of customers
	 */
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

	/**
	 * Returns a customer by id alone, without naming the salon.
	 *
	 * <p>{@code GET /api/v1/customers/{customerId}}. ADMIN and EMPLOYEE of the customer's salon, or a SUPER_ADMIN.
	 * Another salon's customer is a 404 rather than a 403, so an id doesn't reveal whose client someone is.
	 *
	 * <p>Errors: 404 if there is no such customer, it was removed, or the caller may not see it.
	 *
	 * @param customerId the customer
	 * @param principal the signed-in caller
	 * @return 200 with the customer
	 */
	@GetMapping("/customers/{customerId}")
	public Customer getCustomer(@PathVariable long customerId, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return customerService.getCustomer(customerId, principal);
	}

	/**
	 * Returns one of a salon's customers.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/customers/{customerId}}. ADMIN and EMPLOYEE of the salon, or a
	 * SUPER_ADMIN.
	 *
	 * <p>Errors: 403 for another salon; 404 if the customer isn't a current customer of this salon.
	 *
	 * @param businessId the salon
	 * @param customerId the customer
	 * @param principal the signed-in caller
	 * @return 200 with the customer
	 */
	@GetMapping("/businesses/{businessId}/customers/{customerId}")
	public Customer getCustomerById(@PathVariable long businessId, @PathVariable long customerId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return customerService.getCustomerById(businessId, customerId, principal);
	}

	/**
	 * Replaces a customer's details.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/customers/{customerId}}. An ADMIN of the salon or a
	 * SUPER_ADMIN; an EMPLOYEE is refused. Every field is replaced: an email, phone or notes left out is cleared,
	 * and a missing {@code marketing_consent} means no. Answers 200 with no body.
	 *
	 * <p>Errors: 400 if the body fails validation; 403 for an EMPLOYEE or another salon; 404 if the customer isn't
	 * a current customer of this salon.
	 *
	 * @param businessId the salon
	 * @param customerId the customer
	 * @param request the name, email, phone, notes and marketing consent: see {@link CustomerRequest}
	 * @param principal the signed-in caller
	 */
	@PutMapping("/businesses/{businessId}/customers/{customerId}")
	public void updateCustomer(@PathVariable long businessId, @PathVariable long customerId,
			@Valid @RequestBody CustomerRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		customerService.updateCustomerById(businessId, customerId, request.toCustomer(), principal);
	}

	/**
	 * Removes a customer from a salon.
	 *
	 * <p>{@code DELETE /api/v1/businesses/{businessId}/customers/{customerId}}. An ADMIN of the salon or a
	 * SUPER_ADMIN; an EMPLOYEE is refused. The record is kept, so the customer's appointments still name them,
	 * but every customer read leaves it out and it can't be booked any more. Answers 200 with no body.
	 *
	 * <p>Errors: 403 for an EMPLOYEE or another salon; 404 if the customer isn't a current customer of this salon;
	 * 409 while they have an upcoming appointment (booked or confirmed, and not over yet).
	 *
	 * @param businessId the salon
	 * @param customerId the customer
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/businesses/{businessId}/customers/{customerId}")
	public void deleteCustomer(@PathVariable long businessId, @PathVariable long customerId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		customerService.deleteCustomerById(businessId, customerId, principal);
	}
}
