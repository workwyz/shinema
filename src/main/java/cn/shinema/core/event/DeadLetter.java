package cn.shinema.core.event;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import cn.shinema.core.AssertionConcern;

@Entity
@Table(name = "t_dead_letter", uniqueConstraints = { @UniqueConstraint(columnNames = { "tracker_name", "event_id", "receive_name" }) })
@NamedQuery(name = "DeadLetter.findAll", query = "SELECT d FROM DeadLetter d")
public class DeadLetter extends AssertionConcern implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true, length = 20)
	private long id;

	@Column(name = "event_id", nullable = false, length = 20)
	private long eventId;

	@Column(name = "tracker_name", nullable = true, length = 150)
	private String trackerName;

	@Column(name = "receive_name", nullable = true, length = 150)
	private String receiveName;

	@Version
	@Column(name = "concurrency_version", nullable = false, length = 11)
	private int concurrencyVersion;

	@Lob
	@Column(name = "payload", nullable = true)
	private String payload;

	@Column(name = "occurred_on")
	private Long occurredOn;

	@Column(name = "last_occurred_on")
	private Long lastOccurredOn;

	public DeadLetter() {
	}

	public DeadLetter(Long eventId, String trackerName, String receiveName, String payload) {
		super();
		this.eventId = eventId;
		this.trackerName = trackerName;
		this.receiveName = receiveName;
		this.payload = payload;
		this.occurredOn = System.currentTimeMillis();
	}

	public Long notificationId() {
		return eventId;
	}

	public void setNotificationId(Long notificationId) {
		this.eventId = notificationId;
	}

	public String trackerName() {
		return trackerName;
	}

	public void setTrackerName(String trackerName) {
		this.trackerName = trackerName;
	}

	public String payload() {
		return this.payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public int concurrencyVersion() {
		return concurrencyVersion;
	}

	@PrePersist
	void createdAt() {
		this.occurredOn = System.currentTimeMillis();
	}

	@PreUpdate
	void updatedAt() {
		this.lastOccurredOn = System.currentTimeMillis();
	}
}