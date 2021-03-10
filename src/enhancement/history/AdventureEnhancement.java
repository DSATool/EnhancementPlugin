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

import dsa41basis.hero.Attribute;
import dsa41basis.hero.Energy;
import dsa41basis.hero.Enhanceable;
import dsa41basis.hero.Spell;
import dsa41basis.hero.Talent;
import dsa41basis.util.HeroUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.Tuple;
import dsatool.util.Tuple3;
import enhancement.enhancements.Enhancement;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class AdventureEnhancement extends Enhancement {

	public enum Type {
		TALENT, SPELL, ATTRIBUTE, ENERGY
	}

	public static AdventureEnhancement fromJSON(final JSONObject enhancement) {
		final AdventureEnhancement result = new AdventureEnhancement(enhancement);

		result.description.set(enhancement.getStringOrDefault("Name", "Unbenanntes Abenteuer"));

		final JSONObject ap = enhancement.getObj("Abenteuerpunkte");
		result.ap.set(ap.getInt("Auf") - ap.getInt("Von"));

		result.cost.set(enhancement.getDouble("Silber"));

		final JSONObject sesList = enhancement.getObj("Spezielle Erfahrungen");

		final JSONObject attributes = sesList.getObj("Eigenschaften");
		for (final String attribute : attributes.keySet()) {
			result.ses.add(new Tuple3<>(attribute, attributes.getInt(attribute), Type.ATTRIBUTE));
		}

		final JSONObject energies = sesList.getObj("Basiswerte");
		for (final String energy : energies.keySet()) {
			result.ses.add(new Tuple3<>(energy, energies.getInt(energy), Type.ENERGY));
		}

		final JSONObject talents = sesList.getObj("Talente");
		for (final String talent : talents.keySet()) {
			result.ses.add(new Tuple3<>(talent, talents.getInt(talent), Type.TALENT));
		}

		final JSONObject spells = sesList.getObj("Zauber");
		for (final String spell : spells.keySet()) {
			result.ses.add(new Tuple3<>(spell, spells.getInt(spell), Type.SPELL));
		}

		result.notes.set(enhancement.getStringOrDefault("Anmerkungen", ""));
		result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateFormatter));
		return result;
	}

	private final JSONObject actual;

	private final ObservableList<Tuple3<String, Integer, Type>> ses;
	private final StringProperty notes;

	public AdventureEnhancement() {
		this(null);
	}

	private AdventureEnhancement(final JSONObject actual) {
		this.actual = actual;
		ses = FXCollections.observableArrayList();
		notes = new SimpleStringProperty("");
		fullDescription.bind(Bindings.concat("Abenteuer: ", description));
	}

	@Override
	public void apply(final JSONObject hero) {
		final JSONObject bio = hero.getObj("Biografie");
		bio.put("Abenteuerpunkte", bio.getInt("Abenteuerpunkte") + ap.get());
		bio.put("Abenteuerpunkte-Guthaben", bio.getInt("Abenteuerpunkte-Guthaben") + ap.get());
		bio.notifyListeners(null);
		HeroUtil.addMoney(hero, (int) (cost.get() * 100));
		for (final Tuple3<String, Integer, Type> se : ses) {
			final Type tpe = se._3;
			final Enhanceable toEnhance = getForSEs(hero, se._1, tpe);
			toEnhance.setSes(toEnhance.getSes() + se._2);
		}
	}

	@Override
	public void applyTemporarily(final JSONObject hero) {}

	@Override
	protected boolean calculateValid(final JSONObject hero) {
		return true;
	}

	@Override
	public StringProperty dateProperty() {
		return date;
	}

	@Override
	public StringProperty descriptionProperty() {
		return description;
	}

	public JSONObject getActual() {
		return actual;
	}

	@Override
	protected int getCalculatedAP(final JSONObject hero) {
		return ap.get();
	}

	@Override
	protected double getCalculatedCost(final JSONObject hero) {
		return -cost.get();
	}

	private Enhanceable getForSEs(final JSONObject hero, final String name, final Type tpe) {
		return switch (tpe) {
			case ATTRIBUTE -> {
				yield new Attribute(name, hero.getObj("Eigenschaften").getObj(name));
			}
			case ENERGY -> {
				yield new Energy(name, ResourceManager.getResource("data/Basiswerte").getObj(name), hero);
			}
			case TALENT -> {
				final Tuple<JSONObject, String> talentAndGroup = HeroUtil.findTalent(name);
				final Tuple<JSONValue, JSONObject> actualTalent = HeroUtil.findActualTalent(hero, name);
				final JSONObject talent = talentAndGroup._1.containsKey("Auswahl") || talentAndGroup._1.containsKey("Freitext")
						? ((JSONArray) actualTalent._1).getObj(0) : (JSONObject) actualTalent._1;
				yield Talent.getTalent(name, ResourceManager.getResource("data/Talente").getObj(talentAndGroup._2), talentAndGroup._1, talent, actualTalent._2);
			}
			case SPELL -> {
				final int repStart = name.lastIndexOf('(');
				final String spellName = name.substring(0, repStart - 1);
				final String representation = name.substring(repStart + 1, name.length() - 1);
				final JSONObject spell = HeroUtil.findTalent(spellName)._1;
				final Tuple<JSONValue, JSONObject> actualTalentAndGroup = HeroUtil.findActualTalent(hero, spellName);
				final JSONObject actualSpell = (JSONObject) actualTalentAndGroup._1;
				yield Spell.getSpell(spellName, spell, actualSpell != null ? actualSpell.getObj(representation) : null, actualSpell, actualTalentAndGroup._2,
						representation);
			}
		};
	}

	@Override
	public String getName() {
		return description.get();
	}

	public String getNotes() {
		return notes.get();
	}

	public ObservableList<Tuple3<String, Integer, Type>> getSes() {
		return ses;
	}

	public StringProperty notesProperty() {
		return notes;
	}

	@Override
	public JSONObject toJSON() {
		throw new UnsupportedOperationException();
	}

	public JSONObject toJSON(final JSONObject hero) {
		final JSONObject result = new JSONObject(null);
		result.put("Typ", "Abenteuer");
		result.put("Name", description.get());

		final JSONObject ap = new JSONObject(result);
		final int startAP = hero.getObj("Biografie").getIntOrDefault("Abenteuerpunkte", 0);
		ap.put("Von", startAP);
		ap.put("Auf", startAP + this.ap.get());
		result.put("Abenteuerpunkte", ap);

		result.put("Silber", cost.get());

		final JSONObject sesList = new JSONObject(result);
		final JSONObject talents = new JSONObject(sesList);
		final JSONObject spells = new JSONObject(sesList);
		final JSONObject attributes = new JSONObject(sesList);
		final JSONObject energies = new JSONObject(sesList);

		for (final Tuple3<String, Integer, Type> se : ses) {
			switch (se._3) {
				case ATTRIBUTE -> attributes.put(se._1, se._2);
				case ENERGY -> energies.put(se._1, se._2);
				case TALENT -> talents.put(se._1, se._2);
				case SPELL -> spells.put(se._1, se._2);
			}
		}

		if (attributes.size() != 0) {
			sesList.put("Eigenschaften", attributes);
		}
		if (attributes.size() != 0) {
			sesList.put("Basiswerte", energies);
		}
		if (talents.size() != 0) {
			sesList.put("Talente", talents);
		}
		if (spells.size() != 0) {
			sesList.put("Zauber", spells);
		}

		if (sesList.size() != 0) {
			result.put("Spezielle Erfahrungen", sesList);
		}

		if (!notes.get().isEmpty()) {
			result.put("Anmerkungen", notes.get());
		}

		result.put("Datum", LocalDate.parse(date.get(), DateFormatter).toString());
		return result;
	}

	@Override
	public void unapply(final JSONObject hero) {
		final JSONObject bio = hero.getObj("Biografie");
		bio.put("Abenteuerpunkte", bio.getInt("Abenteuerpunkte") - ap.get());
		bio.notifyListeners(null);
		for (final Tuple3<String, Integer, Type> se : ses) {
			final Type tpe = se._3;
			final Enhanceable toEnhance = getForSEs(hero, se._1, tpe);
			toEnhance.setSes(toEnhance.getSes() - se._2);
		}
	}

	@Override
	public void unapplyTemporary(final JSONObject hero) {}

}
