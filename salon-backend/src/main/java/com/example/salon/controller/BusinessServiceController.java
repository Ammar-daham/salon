package com.example.salon.controller;

import com.example.salon.model.SalonService;
import com.example.salon.service.BusinessSalonServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("api/v1/business")
public class BusinessServiceController {

    private final BusinessSalonServiceService businessSalonServiceService;

    @Autowired
    public BusinessServiceController(BusinessSalonServiceService businessSalonServiceService) {
        this.businessSalonServiceService = businessSalonServiceService;
    }

    @PostMapping("/{businessId}/services")
    public SalonService createServiceForBusiness(
            @PathVariable Long businessId,
            @RequestBody SalonService salonService
    ) {
        SalonService s = businessSalonServiceService
                .createServiceForBusiness(businessId, salonService);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(salonService.getId())
                .toUri();

        return ResponseEntity.created(location).body(s).getBody();
    }

    @GetMapping("/{businessId}/services/{serviceId}")
    public SalonService getServiceForBusiness(@PathVariable int businessId, @PathVariable int serviceId) {
        return businessSalonServiceService.getServiceById(businessId, serviceId);
    }

    @PutMapping("/{businessId}/services/{serviceId}")
    public void updateServiceForBusiness(@PathVariable int serviceId,  @RequestBody SalonService salonService) {
        businessSalonServiceService.updateServiceForBusiness(serviceId, salonService);
    }

    @DeleteMapping("/{businessId}/services/{serviceId}")
    public void deleteServiceForBusiness(@PathVariable int id) {
        businessSalonServiceService.deleteServiceForBusiness(id);
    }
}
