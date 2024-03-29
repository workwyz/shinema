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

package cn.shinema.core.event.sourcing;

import cn.shinema.core.domain.DomainEvent;

public class DispatchableDomainEvent {

    private DomainEvent domainEvent;
    private long eventId;

    public DispatchableDomainEvent(long anEventId, DomainEvent aDomainEvent) {
        super();

        this.domainEvent = aDomainEvent;
        this.eventId = anEventId;
    }

    public DomainEvent domainEvent() {
        return this.domainEvent;
    }

    public long eventId() {
        return this.eventId;
    }
}
