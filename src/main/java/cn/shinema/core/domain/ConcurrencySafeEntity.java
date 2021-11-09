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

package cn.shinema.core.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@MappedSuperclass
public class ConcurrencySafeEntity extends IdentifiedDomainObject {
	private static final long serialVersionUID = 4640713228030038301L;

	@Version
	@Column(name = "concurrency_version")
	private int concurrencyVersion;

	@CreatedDate
	@Column(name = "created_date")
	private Date createdDate;

	@LastModifiedDate
	@Column(name = "last_modified_date")
	private Date lastModifiedDate;

	protected ConcurrencySafeEntity() {
		super();
	}

	public int concurrencyVersion() {
		return this.concurrencyVersion;
	}

	public void setConcurrencyVersion(int aVersion) {
		this.failWhenConcurrencyViolation(aVersion);
		this.concurrencyVersion = aVersion;
	}

	public Date createdDate() {
		return createdDate;
	}

	public Date lastModifiedDate() {
		return lastModifiedDate;
	}

	public void failWhenConcurrencyViolation(int aVersion) {
		if (aVersion != this.concurrencyVersion()) {
			throw new IllegalStateException("Concurrency Violation: Stale data detected. Entity was already modified.");
		}
	}
}
