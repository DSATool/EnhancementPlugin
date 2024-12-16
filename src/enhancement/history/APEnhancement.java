/*
 * Copyright 2017 DSATool team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package enhancement.history;

import java.time.LocalDate;

import enhancement.enhancements.Enhancement;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class APEnhancement extends Enhancement {
	public static APEnhancement fromJSON(final JSONObject enhancement) {
		final APEnhancement result = new APEnhancement(enhancement.getInt("Von"), enhancement.getInt("Auf"));
		result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateFormatter));
		return result;
	}

	private final int start;
	private final int target;

	private APEnhancement(final int start, final int target) {
		this.start = start;
		this.target = target;
		fullDescription.bind(description);
		description.set("Abenteuerpunkte (" + start + "->" + target + ")");
		ap.set(target - start);
	}

	@Override
	public void apply(final JSONObject hero) {}

	@Override
	public void applyTemporarily(final JSONObject hero) {}

	@Override
	protected boolean calculateValid(final JSONObject hero) {
		return true;
	}

	@Override
	protected int getCalculatedAP(final JSONObject hero) {
		return start - target;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#getCalculatedCost(jsonant.value.JSONObject)
	 */
	@Override
	protected double getCalculatedCost(final JSONObject hero) {
		return 0;
	}

	@Override
	public String getInvalidReason(final JSONObject hero) {
		return "";
	}

	@Override
	public String getName() {
		return "Abenteuerpunkte";
	}

	@Override
	public JSONObject toJSON(final JSONValue parent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unapply(final JSONObject hero) {
		final JSONObject bio = hero.getObj("Biografie");
		bio.put("Abenteuerpunkte", start);
		bio.notifyListeners(null);
	}

	@Override
	public void unapplyTemporary(final JSONObject hero) {}
}
