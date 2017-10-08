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
package enhancement.enhancements;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public abstract class Enhancement {
	protected final StringProperty description = new SimpleStringProperty();
	protected final StringProperty fullDescription = new SimpleStringProperty();
	protected final IntegerProperty cost = new SimpleIntegerProperty();
	protected final BooleanProperty valid = new SimpleBooleanProperty(true);
	protected final BooleanProperty cheaper = new SimpleBooleanProperty(false);
	protected final StringProperty date = new SimpleStringProperty();

	private boolean hasCustomCost = false;

	public abstract void apply(JSONObject hero);

	public abstract void applyTemporarily(JSONObject hero);

	protected abstract boolean calculateValid(JSONObject hero);

	public BooleanProperty cheaperProperty() {
		return cheaper;
	}

	public IntegerProperty costProperty() {
		return cost;
	}

	public ReadOnlyStringProperty dateProperty() {
		return date;
	}

	public ReadOnlyStringProperty descriptionProperty() {
		return description;
	}

	public ReadOnlyStringProperty fullDescriptionProperty() {
		return fullDescription;
	}

	protected abstract int getCalculatedCost(JSONObject hero);

	public int getCost() {
		return cost.get();
	}

	public String getDate() {
		return date.get();
	}

	public abstract String getName();

	public boolean isCheaper() {
		return cheaper.get();
	}

	public boolean isValid() {
		return valid.get();
	}

	public void recalculateCost(final JSONObject hero) {
		if (!hasCustomCost) {
			resetCost(hero);
		}
	}

	public void recalculateValid(final JSONObject hero) {
		valid.set(calculateValid(hero));
	}

	public void resetCost(final JSONObject hero) {
		cost.set(getCalculatedCost(hero));
		hasCustomCost = false;
	}

	public void setCost(final int cost) {
		this.cost.set(cost);
		hasCustomCost = true;
	}

	public JSONObject toJSON() {
		return null;
	}

	public abstract void unapply(JSONObject hero);

	public BooleanProperty validProperty() {
		return valid;
	}
}
