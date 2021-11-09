package cn.shinema.core.event.process;

import org.springframework.context.ApplicationEvent;

public class EventStoreSuccess extends ApplicationEvent {
	private static final long serialVersionUID = 8579314799557592053L;

	private String trackerName;
	private Long eventId;

	public EventStoreSuccess(Object source) {
		super(source);
	}

	public EventStoreSuccess(Object source, String trackerName, Long eventId) {
		super(source);
		this.trackerName = trackerName;
		this.eventId = eventId;
	}

	public String trackerName() {
		return trackerName;
	}

	public void setTrackerName(String trackerName) {
		this.trackerName = trackerName;
	}

	public Long eventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

}
