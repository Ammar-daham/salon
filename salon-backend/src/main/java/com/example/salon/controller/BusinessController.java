package com.example.salon.controller;

import com.example.salon.dao.BusinessDao;
import com.example.salon.dto.BusinessStatusRequest;
import com.example.salon.dto.CreateBusinessRequest;
import com.example.salon.dto.UpdateBusinessRequest;
import com.example.salon.model.Business;
import com.example.salon.model.Status;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Salons, under {@code /api/v1/businesses}.
 *
 * <p>Anyone signed in may read the salons they can see: ADMIN and SUPER_ADMIN see every salon, anyone
 * else the approved ones and their own. Only a SUPER_ADMIN creates a salon or changes its status; an
 * ADMIN edits and deletes only their own.
 */
@RequestMapping("api/v1/businesses")
@RestController
public class BusinessController 
{
    private final BusinessService businessService;

    @Autowired
    public BusinessController(BusinessService businessService) 
    {
        this.businessService = businessService;
    }

    /**
     * Creates a salon, with its addresses and contacts.
     *
     * <p>{@code POST /api/v1/businesses}. SUPER_ADMIN only. A new salon is {@code PENDING} until a
     * SUPER_ADMIN approves it; a {@code status} in the body is ignored. {@code currency} is {@code EUR}
     * and {@code timezone} {@code Europe/Berlin} unless sent. More addresses and contacts can be added
     * later with {@code POST /api/v1/businesses/{id}/addresses} and {@code .../contacts}, and each is
     * edited under {@code /api/v1/addresses} or {@code /api/v1/contacts}.
     *
     * <p>Errors: 400 if the body breaks the rules of {@link CreateBusinessRequest}; 409 if another salon
     * already has the name, or the body lists the same contact value twice.
     *
     * @param request the salon to create, as a {@link CreateBusinessRequest}
     * @return 201 Created with the salon as stored, and its address in {@code Location}
     */
    @PostMapping
    public ResponseEntity<Business> addBusiness(@Valid @RequestBody CreateBusinessRequest request)
    {
        Business business = request.toBusiness();
        Business b = businessService.addBusiness(business);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(b.getId())
                .toUri();

        return ResponseEntity.created(location).body(b);
    }

    /**
     * Lists the salons the caller may see, a page at a time.
     *
     * <p>{@code GET /api/v1/businesses}. Anyone signed in. ADMIN and SUPER_ADMIN see every salon; anyone
     * else sees the approved ones and their own. Each salon comes with its addresses, contacts and
     * services. {@code q} looks for the text, ignoring case, in the name, the description and the city of
     * any of the salon's addresses. {@code sort} is {@code name} (the default, ignoring case),
     * {@code status} or {@code created_at}.
     *
     * <p>Errors: 400 if {@code page}, {@code size}, {@code sort} or {@code status} isn't valid.
     *
     * @param q text to search for, or nothing for every salon
     * @param status only salons with this status: {@code PENDING}, {@code APPROVED}, {@code REJECTED} or
     *        {@code SUSPENDED}
     * @param sort the order, {@code name} if left out
     * @param page which page, counting from 1
     * @param size how many salons a page holds, 1 to 100, 20 if left out
     * @param principal the signed-in caller
     * @return 200 with one page of salons
     */
    @GetMapping
    public Page<Business> getBusinesses(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser principal)
    {
        return businessService.getBusinesses(q, status, Sort.parse(sort, BusinessDao.SortBy.class),
                PageQuery.of(page, size), principal);
    }

    /**
     * Returns one salon, with its addresses, contacts and services.
     *
     * <p>{@code GET /api/v1/businesses/{id}}. Anyone signed in who may see the salon: ADMIN and
     * SUPER_ADMIN see every salon, anyone else an approved one or their own.
     *
     * <p>Errors: 404 if there's no such salon, or the caller may not see it.
     *
     * @param id the salon's id
     * @param principal the signed-in caller
     * @return 200 with the salon
     */
    @GetMapping("/{id}")
    public Business getBusinessById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal) 
    {
        return businessService.getBusinessById(id, principal);
    }

    /**
     * Replaces a salon's details.
     *
     * <p>{@code PUT /api/v1/businesses/{id}}. An ADMIN of the salon, or a SUPER_ADMIN. Every field is
     * replaced: {@code name}, {@code image}, {@code currency} and {@code timezone} are required, and leaving
     * {@code description} out clears it. A {@code status}, addresses and contacts in the body are ignored:
     * the status changes with {@code PUT /api/v1/businesses/{id}/status}, and addresses and contacts are
     * added with {@code POST /api/v1/businesses/{id}/addresses} and {@code .../contacts}. A new
     * {@code timezone} keeps time off and appointments at the same moments, so they read differently on the
     * new clock, while opening hours and shifts keep their wall-clock times.
     *
     * <p>Errors: 400 if the body breaks the rules of {@link UpdateBusinessRequest}; 403 for another salon;
     * 404 if there's no such salon; 409 if another salon already has the name.
     *
     * @param id the salon's id
     * @param request the new details, as an {@link UpdateBusinessRequest}
     * @param principal the signed-in caller
     * @return 200 with the salon as stored
     */
    @PutMapping("/{id}")
    public Business updateBusiness(@PathVariable int id, @Valid @RequestBody UpdateBusinessRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
    {
        return businessService.updateBusinessById(id, request.toBusiness(), principal);
    }

    /**
     * Moves a salon to another status: approves, rejects or suspends it, or puts it back to pending.
     *
     * <p>{@code PUT /api/v1/businesses/{id}/status}. SUPER_ADMIN only. Any status may follow any other, and
     * asking for the status it already has changes nothing. A salon that isn't {@code APPROVED} is hidden
     * from everyone but ADMIN and SUPER_ADMIN users and the salon's own users.
     *
     * <p>Errors: 400 if {@code status} is missing or isn't one of the four {@link Status} names; 403 for
     * anyone but a SUPER_ADMIN; 404 if there's no such salon.
     *
     * @param id the salon's id
     * @param request the new {@code status}: see {@link BusinessStatusRequest}
     * @param principal the signed-in caller
     * @return 200 with the salon as stored
     */
    @PutMapping("/{id}/status")
    public Business changeStatus(@PathVariable int id, @Valid @RequestBody BusinessStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal)
    {
        return businessService.changeStatus(id, request.status(), principal);
    }

    /**
     * Deletes a salon and everything that belongs to it.
     *
     * <p>{@code DELETE /api/v1/businesses/{id}}. An ADMIN of the salon, or a SUPER_ADMIN. A hard delete:
     * it takes the salon's addresses, contacts, services, opening hours, staff records, customers and
     * appointments with it. It's refused while any user account still belongs to the salon (its
     * {@code business_id} is the salon), so delete those users or move them first. An ADMIN belongs to
     * their salon, so in practice only a SUPER_ADMIN can delete one. Answers 204.
     *
     * <p>Errors: 403 for another salon; 404 if there's no such salon; 409 while users still belong to it.
     *
     * @param id the salon's id
     * @param principal the signed-in caller
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBusiness(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
    {
        businessService.deleteBusiness(id, principal);
    }
}
