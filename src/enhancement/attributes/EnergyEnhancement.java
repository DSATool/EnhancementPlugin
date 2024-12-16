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

import java.time.LocalDate;

import dsa41basis.hero.Energy;
import dsa41basis.util.DSAUtil;
import dsatool.resources.ResourceManager;
import enhancement.enhancements.Enhancement;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class EnergyEnhancement extends Enhancement {
	public static EnergyEnhancement fromJSON(final JSONObject enhancement, final JSONObject hero) {
		final String energyName = enhancement.getString("Basiswert");
		final Energy energy = new Energy(energyName, ResourceManager.getResource("data/Basiswerte").getObj(energyName), hero);
		final EnergyEnhancement result = new EnergyEnhancement(energy, hero);
		result.start.set(enhancement.getInt("Von"));
		result.target.set(enhancement.getInt("Auf"));
		result.ses.set(energy.getSes() + enhancement.getIntOrDefault("SEs", 0));
		result.ap.set(enhancement.getInt("AP"));
		result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateFormatter));
		result.updateDescription();
		return result;
	}

	private final Energy energy;
	private final IntegerProperty start;
	private final IntegerProperty target;
	private final IntegerProperty ses;

	public EnergyEnhancement(final Energy energy, final JSONObject hero) {
		this.energy = energy;
		start = new SimpleIntegerProperty(energy.getBought());
		target = new SimpleIntegerProperty(start.get() + 1);
		ses = new SimpleIntegerProperty(energy.getSes());
		fullDescription.bind(description);
		updateDescription();
		ap.set(getCalculatedAP(hero));
		recalculateValid(hero);
		cheaper.set(energy.getSes() > 0);
	}

	@Override
	public void apply(final JSONObject hero) {
		final JSONObject actual = hero.getObj("Basiswerte").getObj(energy.getName());
		final int resultSes = ses.get() - (target.get() - start.get());
		if (resultSes <= 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", resultSes);
		}
		actual.put("Kauf", target.getValue());
		actual.notifyListeners(null);
	}

	@Override
	public void applyTemporarily(final JSONObject hero) {
		final JSONObject actual = hero.getObj("Basiswerte").getObj(energy.getName());
		actual.put("Kauf", target.getValue());
	}

	@Override
	protected boolean calculateValid(final JSONObject hero) {
		return target.getValue() <= energy.getBuyableMaximum();
	}

	public EnergyEnhancement clone(final JSONObject hero) {
		final EnergyEnhancement result = new EnergyEnhancement(energy, hero);
		result.start.set(start.get());
		result.target.set(target.get());
		result.ses.set(ses.get());
		result.valid.set(valid.get());
		result.updateDescription();
		return result;
	}

	@Override
	protected int getCalculatedAP(final JSONObject hero) {
		final int SELevel = energy.getBought() + Math.min(target.getValue() - energy.getBought(), ses.get());
		return DSAUtil.getEnhancementCost(energy.getEnhancementCost() - 1, energy.getBought(), SELevel)
				+ DSAUtil.getEnhancementCost(energy.getEnhancementCost(), SELevel, target.getValue());
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
		if (target.get() > energy.getBuyableMaximum()) return "Maximale Steigerung " + (energy.getMax() - energy.getBought() + energy.getBuyableMaximum());
		return "";
	}

	@Override
	public String getName() {
		return energy.getName();
	}

	public int getSes() {
		return ses.get();
	}

	public int getStart() {
		return energy.getMax() - energy.getBought() + start.get();
	}

	public int getTarget() {
		return energy.getMax() - energy.getBought() + target.get();
	}

	public IntegerProperty sesProperty() {
		return ses;
	}

	public void setSes(final int ses, final JSONObject hero) {
		this.ses.set(ses);
		reset(hero);
	}

	public void setTarget(final int target, final JSONObject hero) {
		this.target.set(target + energy.getBought() - energy.getMax());
		updateDescription();
		recalculateValid(hero);
		reset(hero);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#toJSON()
	 */
	@Override
	public JSONObject toJSON(final JSONValue parent) {
		final JSONObject result = new JSONObject(parent);
		result.put("Typ", "Basiswert");
		result.put("Basiswert", energy.getName());
		result.put("Von", start.get());
		result.put("Auf", target.get());
		final int resultSes = Math.min(ses.get(), target.get() - start.get());
		if (resultSes > 0) {
			result.put("SEs", resultSes);
		}
		result.put("AP", ap.get());
		final LocalDate currentDate = LocalDate.now();
		result.put("Datum", currentDate.toString());
		return result;
	}

	@Override
	public void unapply(final JSONObject hero) {
		final JSONObject actual = hero.getObj("Basiswerte").getObj(energy.getName());
		actual.put("Kauf", start.get());
		actual.put("SEs", ses.get());
		actual.notifyListeners(null);
	}

	@Override
	public void unapplyTemporary(final JSONObject hero) {
		unapply(hero);
	}

	private void updateDescription() {
		final String desc = energy.getName() + " (" + (energy.getMax() - energy.getBought() + start.getValue()) + "->"
				+ (energy.getMax() - energy.getBought() + target.get()) + ")";
		description.set(desc);
	}
}
