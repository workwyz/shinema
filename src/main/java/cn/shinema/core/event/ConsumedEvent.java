package cn.shinema.core.event;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "t_consumed_event", indexes = { @Index(columnList = "occurred_on") }, uniqueConstraints = {
		@UniqueConstraint(columnNames = { "tracker_name", "event_id", "receive_name" }) })
public class ConsumedEvent implements Serializable {
	private static final long serialVersionUID = -5673529205674116258L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true, length = 20)
	private Long id;

	@Column(name = "event_id", nullable = false, length = 20)
	private Long eventId;

	@Column(name = "tracker_name", nullable = false, length = 150)
	private String trackerName;

	@Column(name = "receive_name", nullable = true, length = 150)
	private String receiveName;

	@Column(name = "occurred_on")
	private Long occurredOn;

	public ConsumedEvent() {
		super();
	}

	public ConsumedEvent(Long eventId, String trackerName, String receiveName) {
		super();
		this.eventId = eventId;
		this.receiveName = receiveName;
		this.trackerName = trackerName;
		this.occurredOn = System.currentTimeMillis();
	}

	public long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String getTrackerName() {
		return trackerName;
	}

	public void setTrackerName(String trackerName) {
		this.trackerName = trackerName;
	}

	public String getReceiveName() {
		return receiveName;
	}

	public void setReceiveName(String receiveName) {
		this.receiveName = receiveName;
	}

	@PrePersist
	void createdAt() {
		this.occurredOn = System.currentTimeMillis();
	}

	public String toString() {
		return "ConsumedEventStore [eventId=" + eventId + ", trackerName=" + trackerName + "]";
	}

}
