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
public class Filling implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false)
	private Long id;
	@Version
	@Column(name = "version")
	private int version;

	@Column(nullable = false)
	private Long poubelle;

	@Column(nullable = false)
	private int fillingLevel;

	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date time;

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
		if (!(obj instanceof Filling)) {
			return false;
		}
		Filling other = (Filling) obj;
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

	public Long getPoubelle() {
		return poubelle;
	}

	public void setPoubelle(Long poubelle) {
		this.poubelle = poubelle;
	}

	public int getFillingLevel() {
		return fillingLevel;
	}

	public void setFillingLevel(int fillingLevel) {
		this.fillingLevel = fillingLevel;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	@Override
	public String toString() {
		String result = getClass().getSimpleName() + " ";
		if (id != null)
			result += "id: " + id;
		result += ", version: " + version;
		if (poubelle != null)
			result += ", poubelle: " + poubelle;
		result += ", fillingLevel: " + fillingLevel;
		if (time != null)
			result += ", time: " + time;
		return result;
	}
}