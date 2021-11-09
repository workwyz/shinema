package cn.shinema.core.notification;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublishedNotificationTrackerRepository extends JpaRepository<PublishedNotificationTracker, Serializable> {

	public PublishedNotificationTracker findByTrackerName(String trackerName);
}
