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
package enhancement.talents;

import java.time.LocalDate;

import dsa41basis.hero.Spell;
import dsa41basis.util.DSAUtil;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import enhancement.enhancements.EnhancementController;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class SpellEnhancement extends TalentEnhancement {
	public static SpellEnhancement fromJSON(final JSONObject enhancement, final JSONObject hero) {
		final String spellName = enhancement.getString("Zauber");
		final JSONObject spell = ResourceManager.getResource("data/Zauber").getObj(spellName);
		final JSONObject actualSpell = hero.getObj("Zauber").getObj(spellName);
		final String rep = enhancement.getString("Repräsentation");
		final JSONValue actualRep = (JSONValue) actualSpell.getUnsafe(rep);
		JSONObject actual = null;
		if (spell.containsKey("Auswahl")) {
			for (int i = 0; i < actualRep.size(); ++i) {
				final JSONObject choiceTalent = ((JSONArray) actualRep).getObj(i);
				if (choiceTalent.getString("Auswahl").equals(enhancement.getString("Auswahl"))) {
					actual = choiceTalent;
					break;
				}
			}
		} else if (spell.containsKey("Freitext")) {
			for (int i = 0; i < actualRep.size(); ++i) {
				final JSONObject choiceTalent = ((JSONArray) actualRep).getObj(i);
				if (choiceTalent.getString("Freitext").equals(enhancement.getString("Freitext"))) {
					actual = choiceTalent;
					break;
				}
			}
		} else {
			actual = (JSONObject) actualRep;
		}
		final Spell newSpell = Spell.getSpell(spellName, spell, actual, actualSpell, hero.getObj("Zauber"), rep);
		final SpellEnhancement result = new SpellEnhancement(newSpell, hero, true);
		if (enhancement.containsKey("Von")) {
			final int start = enhancement.getInt("Von");
			result.start.set(start < 0 ? start - 1 : start);
		} else {
			result.start.set(-1);
		}
		result.startString.set(getOfficial(result.start.get(), false));
		if (enhancement.containsKey("Auf")) {
			final int target = enhancement.getInt("Auf");
			result.setTarget(target < 0 ? target - 1 : target, hero, false);
		} else {
			result.setTarget(-1, hero, false);
		}
		result.ses.set(newSpell.getSes() + enhancement.getIntOrDefault("SEs", 0));
		result.method.set(enhancement.getString("Methode"));
		result.ap.set(enhancement.getInt("AP"));
		result.cost.set(enhancement.getDoubleOrDefault("Kosten", 0.0));
		result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateFormatter));
		result.updateDescription();
		return result;
	}

	public SpellEnhancement(final Spell spell, final JSONObject hero) {
		this(spell, hero, false);
	}

	public SpellEnhancement(final Spell spell, final JSONObject hero, final boolean fixed) {
		super(spell, "Zauber", hero, fixed);
	}

	@Override
	public SpellEnhancement clone(final JSONObject hero) {
		final SpellEnhancement result = new SpellEnhancement((Spell) talent, hero);
		result.start.set(start.get());
		result.startString.set(startString.get());
		result.setTarget(target.get(), hero, false);
		result.targetString.set(targetString.get());
		result.basis = basis;
		result.method.set(method.get());
		result.ses.set(ses.get());
		result.updateDescription();
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#getCalculatedCost(jsonant.value.JSONObject)
	 */
	@Override
	protected double getCalculatedCost(final JSONObject hero) {
		if (!Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten")) return 0;
		if (!"Lehrmeister".equals(method.get())) return 0;
		if (hasCustomAP)
			return ap.get() * 5;
		else {
			final int SELevel = start.get() + Math.min(target.get() - start.get(), ses.get());
			final int ap = DSAUtil.getEnhancementCost(talent, hero, "Lehrmeister", SELevel, Math.max(target.get(), SELevel),
					EnhancementController.usesChargenRules.get());
			return ap * 5;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#toJSON()
	 */
	@Override
	public JSONObject toJSON() {
		final JSONObject result = new JSONObject(null);
		result.put("Typ", "Zauber");
		result.put("Zauber", talent.getName());
		result.put("Repräsentation", ((Spell) talent).getRepresentation());
		if (talent.getTalent().containsKey("Auswahl")) {
			result.put("Auswahl", talent.getActual().getString("Auswahl"));
		}
		if (talent.getTalent().containsKey("Freitext")) {
			result.put("Freitext", talent.getActual().getString("Freitext"));
		}
		if (start.get() != -1) {
			result.put("Von", start.get() < 0 && !basis ? start.get() + 1 : start.get());
		}
		if (target.get() != -1) {
			result.put("Auf", target.get() < 0 && !basis ? target.get() + 1 : target.get());
		}
		final int resultSes = Math.min(ses.get(), target.get() - start.get());
		if (resultSes > 0) {
			result.put("SEs", resultSes);
		}
		result.put("Methode", method.get());
		result.put("AP", ap.get());
		if (Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten") && cost.get() != 0) {
			result.put("Kosten", cost.get());
		}
		final LocalDate currentDate = LocalDate.now();
		result.put("Datum", currentDate.toString());
		return result;
	}

	@Override
	protected void updateDescription() {
		description.set(talent.getDisplayName() + " (" + ((Spell) talent).getRepresentation() + ") (" + ((Spell) talent).getComplexity() + ") ("
				+ startString.get() + "->" + target.get() + ")");
	}
}
