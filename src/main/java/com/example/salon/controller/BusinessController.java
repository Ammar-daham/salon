package com.example.salon.controller;

import com.example.salon.model.Business;
import com.example.salon.service.BusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequestMapping("api/v1/business")
@RestController
public class BusinessController {
    private final BusinessService businessService;

    @Autowired
    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public Business addBusiness(@RequestBody Business business) {
        Business b = businessService.addBusiness(business);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(business.getId())
                .toUri();

        return ResponseEntity.created(location).body(b).getBody();
    }

    @GetMapping
    public List<Business> getAllBusiness() {
        return businessService.getAllBusiness();
    }

    @PutMapping("/{id}")
    public void updateBusiness(@PathVariable int id,  @RequestBody Business business) {
        businessService.updateBusinessById(id, business);
    }

    @DeleteMapping("/{id}")
    public void deleteBusiness(@PathVariable int id) {
        businessService.deleteBusiness(id);
    }
}
