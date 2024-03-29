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

package cn.shinema.core.domain.process;

import java.util.Date;

import cn.shinema.core.domain.DomainEvent;

public class ProcessTimedOut implements DomainEvent {

	private ProcessId processId;
	private String tenantId;
	private int retryCount;
	private int totalRetriesPermitted;
	private int eventVersion;
	private Date occurredOn;

	public ProcessTimedOut(String aTenantId, ProcessId aProcessId, int aTotalRetriesPermitted, int aRetryCount) {
		super();

		this.eventVersion = 1;
		this.occurredOn = new Date();
		this.processId = aProcessId;
		this.retryCount = aRetryCount;
		this.tenantId = aTenantId;
		this.totalRetriesPermitted = aTotalRetriesPermitted;
	}

	public boolean allowsRetries() {
		return this.totalRetriesPermitted() > 0;
	}

	public int eventVersion() {
		return this.eventVersion;
	}

	public boolean hasFullyTimedOut() {
		return !this.allowsRetries() || this.totalRetriesReached();
	}

	public Date occurredOn() {
		return this.occurredOn;
	}

	public ProcessId processId() {
		return processId;
	}

	public int retryCount() {
		return retryCount;
	}

	public String tenantId() {
		return tenantId;
	}

	public int totalRetriesPermitted() {
		return totalRetriesPermitted;
	}

	public boolean totalRetriesReached() {
		return this.retryCount() >= this.totalRetriesPermitted();
	}
}
