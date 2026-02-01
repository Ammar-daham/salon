package com.example.salon.controller;

import com.example.salon.model.SalonService;
import com.example.salon.service.BusinessServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("api/v1/business")
public class BusinessServiceController {

    private final BusinessServiceService businessServiceService;

    @Autowired
    public BusinessServiceController(BusinessServiceService businessServiceService) {
        this.businessServiceService = businessServiceService;
    }

    @PostMapping("/{businessId}/services")
    public SalonService createServiceForBusiness(
            @PathVariable Long businessId,
            @RequestBody SalonService salonService
    ) {
        SalonService s = businessServiceService
                .createServiceForBusiness(businessId, salonService);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(salonService.getId())
                .toUri();

        return ResponseEntity.created(location).body(s).getBody();
    }


}
