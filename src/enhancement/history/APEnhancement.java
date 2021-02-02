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
import java.time.format.DateTimeFormatter;

import enhancement.enhancements.Enhancement;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jsonant.value.JSONObject;

public class APEnhancement extends Enhancement {
	public static APEnhancement fromJSON(final JSONObject enhancement) {
		final APEnhancement result = new APEnhancement(enhancement.getInt("Von"), enhancement.getInt("Auf"));
		result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu")));
		return result;
	}

	private final IntegerProperty start;
	private final IntegerProperty target;

	private APEnhancement(final int start, final int target) {
		this.start = new SimpleIntegerProperty(start);
		this.target = new SimpleIntegerProperty(target);
		fullDescription.bind(description);
		description.set("Abenteuerpunkte (" + this.start.get() + "->" + this.target.get() + ")");
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
		return start.get() - target.get();
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
	public String getName() {
		return "Abenteuerpunkte";
	}

	public int getStart() {
		return start.get();
	}

	public int getTarget() {
		return target.get();
	}

	public ReadOnlyIntegerProperty startProperty() {
		return start;
	}

	public ReadOnlyIntegerProperty targetProperty() {
		return target;
	}

	@Override
	public void unapply(final JSONObject hero) {
		final JSONObject bio = hero.getObj("Biografie");
		bio.put("Abenteuerpunkte", start.get());
		bio.notifyListeners(null);
	}

	@Override
	public void unapplyTemporary(final JSONObject hero) {}
}
