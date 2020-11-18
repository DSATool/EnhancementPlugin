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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public abstract class Enhancement {
	protected final StringProperty description = new SimpleStringProperty();
	protected final StringProperty fullDescription = new SimpleStringProperty();
	protected final DoubleProperty cost = new SimpleDoubleProperty();
	protected final IntegerProperty ap = new SimpleIntegerProperty();
	protected final BooleanProperty valid = new SimpleBooleanProperty(true);
	protected final BooleanProperty cheaper = new SimpleBooleanProperty(false);
	protected final StringProperty date = new SimpleStringProperty();

	protected boolean hasCustomCost = false;
	protected boolean hasCustomAP = false;

	public abstract void apply(JSONObject hero);

	public abstract void applyTemporarily(JSONObject hero);

	public IntegerProperty apProperty() {
		return ap;
	}

	protected abstract boolean calculateValid(JSONObject hero);

	public BooleanProperty cheaperProperty() {
		return cheaper;
	}

	public DoubleProperty costProperty() {
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

	public int getAP() {
		return ap.get();
	}

	protected abstract int getCalculatedAP(JSONObject hero);

	protected abstract double getCalculatedCost(JSONObject hero);

	public double getCost() {
		return cost.get();
	}

	public String getDate() {
		return date.get();
	}

	public String getFullDescription() {
		return fullDescription.get();
	}

	public abstract String getName();

	public boolean isCheaper() {
		return cheaper.get();
	}

	public boolean isValid() {
		return valid.get();
	}

	public void recalculateCosts(final JSONObject hero) {
		if (!hasCustomAP) {
			ap.set(getCalculatedAP(hero));
		}
		if (!hasCustomCost) {
			cost.set(getCalculatedCost(hero));
		}
	}

	public void recalculateValid(final JSONObject hero) {
		valid.set(calculateValid(hero));
	}

	public void reset(final JSONObject hero) {
		ap.set(getCalculatedAP(hero));
		cost.set(getCalculatedCost(hero));
		hasCustomAP = false;
		hasCustomCost = false;
	}

	public void setAP(final int ap, final JSONObject hero) {
		this.ap.set(ap);
		hasCustomAP = true;
		if (!hasCustomCost) {
			cost.set(getCalculatedCost(hero));
		}
	}

	public void setCost(final double cost) {
		this.cost.set(cost);
		hasCustomCost = true;
	}

	public JSONObject toJSON() {
		return null;
	}

	public abstract void unapply(JSONObject hero);

	public abstract void unapplyTemporary(JSONObject hero);

	public BooleanProperty validProperty() {
		return valid;
	}
}
