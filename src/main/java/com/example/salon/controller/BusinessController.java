package com.example.salon.controller;

import com.example.salon.model.Business;
import com.example.salon.service.BusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("api/v1/business")
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
    public void addBusiness(@RequestBody Business business)
    {
        businessService.addBusiness(business);
    }

    @GetMapping
    public List<Business> getAllBusiness()
    {
       return businessService.getAllBusiness();
    }
}
