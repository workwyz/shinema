package cn.shinema.core.event;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface StoredEventRepository extends JpaRepository<StoredEvent, Long> {

	@Query("select eventId from StoredEvent where trackerName = :trackerName and sendStatus = 0")
	public Collection<Long> listUnpublishedEventIds(@Param("trackerName") String trackerName);

	@Modifying
	@Transactional(rollbackFor = { Throwable.class })
	@Query("update StoredEvent set sendStatus = 1 where eventId in :ids")
	public void batchUpdateByIds(@Param(value = "ids") List<Long> ids);

	@Modifying
	@Transactional(rollbackFor = { Throwable.class })
	@Query("delete StoredEvent where sendStatus = 1 and occurredOn <= :expireDate")
	public void deleteDataOfExpired(@Param(value = "expireDate") long expireDate);

//	@Modifying
//	@Transactional(rollbackFor = { Throwable.class })
//	@Query(value = "delete from t_stored_event where send_status=1 and created_date <= date_sub(now(),interval ?1 day)", nativeQuery = true)
//	int deleteDataOfExpired(int expiredays);

	public List<StoredEvent> findByEventIdBetween(long aLowStoredEventId, long aHighStoredEventId);

	public List<StoredEvent> findByEventIdIn(Collection<Long> eventIds, Pageable pageable);

//	public List<StoredEvent> findBySendStatusAndEventIdIn(int sendStatus, Collection<Long> eventIds);

	public List<StoredEvent> findByTrackerNameAndSendStatus(String trackerName, int sendStatus, Pageable pageable);

//	public List<StoredEvent> findByTrackerNameAndSendStatusAndEventIdGreaterThan(String trackerName, int sendStatus, long aStoredEventId, Pageable pageable);

}
