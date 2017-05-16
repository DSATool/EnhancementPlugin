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

import dsa41basis.hero.ProOrCon;
import dsa41basis.hero.ProOrCon.ChoiceOrTextEnum;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsa41basis.util.RequirementsUtil;
import enhancement.enhancements.Enhancement;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class SkillEnhancement extends Enhancement {
	private final ProOrCon skill;

	public SkillEnhancement(ProOrCon skill, JSONObject hero) {
		this.skill = skill;
		description.set(skill.getDisplayName());
		updateDescription();
		recalculateValid(hero);
		skill.descriptionProperty().addListener(o -> updateDescription());
		skill.variantProperty().addListener(o -> updateDescription());
	}

	@Override
	public void apply(JSONObject hero) {
		final JSONObject newSkill = applyInternal(hero);

		final JSONObject skill = this.skill.getProOrCon();
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		final String name = this.skill.getName();
		final boolean hasChoice = skill.containsKey("Auswahl");
		final boolean hasText = skill.containsKey("Freitext");

		newSkill.put("Kosten", cost.getValue());

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

	private JSONObject applyInternal(JSONObject hero) {
		final JSONObject actual = skill.getActual();
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		final JSONObject skill = this.skill.getProOrCon();
		final String name = this.skill.getName();
		JSONObject newSkill;
		if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
			JSONArray actualSkill;
			if (skills.containsKey(name)) {
				actualSkill = skills.getArr(name);
			} else {
				actualSkill = new JSONArray(skills);
			}
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
	public void applyTemporarily(JSONObject hero) {
		applyInternal(hero);
	}

	@Override
	protected boolean calculateValid(JSONObject hero) {
		final JSONObject actualSkill = skill.getProOrCon();
		if (!actualSkill.containsKey("Voraussetzungen")) return true;
		final String choice = skill.firstChoiceOrText() == ChoiceOrTextEnum.CHOICE ? skill.getDescription() : null;
		final String text = skill.firstChoiceOrText() == ChoiceOrTextEnum.TEXT ? skill.getDescription()
				: skill.secondChoiceOrText() == ChoiceOrTextEnum.TEXT ? skill.getVariant() : null;
		return RequirementsUtil.isRequirementFulfilled(hero, actualSkill.getObj("Voraussetzungen"), choice, text);
	}

	public SkillEnhancement clone(JSONObject hero) {
		return new SkillEnhancement(new ProOrCon(skill.getName(), hero, skill.getProOrCon(), skill.getActual().clone(null)), hero);
	}

	@Override
	protected int getCalculatedCost(JSONObject hero) {
		cheaper.set(skill.getCost() < skill.getProOrCon().getIntOrDefault("Kosten", 0));
		return skill.getCost();
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

	@Override
	public void unapply(JSONObject hero) {
		final JSONObject actual = skill.getActual();
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		final JSONObject skill = this.skill.getProOrCon();
		final String name = this.skill.getName();
		if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
			JSONArray actualSkill;
			if (skills.containsKey(name)) {
				actualSkill = skills.getArr(name);
			} else {
				actualSkill = new JSONArray(skills);
			}
			actualSkill.remove(actual.clone(actualSkill));
			if (actualSkill.size() == 0) {
				skills.removeKey(name);
			}
		} else {
			skills.removeKey(name);
		}
		HeroUtil.unapplyEffect(hero, name, skill, actual);
	}

	private void updateDescription() {
		fullDescription.set(DSAUtil.printProOrCon(skill.getActual(), skill.getDisplayName(), skill.getProOrCon(), false));
		cheaper.set(skill.getCost() < skill.getProOrCon().getIntOrDefault("Kosten", 0));
		cost.set(skill.getCost());
	}
}
