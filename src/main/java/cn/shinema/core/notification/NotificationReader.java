//   Copyright 2012,2013 Vaughn Vernon
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package cn.shinema.core.notification;

import java.math.BigDecimal;
import java.util.Date;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cn.shinema.core.media.AbstractJSONMediaReader;

public class NotificationReader extends AbstractJSONMediaReader {

	private JsonObject event;

	public NotificationReader(String aJSONNotification) {
		super(aJSONNotification);
		JsonElement ele = this.representation().get("event");
		if (null != ele) {
			this.setEvent(ele.getAsJsonObject());
		}
	}

	public NotificationReader(JsonObject aRepresentationObject) {
		super(aRepresentationObject);
		JsonElement ele = this.representation().get("event");
		if (null != ele) {
			this.setEvent(ele.getAsJsonObject());
		}
	}

	public BigDecimal eventBigDecimalValue(String... aKeys) {
		String stringValue = this.stringValue(this.event(), aKeys);

		return stringValue == null ? null : new BigDecimal(stringValue);
	}

	public Boolean eventBooleanValue(String... aKeys) {
		String stringValue = this.stringValue(this.event(), aKeys);

		return stringValue == null ? null : Boolean.parseBoolean(stringValue);
	}

	public Date eventDateValue(String... aKeys) {
		String stringValue = this.stringValue(this.event(), aKeys);

		return stringValue == null ? null : new Date(Long.parseLong(stringValue));
	}

	public Double eventDoubleValue(String... aKeys) {
		String stringValue = this.stringValue(this.event(), aKeys);

		return stringValue == null ? null : Double.parseDouble(stringValue);
	}

	public Float eventFloatValue(String... aKeys) {
		String stringValue = this.stringValue(this.event(), aKeys);

		return stringValue == null ? null : Float.parseFloat(stringValue);
	}

	public Integer eventIntegerValue(String... aKeys) {
		String stringValue = this.stringValue(this.event(), aKeys);

		return stringValue == null ? null : Integer.parseInt(stringValue);
	}

	public Long eventLongValue(String... aKeys) {
		String stringValue = this.stringValue(this.event(), aKeys);

		return stringValue == null ? null : Long.parseLong(stringValue);
	}

	public String eventStringValue(String... aKeys) {
		String stringValue = this.stringValue(this.event(), aKeys);

		return stringValue;
	}

	public long notificationId() {
		long notificationId = this.longValue("eventId");

		return notificationId;
	}

	public String notificationIdAsString() {
		String notificationId = this.stringValue("eventId");

		return notificationId;
	}

	public Date occurredOn() {
		long time = this.longValue("occurredOn");
		return new Date(time);
	}

	public String trackerName() {
		return this.stringValue("trackerName");
	}

	public int version() {
		return this.integerValue("version");
	}

	public String eventAsString() {
		return this.event().toString();
	}

	private void setEvent(JsonObject anEvent) {
		this.event = anEvent;
	}

	private JsonObject event() {
		return this.event;
	}

	public boolean isEventBody() {
		if (null != event()) {
			return true;
		} else {
			return false;
		}
	}
}
