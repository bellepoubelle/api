package fr.bellepoubelle.api.model;

import javax.persistence.Entity;
import java.io.Serializable;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Version;
import java.util.Date;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class Account implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, nullable = false)
	private Long id;
	@Version
	@Column(name = "version")
	private int version;

	@Column(length = 120, nullable = false)
	private String email;

	@Column(length = 120)
	private String newEmail;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private boolean activated;

	@Column
	@Temporal(TemporalType.DATE)
	private Date registeredSince;

	@Column
	private String uniqueToken;

	@Column
	@Temporal(TemporalType.DATE)
	private Date tokenAge;

	@Column(length = 120)
	private String firstName;

	@Column(length = 120)
	private String lastName;

	@Column(length = 20)
	private String phone;

	@Column(length = 200)
	private String address;

	@Column
	private Long image;

	@Column(length = 20, nullable = false)
	private String role;

	@Column(nullable = false)
	private boolean enabled;

	@Column(nullable = false, updatable = false)
	private Long operator;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public int getVersion() {
		return this.version;
	}

	public void setVersion(final int version) {
		this.version = version;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Account)) {
			return false;
		}
		Account other = (Account) obj;
		if (id != null) {
			if (!id.equals(other.id)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getNewEmail() {
		return newEmail;
	}

	public void setNewEmail(String newEmail) {
		this.newEmail = newEmail;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	public Date getRegisteredSince() {
		return registeredSince;
	}

	public void setRegisteredSince(Date registeredSince) {
		this.registeredSince = registeredSince;
	}

	public String getUniqueToken() {
		return uniqueToken;
	}

	public void setUniqueToken(String uniqueToken) {
		this.uniqueToken = uniqueToken;
	}

	public Date getTokenAge() {
		return tokenAge;
	}

	public void setTokenAge(Date tokenAge) {
		this.tokenAge = tokenAge;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Long getImage() {
		return image;
	}

	public void setImage(Long image) {
		this.image = image;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Long getOperator() {
		return operator;
	}

	public void setOperator(Long operator) {
		this.operator = operator;
	}

	@Override
	public String toString() {
		String result = getClass().getSimpleName() + " ";
		if (id != null)
			result += "id: " + id;
		result += ", version: " + version;
		if (email != null && !email.trim().isEmpty())
			result += ", email: " + email;
		if (newEmail != null && !newEmail.trim().isEmpty())
			result += ", newEmail: " + newEmail;
		if (password != null && !password.trim().isEmpty())
			result += ", password: " + password;
		result += ", activated: " + activated;
		if (registeredSince != null)
			result += ", registeredSince: " + registeredSince;
		if (uniqueToken != null && !uniqueToken.trim().isEmpty())
			result += ", uniqueToken: " + uniqueToken;
		if (tokenAge != null)
			result += ", tokenAge: " + tokenAge;
		if (firstName != null && !firstName.trim().isEmpty())
			result += ", firstName: " + firstName;
		if (lastName != null && !lastName.trim().isEmpty())
			result += ", lastName: " + lastName;
		if (phone != null && !phone.trim().isEmpty())
			result += ", phone: " + phone;
		if (address != null && !address.trim().isEmpty())
			result += ", address: " + address;
		if (image != null)
			result += ", image: " + image;
		if (role != null && !role.trim().isEmpty())
			result += ", role: " + role;
		result += ", enabled: " + enabled;
		if (operator != null)
			result += ", operator: " + operator;
		return result;
	}
}