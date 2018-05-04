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
public class Alert implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, nullable = false)
	private Long id;
	@Version
	@Column(name = "version")
	private int version;

	@Column(nullable = false, updatable = false)
	private Long poubelle;

	@Column
	private Long alertType;

	@Column(length = 100000)
	private String message;

	@Column
	private Long image;

	@Column
	@Temporal(TemporalType.DATE)
	private Date created;

	@Column
	private boolean favored;

	@Column
	private boolean checked;

	@Column
	private boolean deleted;
	
	@Column
	private boolean voted;
	
	@Column
	private Long votes;

	@Column
	private Long creator;

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
		if (!(obj instanceof Alert)) {
			return false;
		}
		Alert other = (Alert) obj;
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

	public Long getAlertType() {
		return alertType;
	}

	public void setAlertType(Long alertType) {
		this.alertType = alertType;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Long getImage() {
		return image;
	}

	public void setImage(Long image) {
		this.image = image;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public boolean isFavored() {
		return favored;
	}

	public void setFavored(boolean favored) {
		this.favored = favored;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	public boolean isVoted() {
		return voted;
	}

	public void setVoted(boolean voted) {
		this.voted = voted;
	}
	
	public Long getVotes() {
		return votes;
	}

	public void setVotes(Long votes) {
		this.votes = votes;
	}

	public Long getCreator() {
		return creator;
	}

	public void setCreator(Long creator) {
		this.creator = creator;
	}

	@Override
	public String toString() {
		String result = getClass().getSimpleName() + " ";
		if (id != null)
			result += "id: " + id;
		result += ", version: " + version;
		if (poubelle != null)
			result += ", poubelle: " + poubelle;
		if (alertType != null)
			result += ", alertType: " + alertType;
		if (message != null && !message.trim().isEmpty())
			result += ", message: " + message;
		if (image != null)
			result += ", image: " + image;
		if (created != null)
			result += ", created: " + created;
		result += ", favored: " + favored;
		result += ", checked: " + checked;
		result += ", deleted: " + deleted;
		result += ", voted: " + voted;
		if (votes != null)
			result += ", votes: " + votes;
		if (creator != null)
			result += ", creator: " + creator;
		return result;
	}
}