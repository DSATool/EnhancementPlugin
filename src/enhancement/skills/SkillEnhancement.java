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
package enhancement.skills;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Stack;

import dsa41basis.hero.ProOrCon;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class SkillEnhancement extends Enhancement {

	public static SkillEnhancement fromJSON(final JSONObject enhancement, final JSONObject hero, final boolean planned) {
		final String skillName = enhancement.getString("Sonderfertigkeit");
		final JSONObject skill = HeroUtil.findSkill(skillName);
		final ProOrCon newSkill = new ProOrCon(skillName, hero, skill, new JSONObject(null));
		final SkillEnhancement result = new SkillEnhancement(newSkill, hero);
		if (skill.containsKey("Auswahl")) {
			newSkill.setDescription(enhancement.getString("Auswahl"), false);
			if (skill.containsKey("Freitext")) {
				newSkill.setVariant(enhancement.getString("Freitext"), false);
			}
		} else if (skill.containsKey("Freitext")) {
			newSkill.setDescription(enhancement.getString("Freitext"), false);
		}
		result.ap.set(enhancement.getInt("AP"));
		result.cost.set(enhancement.getDoubleOrDefault("Kosten", 0.0));
		if (!planned) {
			result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateFormatter));
		}
		result.updateDescription();
		return result;
	}

	private final ProOrCon skill;

	private final ChangeListener<Boolean> chargenListener;

	public SkillEnhancement(final ProOrCon skill, final JSONObject hero) {
		this.skill = skill;
		description.set(skill.getDisplayName());
		updateDescription();
		reset(hero);

		updateValid(hero);

		skill.descriptionProperty().addListener(o -> {
			updateDescription();
			updateValid(hero);
		});
		skill.variantProperty().addListener(o -> {
			updateDescription();
			updateValid(hero);
		});

		chargenListener = (o, oldV, newV) -> reset(hero);
		EnhancementController.usesChargenRules.addListener(chargenListener);
	}

	@Override
	public void apply(final JSONObject hero) {
		final JSONObject newSkill = applyInternal(hero);

		final JSONObject skill = this.skill.getProOrCon();
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		final String name = this.skill.getName();
		final boolean hasChoice = skill.containsKey("Auswahl");
		final boolean hasText = skill.containsKey("Freitext");

		newSkill.put("Kosten", ap.getValue());

		final JSONObject cheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");
		if (cheaperSkills.containsKey(name)) {
			if (hasChoice || hasText) {
				final JSONArray actualArray = cheaperSkills.getArr(name);
				for (int i = 0; i < actualArray.size(); ++i) {
					final JSONObject current = actualArray.getObj(i);
					if (!hasChoice || !current.containsKey("Auswahl") || current.getString("Auswahl").equals(newSkill.getString("Auswahl"))) {
						if (!hasText || !current.containsKey("Freitext") || current.getString("Freitext").equals(newSkill.getString("Freitext"))) {
							actualArray.removeAt(i);
							actualArray.notifyListeners(null);
							break;
						}
					}
				}
			} else {
				cheaperSkills.removeKey(name);
				cheaperSkills.notifyListeners(null);
			}
		}

		skills.notifyListeners(null);
	}

	private JSONObject applyInternal(final JSONObject hero) {
		final JSONObject actual = skill.getActual();
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		final JSONObject skill = this.skill.getProOrCon();
		final String name = this.skill.getName();
		JSONObject newSkill;
		if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
			JSONArray actualSkill;
			actualSkill = skills.getArr(name);
			newSkill = actual.clone(actualSkill);
			actualSkill.add(newSkill);

		} else {
			newSkill = actual.clone(skills);
			skills.put(name, newSkill);
		}
		HeroUtil.applyEffect(hero, name, skill, actual);
		return newSkill;
	}

	@Override
	public void applyTemporarily(final JSONObject hero) {
		applyInternal(hero);
	}

	@Override
	protected boolean calculateValid(final JSONObject hero) {
		return skill.getValid(false);
	}

	@Override
	public SkillEnhancement clone(final JSONObject hero, final Collection<Enhancement> enhancements) {
		return new SkillEnhancement(new ProOrCon(skill.getName(), hero, skill.getProOrCon(), skill.getActual().clone(null)), hero);
	}

	@Override
	protected int getCalculatedAP(final JSONObject hero) {
		cheaper.set(skill.getCost() < skill.getProOrCon().getIntOrDefault("Kosten", 0));
		return skill.getCost();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#getCalculatedCost(jsonant.value.JSONObject)
	 */
	@Override
	protected double getCalculatedCost(final JSONObject hero) {
		if (!Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten") || EnhancementController.usesChargenRules.get()) return 0;

		final JSONObject group = (JSONObject) skill.getProOrCon().getParent();
		if (group == ResourceManager.getResource("data/Sonderfertigkeiten").getObj("Magische Sonderfertigkeiten") ||
				group.getParent() == ResourceManager.getResource("data/Rituale") || group == ResourceManager.getResource("data/Schamanenrituale"))
			return ap.get() * 5;
		return ap.get() * 7 / 10.0;
	}

	@Override
	public String getInvalidReason(final JSONObject hero) {
		return skill.getInvalidReason(false);
	}

	@Override
	public String getName() {
		return skill.getName();
	}

	public ProOrCon getSkill() {
		return skill;
	}

	public StringProperty skillDescriptionProperty() {
		return skill.descriptionProperty();
	}

	public StringProperty skillVariantProperty() {
		return skill.variantProperty();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#toJSON()
	 */
	@Override
	public JSONObject toJSON(final JSONValue parent, final boolean planned) {
		final JSONObject result = new JSONObject(parent);
		result.put("Typ", "Sonderfertigkeit");
		result.put("Sonderfertigkeit", skill.getName());
		final JSONObject con = skill.getProOrCon();
		if (con.containsKey("Auswahl")) {
			result.put("Auswahl", skill.getActual().getString("Auswahl"));
		}
		if (con.containsKey("Freitext")) {
			result.put("Freitext", skill.getActual().getString("Freitext"));
		}
		result.put("AP", ap.get());
		if (Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten") && cost.get() != 0) {
			result.put("Kosten", cost.get());
		}
		if (!planned) {
			final LocalDate currentDate = LocalDate.now();
			result.put("Datum", currentDate.toString());
		}
		return result;
	}

	@Override
	public void unapply(final JSONObject hero) {
		unapplyTemporary(hero);

		final int ap = getAP();

		if (ap != skill.getCost()) {
			final JSONObject actual = skill.getActual();
			final JSONObject cheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");
			final JSONObject proOrCon = skill.getProOrCon();
			final String name = skill.getName();
			JSONObject newSkill;
			if (proOrCon.containsKey("Auswahl") || proOrCon.containsKey("Freitext")) {
				JSONArray actualSkill;
				actualSkill = cheaperSkills.getArr(name);
				newSkill = actual.clone(actualSkill);
				actualSkill.add(newSkill);
			} else {
				newSkill = actual.clone(cheaperSkills);
				cheaperSkills.put(name, newSkill);
			}

			final double numCheaper = ap == 0 ? Double.POSITIVE_INFINITY : Math.log(skill.getCost() / ap) / Math.log(2);
			if (numCheaper == (int) numCheaper) {
				if (numCheaper != 1) {
					newSkill.put("Verbilligungen", (int) numCheaper);
				}
			} else {
				newSkill.put("Kosten", ap);
			}
		}
	}

	@Override
	public void unapplyTemporary(final JSONObject hero) {
		final JSONObject actual = skill.getActual();
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		final JSONObject skill = this.skill.getProOrCon();
		final String name = this.skill.getName();
		if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
			JSONArray actualSkill;
			if (skills.containsKey(name)) {
				actualSkill = skills.getArr(name);
				for (final JSONObject current : actualSkill.getObjs()) {
					if ((!actual.containsKey("Auswahl") || actual.getString("Auswahl").equals(current.getString("Auswahl"))) &&
							(!actual.containsKey("Freitext") || actual.getString("Freitext").equals(current.getString("Freitext")))) {
						actualSkill.remove(current);
						if (actualSkill.size() == 0) {
							skills.removeKey(name);
						}
						break;
					}
				}
			}
		} else {
			skills.removeKey(name);
		}

		HeroUtil.unapplyEffect(hero, name, skill, actual);
	}

	public void unregister() {
		EnhancementController.usesChargenRules.removeListener(chargenListener);
	}

	private void updateDescription() {
		fullDescription.set(DSAUtil.printProOrCon(skill.getActual(), skill.getDisplayName(), skill.getProOrCon(), false));
		cheaper.set(skill.getCost() < skill.getProOrCon().getIntOrDefault("Kosten", 0));
	}

	private void updateValid(final JSONObject hero) {
		final Stack<Enhancement> enhancements = new Stack<>();
		for (final Enhancement e : EnhancementController.instance.getEnhancements()) {
			e.applyTemporarily(hero);
			enhancements.push(e);
		}
		recalculateValid(hero);
		for (final Enhancement e : enhancements) {
			e.unapplyTemporary(hero);
		}
	}
}
