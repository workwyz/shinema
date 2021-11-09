package cn.shinema.core.event;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ConsumedEventRepository extends JpaRepository<ConsumedEvent, Serializable> {

//	@Query("select count(1) from ConsumedEvent  where trackerName= :trackerName and eventId= :eventId")
//	public int isDealWithEvent(@Param("trackerName") String trackerName, @Param("eventId") long eventId);

	@Modifying
	@Transactional(rollbackFor = { Throwable.class })
	@Query("delete ConsumedEvent where occurredOn <= :expireDate")
	public void deleteDataOfExpired(@Param(value = "expireDate") long expireDate);

//	@Modifying
//	@Transactional(rollbackFor = { Throwable.class })
//	@Query(value = "delete from t_consumed_event where created_date <= date_sub(now(),interval ?1 day)", nativeQuery = true)
//	int deleteDataOfExpired(int expiredays);

	public int countByTrackerNameAndReceiveNameAndEventId(String trackerName, String receiveName, long eventId);

}
