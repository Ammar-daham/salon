package com.example.salon.dao;

import com.example.salon.model.Business;
import com.example.salon.model.Status;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;

public interface BusinessDao 
{
    enum SortBy { NAME, STATUS, CREATED_AT }

    /**
     * Which businesses to list. search matches the name, description or a city; status narrows to one
     * status. Without everyStatus, only APPROVED businesses are listed, plus ownBusinessId's.
     */
    record Filter(String search, Status status, boolean everyStatus, Long ownBusinessId) {}

    Long addBusiness(Business business);

    Page<Business> getBusinesses(Filter filter, Sort<SortBy> sort, PageQuery page);

    Business getBusinessById(long id);

    int updateBusinessById(long id, Business business);

    int updateBusinessStatus(long id, Status status);

    int deleteBusiness(long id);
}
