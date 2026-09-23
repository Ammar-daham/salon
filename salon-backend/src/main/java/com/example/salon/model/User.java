package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class User
{
	public Long id;
	@JsonProperty("first_name")
	public String firstName;
	@JsonProperty("last_name")
	public String lastName;
	public Role role;
	public String email;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	public String password;
	@JsonIgnore
	public String passwordHash;
	// A super admin must be able to set this when creating a user for a specific business, but it
	// should never be echoed back in a response - same treatment as the write-only password field.
	@JsonProperty(value = "business_id", access = JsonProperty.Access.WRITE_ONLY)
	public Long businessId;
	public List<Address> addresses;
	public List<Contact> contacts;
	@JsonProperty("created_at")
	public Instant createdAt;
	@JsonProperty("updated_at")
	public Instant updatedAt;

	public User()
	{
	}

	public User(Long id, String firstName, String lastName, Role role, Instant createdAt)
	{
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
		this.createdAt = createdAt;
	}

	public Long getId()
	{
		return id;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public Role getRole()
	{
		return role;
	}

	public String getEmail()
	{
		return email;
	}

	public String getPassword()
	{
		return password;
	}

	public String getPasswordHash()
	{
		return passwordHash;
	}

	public Long getBusinessId()
	{
		return businessId;
	}

	public List<Address> getAddresses()
	{
		return addresses;
	}

	public List<Contact> getContacts()
	{
		return contacts;
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

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public void setRole(Role role)
	{
		this.role = role;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public void setPasswordHash(String passwordHash)
	{
		this.passwordHash = passwordHash;
	}

	public void setBusinessId(Long businessId)
	{
		this.businessId = businessId;
	}

	public void setAddresses(List<Address> addresses)
	{
		this.addresses = addresses;
	}

	public void setContacts(List<Contact> contacts)
	{
		this.contacts = contacts;
	}

	public void setCreatedAt(Instant createdAt)
	{
		this.createdAt = createdAt;
	}

	public void setUpdatedAt(Instant updatedAt)
	{
		this.updatedAt = updatedAt;
	}
}
