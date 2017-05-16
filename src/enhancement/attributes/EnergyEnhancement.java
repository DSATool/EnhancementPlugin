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
package enhancement.attributes;

import dsa41basis.hero.Energy;
import dsa41basis.util.DSAUtil;
import enhancement.enhancements.Enhancement;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jsonant.value.JSONObject;

public class EnergyEnhancement extends Enhancement {
	private final Energy energy;
	private final IntegerProperty start;
	private final IntegerProperty target;
	private final int boughtStart;
	private final IntegerProperty ses;
	private final int seMin;

	public EnergyEnhancement(Energy energy, JSONObject hero) {
		this.energy = energy;
		start = new SimpleIntegerProperty(energy.getMax());
		target = new SimpleIntegerProperty(start.get() + 1);
		seMin = energy.getSes();
		ses = new SimpleIntegerProperty(seMin);
		fullDescription.bind(description);
		updateDescription();
		boughtStart = energy.getBought();
		cost.set(getCalculatedCost(hero));
		recalculateValid(hero);
		cheaper.set(energy.getSes() > 0);
	}

	@Override
	public void apply(JSONObject hero) {
		final JSONObject actual = hero.getObj("Basiswerte").getObj(energy.getName());
		final int resultSes = ses.get() - (target.get() - start.get());
		if (resultSes <= 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", resultSes);
		}
		actual.put("Kauf", getBoughtTarget());
		actual.notifyListeners(null);
	}

	@Override
	public void applyTemporarily(JSONObject hero) {
		final JSONObject actual = hero.getObj("Basiswerte").getObj(energy.getName());
		actual.put("Kauf", getBoughtTarget());
	}

	@Override
	protected boolean calculateValid(JSONObject hero) {
		return getBoughtTarget() <= energy.getBuyableMaximum();
	}

	public EnergyEnhancement clone(JSONObject hero) {
		final EnergyEnhancement result = new EnergyEnhancement(energy, hero);
		result.setTarget(target.get(), hero);
		return result;
	}

	private int getBoughtTarget() {
		return boughtStart + target.get() - start.get();
	}

	@Override
	protected int getCalculatedCost(JSONObject hero) {
		final int SELevel = boughtStart + Math.min(getBoughtTarget() - boughtStart, ses.get());
		return DSAUtil.getEnhancementCost(energy.getEnhancementCost() - 1, boughtStart, SELevel)
				+ DSAUtil.getEnhancementCost(energy.getEnhancementCost(), SELevel, getBoughtTarget());
	}

	@Override
	public String getName() {
		return energy.getName();
	}

	public int getSeMin() {
		return seMin;
	}

	public int getSes() {
		return ses.get();
	}

	public int getStart() {
		return start.get();
	}

	public int getTarget() {
		return target.get();
	}

	public IntegerProperty sesProperty() {
		return ses;
	}

	public void setSes(int ses, JSONObject hero) {
		this.ses.set(ses);
		resetCost(hero);
	}

	public void setTarget(int target, JSONObject hero) {
		this.target.set(target);
		updateDescription();
		recalculateValid(hero);
		resetCost(hero);
	}

	public IntegerProperty startProperty() {
		return start;
	}

	public IntegerProperty targetProperty() {
		return target;
	}

	@Override
	public void unapply(JSONObject hero) {
		final JSONObject actual = hero.getObj("Basiswerte").getObj(energy.getName());
		actual.put("Kauf", boughtStart);
	}

	private void updateDescription() {
		final String desc = energy.getName() + " (" + start.get() + "->" + target.get() + ")";
		description.set(desc);
	}
}
