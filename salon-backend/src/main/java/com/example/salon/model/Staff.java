package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;

public class Staff
{
    public Long id;
    public String title;
    public boolean active;
    @JsonProperty("user_id")
    public Long userId;
    @JsonIgnore
    public Long businessId;
    // Who the staff member is, read from their user account: the roster is shown without loading every user.
    @JsonProperty("first_name")
    public String firstName;
    @JsonProperty("last_name")
    public String lastName;
    // Only for the salon's admins and super admins, as on /users; null for anyone else.
    public String email;
    @JsonProperty("hired_at")
    public LocalDate hiredAt;
    @JsonProperty("calendar_colour")
    public String calendarColour;
    @JsonProperty("created_at")
    public Instant createdAt;
    @JsonProperty("updated_at")
    public Instant updatedAt;

    public Staff(
            @JsonProperty("id") Long id,
            @JsonProperty("title") String title,
            @JsonProperty("is_active") boolean active,
            @JsonProperty("user_id") Long userId,
            @JsonProperty("hired_at") LocalDate hiredAt,
            @JsonProperty("calendar_colour") String calendarColour,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt)
    {
        this.id = id;
        this.title = title;
        this.active = active;
        this.userId = userId;
        this.hiredAt = hiredAt;
        this.calendarColour = calendarColour;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public boolean isActive()
    {
        return active;
    }

    public Long getUserId()
    {
        return userId;
    }

    public Long getBusinessId()
    {
        return businessId;
    }

    public LocalDate getHiredAt()
    {
        return hiredAt;
    }

    public String getCalendarColour()
    {
        return calendarColour;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public void setBusinessId(Long businessId)
    {
        this.businessId = businessId;
    }

    public void setHiredAt(LocalDate hiredAt)
    {
        this.hiredAt = hiredAt;
    }

    public void setCalendarColour(String calendarColour)
    {
        this.calendarColour = calendarColour;
    }

    public void setCreatedAt(Instant createdAt)
    {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
