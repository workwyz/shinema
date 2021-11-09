package cn.shinema.core.port.adapter.schedule;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cn.shinema.core.event.ConsumedEventRepository;
import cn.shinema.core.event.StoredEventRepository;

@Order(999)
@Component
public class EventCleanTask implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventCleanTask.class);

	@Autowired
	StoredEventRepository storedEventRepository;

	@Autowired
	ConsumedEventRepository consumedEventRepository;

	@Scheduled(cron = "0 0 1 L * ?")
//	@Scheduled(cron = "0 0/1 * * * ?")
	public void eventStoreClean() {
		Date expiredDate = getDeleteTime();
		long starttime = System.currentTimeMillis();
		LOGGER.info("Event Clean Task ：Expired Date =>{} , started=>{}", formatDate(expiredDate), starttime);

		storedEventRepository.deleteDataOfExpired(expiredDate.getTime());
//		storedEventRepository.deleteDataOfExpired(180);

		long endtime = System.currentTimeMillis();
		LOGGER.info("Event Clean Task ：completed=>{} , elapsed=>{}", endtime, (endtime - starttime));

	}

	@Scheduled(cron = "0 30 1 L * ?")
//	@Scheduled(cron = "0 0/1 * * * ?")
	public void consumedEventClean() {
		long starttime = System.currentTimeMillis();
		Date expiredDate = getDeleteTime();
		LOGGER.info("ConsumedEvent  Clean Task ：Expired Date =>{} , started=>{}", formatDate(expiredDate), starttime);

		consumedEventRepository.deleteDataOfExpired(expiredDate.getTime());
//		consumedEventRepository.deleteDataOfExpired(180);

		long endtime = System.currentTimeMillis();
		LOGGER.info("ConsumedEvent Clean Task ：completed=>{},elapsed=>{}", endtime, (endtime - starttime));
	}

	private Date getDeleteTime() {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.MONTH, -6);
		Date m3 = c.getTime();
		return m3;
	}

	private String formatDate(Date date) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(date);
	}

	public void run(String... args) throws Exception {
		LOGGER.info("===================================start");
	}

	public static void main(String[] args) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.MONTH, -3);
		Date m3 = c.getTime();
		String mon3 = format.format(m3);
		System.out.println("过去三个月：" + mon3);
		LOGGER.info("===================================currentTimeMillis=>{},lastMonthMillis=>{}", System.currentTimeMillis(), m3.getTime());
	}

}
