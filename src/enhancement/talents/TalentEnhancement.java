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

import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;

import dsa41basis.hero.Talent;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsa41basis.util.RequirementsUtil;
import dsatool.resources.Settings;
import dsatool.util.Tuple;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class TalentEnhancement extends Enhancement {
	protected final Talent talent;
	protected final IntegerProperty start;
	protected final StringProperty startString;
	protected final IntegerProperty target;
	private final StringProperty targetString;
	private final boolean basis;
	private final String talentGroupName;
	private final StringProperty method;
	private final IntegerProperty ses;
	private final int seMin;

	private final ChangeListener<Boolean> chargenListener;

	public TalentEnhancement(Talent talent, String talentGroupName, JSONObject hero) {
		this.talent = talent;
		this.talentGroupName = talentGroupName;
		int value = talent.getValue();
		if (value == Integer.MIN_VALUE) {
			value = -1;
			basis = false;
		} else if (value < 0 && !talent.getTalent().getBoolOrDefault("Basis", false)) {
			value -= 1;
			basis = false;
		} else {
			basis = true;
		}
		startString = new SimpleStringProperty(getOfficial(value));
		start = new SimpleIntegerProperty(value);
		target = new SimpleIntegerProperty(value + 1);
		targetString = new SimpleStringProperty(getOfficial(value + 1));
		seMin = talent.getSes();
		ses = new SimpleIntegerProperty(seMin);
		fullDescription.bind(description);
		method = new SimpleStringProperty(Settings.getSettingStringOrDefault("Gegenseitiges Lehren", "Steigerung", "Lernmethode"));
		updateDescription();
		cost.set(getCalculatedCost(hero));
		recalculateValid(hero);
		cheaper.bind(ses.greaterThan(0));
		chargenListener = (o, oldV, newV) -> resetCost(hero);
		EnhancementController.usesChargenRules.addListener(chargenListener);
	}

	@Override
	public void apply(JSONObject hero) {
		JSONObject actual = HeroUtil.findActualTalent(hero, talent.getName())._1;
		if (actual == null) {
			final JSONObject talentGroup = hero.getObj("Talente").getObj(talentGroupName);
			actual = new JSONObject(talentGroup);
			talentGroup.put(talent.getName(), actual);
		}
		actual.put("TaW", target.get());
		final int resultSes = ses.get() - (target.get() - start.get());
		if (resultSes <= 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", resultSes);
		}
		actual.notifyListeners(null);
	}

	@Override
	public void applyTemporarily(JSONObject hero) {
		JSONObject actual = HeroUtil.findActualTalent(hero, talent.getName())._1;
		if (actual == null) {
			final JSONObject talentGroup = hero.getObj("Talente").getObj(talentGroupName);
			actual = new JSONObject(talentGroup);
			actual.put("Temporär", true);
			talentGroup.put(talent.getName(), actual);
		}
		actual.put("TaW", target.get());
	}

	@Override
	protected boolean calculateValid(JSONObject hero) {
		final JSONObject talent = this.talent.getTalent();
		boolean valid = target.get() <= this.talent.getMaximum(hero);
		if (!talent.containsKey("Voraussetzungen")) return valid;
		final JSONArray requirements = talent.getArr("Voraussetzungen");
		for (int i = 0; i < requirements.size(); ++i) {
			final JSONObject requirement = requirements.getObj(i);
			if (!requirement.containsKey("Ab") || target.get() > requirement.getInt("Ab")) {
				valid = valid && RequirementsUtil.isRequirementFulfilled(hero, requirement, null, null);
			}
		}
		return valid;
	}

	public TalentEnhancement clone(JSONObject hero, Collection<Enhancement> enhancements) {
		final TalentEnhancement result = new TalentEnhancement(talent, talentGroupName, hero);
		result.setTarget(target.get(), hero, enhancements);
		return result;
	}

	private int fromOfficial(String taw) {
		if ("n.a.".equals(taw)) return -1;
		int value = Integer.parseInt(taw);
		if (value < 0 && !basis) {
			value -= 1;
		}
		return value;
	}

	@Override
	public int getCalculatedCost(JSONObject hero) {
		int modifier = 0;
		int maxLevel = 10;
		if (EnhancementController.usesChargenRules.get()) {
			final JSONObject pros = hero.getObj("Vorteile");
			if (pros.containsKey("Breitgefächerte Bildung") || pros.containsKey("Veteran")) {
				maxLevel = 15;
			}
			if (pros.containsKey("Akademische Ausbildung (Gelehrter)") || pros.containsKey("Akademische Ausbildung (Magier)")) {
				final String talentGroup = HeroUtil.findTalent(talent.getName())._2;
				if (Arrays.asList("Wissenstalente", "Sprachen und Schriften").contains(talentGroup)) {
					--modifier;

					final JSONArray proGroup = pros.getArrOrDefault("Begabung für Talentgruppe", null);
					final JSONArray proSingle = pros.getArrOrDefault("Begabung für Talent", null);
					if (proGroup != null) {
						for (int i = 0; i < proGroup.size(); ++i) {
							final JSONObject pro = proGroup.getObj(i);
							final String choice = pro.getString("Auswahl");
							if (talentGroup.equals(choice)) {
								++modifier;
								break;
							}
						}
					}
					if (modifier == -1 && proSingle != null) {
						for (int i = 0; i < proGroup.size(); ++i) {
							final JSONObject pro = proGroup.getObj(i);
							if (talent.getName().equals(pro.getString("Auswahl"))) {
								++modifier;
								break;
							}
						}
					}
				}
			}
			if (pros.containsKey("Akademische Ausbildung (Krieger)")) {
				final String talentGroup = HeroUtil.findTalent(talent.getName())._2;
				if (Arrays.asList("Nahkampftalente", "Fernkampftalente").contains(talentGroup)) {
					modifier -= 2;

					final JSONArray proGroup = pros.getArrOrDefault("Begabung für Talentgruppe", null);
					final JSONArray proSingle = pros.getArrOrDefault("Begabung für Talent", null);
					if (proGroup != null) {
						for (int i = 0; i < proGroup.size(); ++i) {
							final JSONObject pro = proGroup.getObj(i);
							final String choice = pro.getString("Auswahl");
							if (talentGroup.equals(choice) || "Kampftalente".equals(choice)) {
								++modifier;
								break;
							}
						}
					}
					if (modifier == -2 && proSingle != null) {
						for (int i = 0; i < proGroup.size(); ++i) {
							final JSONObject pro = proGroup.getObj(i);
							if (talent.getName().equals(pro.getString("Auswahl"))) {
								++modifier;
								break;
							}
						}
					}
				}
			}
		}

		final int SELevel = start.get() + Math.min(target.get() - start.get(), ses.get());
		int cost = 0;
		cost += DSAUtil.getEnhancementCost(talent, hero, "Lehrmeister", modifier, start.get(), Math.max(Math.min(maxLevel, SELevel), start.get()));
		if (maxLevel < SELevel) {
			cost += DSAUtil.getEnhancementCost(talent, hero, "Lehrmeister", 0, Math.max(maxLevel, start.get()), SELevel);
		}
		cost += DSAUtil.getEnhancementCost(talent, hero, method.get(), modifier, SELevel, Math.max(Math.min(maxLevel, target.get()), SELevel));
		if (maxLevel < target.get()) {
			cost += DSAUtil.getEnhancementCost(talent, hero, method.get(), 0, Math.max(maxLevel, SELevel), target.get());
		}
		return cost;
	}

	public String getMethod() {
		return method.get();
	}

	@Override
	public String getName() {
		return talent.getName();
	}

	private String getOfficial(int taw) {
		if (basis) return String.valueOf(taw);
		if (taw == -1)
			return "n.a.";
		else if (taw < -1)
			return String.valueOf(taw + 1);
		else
			return String.valueOf(taw);
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

	public Talent getTalent() {
		return talent;
	}

	public int getTarget() {
		return target.get();
	}

	public boolean isBasis() {
		return basis;
	}

	public StringProperty methodProperty() {
		return method;
	}

	public IntegerProperty sesProperty() {
		return ses;
	}

	public void setMethod(String method, JSONObject hero) {
		this.method.set(method);
		resetCost(hero);
	}

	public void setSes(int ses, JSONObject hero) {
		this.ses.set(ses);
		resetCost(hero);
	}

	public void setTarget(int target, JSONObject hero, Collection<Enhancement> enhancements) {
		final Stack<Enhancement> enhancementStack = new Stack<>();
		for (final Enhancement e : enhancements) {
			e.applyTemporarily(hero);
			enhancementStack.push(e);
		}

		this.target.set(target);
		targetString.set(getOfficial(target));
		updateDescription();
		recalculateValid(hero);
		resetCost(hero);

		for (final Enhancement e : enhancementStack) {
			e.unapply(hero);
		}
	}

	public void setTarget(String newValue, JSONObject hero, Collection<Enhancement> enhancements) {
		setTarget(fromOfficial(newValue), hero, enhancements);
	}

	public IntegerProperty startProperty() {
		return start;
	}

	public ReadOnlyStringProperty startStringProperty() {
		return startString;
	}

	public IntegerProperty targetProperty() {
		return target;
	}

	public ReadOnlyStringProperty targetStringProperty() {
		return targetString;
	}

	@Override
	public void unapply(JSONObject hero) {
		final Tuple<JSONObject, JSONObject> talentAndGroup = HeroUtil.findActualTalent(hero, talent.getName());
		final JSONObject actual = talentAndGroup._1;
		if (actual.getBoolOrDefault("Temporär", false)) {
			talentAndGroup._2.removeKey(talent.getName());
		} else {
			actual.put("TaW", start.get());
		}
	}

	public void unregister() {
		EnhancementController.usesChargenRules.removeListener(chargenListener);
	}

	protected void updateDescription() {
		final String desc = talent.getName() + " (" + startString.get() + "->" + targetString.get() + ")";
		description.set(desc);
	}
}
