/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management.internal.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PagePkMongo implements Serializable {

	private static final long serialVersionUID = -8506413219938069655L;

	private String id;
	private String referenceId;
	private String referenceType;

	public PagePkMongo() {
	}

	public PagePkMongo(String id, String referenceId, String referenceType) {
		this.id = id;
		this.referenceId = referenceId;
		this.referenceType = referenceType;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public String getReferenceType() {
		return referenceType;
	}

	public void setReferenceType(String referenceType) {
		this.referenceType = referenceType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PagePkMongo)) return false;
		PagePkMongo that = (PagePkMongo) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(referenceId, that.referenceId) &&
				Objects.equals(referenceType, that.referenceType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, referenceId, referenceType);
	}

	@Override
	public String toString() {
		return "PagePkMongo{" +
				"id='" + id + '\'' +
				", referenceId='" + referenceId + '\'' +
				", referenceType='" + referenceType + '\'' +
				'}';
	}
}
