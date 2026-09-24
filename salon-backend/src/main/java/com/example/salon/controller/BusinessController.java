package com.example.salon.controller;

import com.example.salon.model.Business;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.BusinessService;
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
    public Business addBusiness(@RequestBody Business business) 
    {
        Business b = businessService.addBusiness(business);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(business.getId())
                .toUri();

        return ResponseEntity.created(location).body(b).getBody();
    }

    @GetMapping
    public List<Business> getAllBusiness() 
    {
        return businessService.getAllBusiness();
    }

    @GetMapping("/{id}")
    public Business getBusinessById(@PathVariable int id) 
    {
        return businessService.getBusinessById(id);
    }

    @PutMapping("/{id}")
    public void updateBusiness(@PathVariable int id, @RequestBody Business business, @AuthenticationPrincipal AuthenticatedUser principal) 
    {
        businessService.updateBusinessById(id, business, principal);
    }

    @DeleteMapping("/{id}")
    public void deleteBusiness(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
    {
        businessService.deleteBusiness(id, principal);
    }
}
