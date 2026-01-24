package com.example.salon.model;

public class Staff
{
    public long id;
    public String title;
    public boolean isActive;
    public long userId;
    public long businessId;

    public Staff(long id, String title, boolean isActive, long userId, long businessId)
    {
        this.id = id;
    }

    public long getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public long getUserId()
    {
        return userId;
    }

    public long getBusinessId()
    {
        return businessId;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setActive(boolean active)
    {
        isActive = active;
    }

    public void setUserId(long userId)
    {
        this.userId = userId;
    }

    public void setBusinessId(long businessId)
    {
        this.businessId = businessId;
    }
}
