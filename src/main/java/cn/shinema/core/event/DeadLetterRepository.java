package cn.shinema.core.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DeadLetterRepository extends JpaRepository<DeadLetter, Long> {

	@Modifying
	@Transactional(rollbackFor = { Throwable.class })
	@Query("update DeadLetter set relayStatus = 2 where eventId = :id")
	public void updateRelayStatusById(@Param(value = "id") Long id);

	public boolean existsByEventIdAndTrackerNameAndReceiveName(long eventId, String trackerName, String receiveName);
}
