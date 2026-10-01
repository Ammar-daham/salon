package com.example.salon.controller;

import com.example.salon.dto.CreateBusinessRequest;
import com.example.salon.dto.UpdateBusinessRequest;
import com.example.salon.model.Business;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

    @PostMapping
    public Business addBusiness(@Valid @RequestBody CreateBusinessRequest request)
    {
        Business business = request.toBusiness();
        Business b = businessService.addBusiness(business);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(business.getId())
                .toUri();

        return ResponseEntity.created(location).body(b).getBody();
    }

    @GetMapping
    public List<Business> getAllBusiness(@AuthenticationPrincipal AuthenticatedUser principal) 
    {
        return businessService.getAllBusiness(principal);
    }

    @GetMapping("/{id}")
    public Business getBusinessById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal) 
    {
        return businessService.getBusinessById(id, principal);
    }

    @PutMapping("/{id}")
    public void updateBusiness(@PathVariable int id, @Valid @RequestBody UpdateBusinessRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
    {
        businessService.updateBusinessById(id, request.toBusiness(), principal);
    }

    @DeleteMapping("/{id}")
    public void deleteBusiness(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
    {
        businessService.deleteBusiness(id, principal);
    }
}
