package fr.bellepoubelle.api.model;

import javax.persistence.Entity;
import java.io.Serializable;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class Poubelle implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, nullable = false)
	private Long id;
	@Version
	@Column(name = "version")
	private int version;

	@Column(nullable = false)
	private double longitude;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private Long category;

	@Column(length = 3)
	private int fillingLevel;

	@Column(nullable = false)
	private Long address;

	@Column(nullable = false, updatable = false)
	private Long codeInsee;

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
		if (!(obj instanceof Poubelle)) {
			return false;
		}
		Poubelle other = (Poubelle) obj;
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

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public Long getCategory() {
		return category;
	}

	public void setCategory(Long category) {
		this.category = category;
	}

	public int getFillingLevel() {
		return fillingLevel;
	}

	public void setFillingLevel(int fillingLevel) {
		this.fillingLevel = fillingLevel;
	}

	public Long getAddress() {
		return address;
	}

	public void setAddress(Long address) {
		this.address = address;
	}

	public Long getCodeInsee() {
		return codeInsee;
	}

	public void setCodeInsee(Long codeInsee) {
		this.codeInsee = codeInsee;
	}

	@Override
	public String toString() {
		String result = getClass().getSimpleName() + " ";
		if (id != null)
			result += "id: " + id;
		result += ", version: " + version;
		result += ", longitude: " + longitude;
		result += ", latitude: " + latitude;
		if (category != null)
			result += ", category: " + category;
		result += ", fillingLevel: " + fillingLevel;
		if (address != null)
			result += ", address: " + address;
		if (codeInsee != null)
			result += ", codeInsee: " + codeInsee;
		return result;
	}
}