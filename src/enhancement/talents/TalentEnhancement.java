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
import java.time.format.DateTimeFormatter;
import java.util.Stack;

import dsa41basis.hero.Talent;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsa41basis.util.RequirementsUtil;
import dsatool.resources.ResourceManager;
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
import jsonant.value.JSONValue;

public class TalentEnhancement extends Enhancement {
	private static boolean suppressUpdate = false;

	public static TalentEnhancement fromJSON(final JSONObject enhancement, final JSONObject hero) {
		final String talentName = enhancement.getString("Talent");
		final Tuple<JSONObject, String> talentAndGroup = HeroUtil.findTalent(talentName);
		final String groupName = talentAndGroup._2;
		final JSONObject talentGroups = ResourceManager.getResource("data/Talentgruppen");
		final Tuple<JSONValue, JSONObject> actualTalentAndGroup = HeroUtil.findActualTalent(hero, talentName);
		JSONObject actual = null;
		if (talentAndGroup._1.containsKey("Auswahl")) {
			for (int i = 0; i < actualTalentAndGroup._1.size(); ++i) {
				final JSONObject choiceTalent = ((JSONArray) actualTalentAndGroup._1).getObj(i);
				if (choiceTalent.getString("Auswahl").equals(enhancement.getString("Auswahl"))) {
					actual = choiceTalent;
					break;
				}
			}
		} else if (talentAndGroup._1.containsKey("Freitext")) {
			for (int i = 0; i < actualTalentAndGroup._1.size(); ++i) {
				final JSONObject choiceTalent = ((JSONArray) actualTalentAndGroup._1).getObj(i);
				if (choiceTalent.getString("Freitext").equals(enhancement.getString("Freitext"))) {
					actual = choiceTalent;
					break;
				}
			}
		} else {
			actual = (JSONObject) actualTalentAndGroup._1;
		}
		final Talent newTalent = Talent.getTalent(talentName, talentGroups.getObj(groupName), talentAndGroup._1, actual, actualTalentAndGroup._2);
		final TalentEnhancement result = new TalentEnhancement(newTalent, talentAndGroup._2, hero, true);
		final boolean basis = talentAndGroup._1.getBoolOrDefault("Basis", false);
		if (enhancement.containsKey("Von")) {
			final int start = enhancement.getInt("Von");
			result.start.set(start < 0 && !basis ? start - 1 : start);
		} else {
			result.start.set(-1);
		}
		result.startString.set(getOfficial(result.start.get(), basis));
		if (enhancement.containsKey("Auf")) {
			final int target = enhancement.getInt("Auf");
			result.setTarget(target < 0 && !basis ? target - 1 : target, hero);
		} else {
			result.setTarget(-1, hero);
		}
		result.ses.set(result.seMin + enhancement.getIntOrDefault("SEs", 0));
		result.method.set(enhancement.getString("Methode"));
		result.ap.set(enhancement.getInt("AP"));
		result.cost.set(enhancement.getDoubleOrDefault("Kosten", 0.0));
		result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu")));
		result.updateDescription();
		return result;
	}

	protected static int fromOfficial(final String taw, final boolean basis) {
		if ("n.a.".equals(taw)) return -1;
		int value = Integer.parseInt(taw);
		if (value < 0 && !basis) {
			value -= 1;
		}
		return value;
	}

	protected static String getOfficial(final int taw, final boolean basis) {
		if (taw == -1)
			return "n.a.";
		else if (taw < -1 && !basis)
			return String.valueOf(taw + 1);
		else
			return String.valueOf(taw);
	}

	protected final Talent talent;
	protected final IntegerProperty start;
	protected final StringProperty startString;
	protected final IntegerProperty target;
	protected final StringProperty targetString;
	protected boolean basis;
	protected final String talentGroupName;
	protected final StringProperty method;
	protected final IntegerProperty ses;
	protected int seMin;

	private final ChangeListener<Boolean> chargenListener;

	public TalentEnhancement(final Talent talent, final String talentGroupName, final JSONObject hero) {
		this(talent, talentGroupName, hero, false);
	}

	public TalentEnhancement(final Talent talent, final String talentGroupName, final JSONObject hero, final boolean fixed) {
		this.talent = talent;
		this.talentGroupName = talentGroupName;
		basis = talent.getTalent().getBoolOrDefault("Basis", false);
		final int value = fromStart(talent.getValue());
		startString = new SimpleStringProperty(getOfficial(value, basis));
		start = new SimpleIntegerProperty(value);
		target = new SimpleIntegerProperty(value + 1);
		targetString = new SimpleStringProperty(getOfficial(value + 1, basis));
		seMin = talent.getSes();
		ses = new SimpleIntegerProperty(seMin);
		fullDescription.bind(description);
		method = new SimpleStringProperty(Settings.getSettingStringOrDefault("Gegenseitiges Lehren", "Steigerung", "Lernmethode"));
		updateDescription();
		if (!fixed) {
			talent.valueProperty().addListener((o, oldV, newV) -> {
				if (!suppressUpdate) {
					final int newValue = fromStart(newV.intValue());
					final int difference = start.get() - newValue;
					start.set(newValue);
					setTarget(target.get() - difference, hero);
					startString.set(getOfficial(newValue, basis));
					updateDescription();
				}
			});
			talent.sesProperty().addListener((o, oldV, newV) -> {
				if (!suppressUpdate) {
					seMin = newV.intValue();
					ses.set(newV.intValue());
					ap.set(getCalculatedAP(hero));
					cost.set(getCalculatedCost(hero));
				}
			});
		}

		final Stack<Enhancement> enhancements = new Stack<>();
		for (final Enhancement e : EnhancementController.instance.getEnhancements()) {
			e.applyTemporarily(hero);
			enhancements.push(e);
		}
		ap.set(getCalculatedAP(hero));
		cost.set(getCalculatedCost(hero));
		recalculateValid(hero);
		for (final Enhancement e : enhancements) {
			e.unapply(hero);
		}

		cheaper.bind(ses.greaterThan(0));
		chargenListener = (o, oldV, newV) -> reset(hero);
		EnhancementController.usesChargenRules.addListener(chargenListener);
	}

	@Override
	public void apply(final JSONObject hero) {
		final int resultSes = Math.max(ses.get() - (target.get() - start.get()), 0);
		talent.insertTalent(true);
		talent.setValue(target.get());
		talent.setSes(resultSes);
	}

	@Override
	public void applyTemporarily(final JSONObject hero) {
		suppressUpdate = true;
		talent.insertTalent(true);
		talent.setValue(target.get());
		suppressUpdate = false;
	}

	@Override
	protected boolean calculateValid(final JSONObject hero) {
		final JSONObject talent = this.talent.getTalent();
		boolean valid = target.get() <= this.talent.getMaximum(hero);
		if (!talent.containsKey("Voraussetzungen")) return valid;
		final JSONArray requirements = talent.getArr("Voraussetzungen");
		for (int i = 0; i < requirements.size(); ++i) {
			final JSONObject requirement = requirements.getObj(i);
			if (!requirement.containsKey("Ab") || target.get() > requirement.getInt("Ab")) {
				valid = valid && RequirementsUtil.isRequirementFulfilled(hero, requirement, null, null, false);
			}
		}
		return valid;
	}

	public TalentEnhancement clone(final JSONObject hero) {
		final TalentEnhancement result = new TalentEnhancement(talent, talentGroupName, hero);
		result.start.set(start.get());
		result.startString.set(startString.get());
		result.setTarget(target.get(), hero);
		result.targetString.set(targetString.get());
		result.basis = basis;
		result.method.set(method.get());
		result.ses.set(ses.get());
		result.seMin = seMin;
		result.updateDescription();
		return result;
	}

	private int fromStart(final int start) {
		if (start == Integer.MIN_VALUE) return -1;
		if (start < 0 && !basis) return start - 1;
		return start;
	}

	@Override
	public int getCalculatedAP(final JSONObject hero) {
		int ap = 0;
		final String method = EnhancementController.usesChargenRules.get() ? "Gegenseitiges Lehren" : this.method.get();

		final int SELevel = start.get() + Math.min(target.get() - start.get(), ses.get());
		ap += DSAUtil.getEnhancementCost(talent, hero, "Lehrmeister", start.get(), Math.max(SELevel, start.get()),
				EnhancementController.usesChargenRules.get());
		ap += DSAUtil.getEnhancementCost(talent, hero, method, SELevel, Math.max(target.get(), SELevel),
				EnhancementController.usesChargenRules.get());
		return ap;
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
			return ap.get() * 7 / 10.0;
		else {
			final int SELevel = start.get() + Math.min(target.get() - start.get(), ses.get());
			final int ap = DSAUtil.getEnhancementCost(talent, hero, "Lehrmeister", SELevel, Math.max(target.get(), SELevel),
					EnhancementController.usesChargenRules.get());
			return ap * 7 / 10.0;
		}
	}

	public String getMethod() {
		return method.get();
	}

	@Override
	public String getName() {
		return talent.getName();
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

	public void setMethod(final String method, final JSONObject hero) {
		this.method.set(method);
		reset(hero);
	}

	public void setSes(final int ses, final JSONObject hero) {
		this.ses.set(ses);
		reset(hero);
	}

	public void setTarget(final int target, final JSONObject hero) {
		final Stack<Enhancement> enhancementStack = new Stack<>();
		for (final Enhancement e : EnhancementController.instance.getEnhancements()) {
			e.applyTemporarily(hero);
			enhancementStack.push(e);
		}

		this.target.set(target);
		targetString.set(getOfficial(target, basis));
		updateDescription();
		recalculateValid(hero);
		reset(hero);

		for (final Enhancement e : enhancementStack) {
			e.unapply(hero);
		}
	}

	public void setTarget(final String newValue, final JSONObject hero) {
		setTarget(fromOfficial(newValue, basis), hero);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#toJSON()
	 */
	@Override
	public JSONObject toJSON() {
		final JSONObject result = new JSONObject(null);
		result.put("Typ", "Talent");
		result.put("Talent", talent.getName());
		if (talent.getTalent().containsKey("Auswahl")) {
			result.put("Auswahl", talent.getActual().getString("Auswahl"));
		}
		if (talent.getTalent().containsKey("Freitext")) {
			result.put("Freitext", talent.getActual().getString("Freitext"));
		}
		if (start.get() != -1 || basis) {
			result.put("Von", start.get() < 0 && !basis ? start.get() + 1 : start.get());
		}
		if (target.get() != -1 || basis) {
			result.put("Auf", target.get() < 0 && !basis ? target.get() + 1 : target.get());
		}
		final int resultSes = ses.get() - (target.get() - start.get());
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
	public void unapply(final JSONObject hero) {
		suppressUpdate = true;
		int value = start.get();
		if (value < 0 && !basis) {
			if (value == -1) {
				value = Integer.MIN_VALUE;
			} else {
				++value;
			}
		}
		talent.setValue(value);
		talent.setSes(ses.get());
		if (value == Integer.MIN_VALUE) {
			talent.removeTalent();
		}
		suppressUpdate = false;
	}

	public void unregister() {
		EnhancementController.usesChargenRules.removeListener(chargenListener);
	}

	protected void updateDescription() {
		final String desc = talent.getDisplayName() + " (" + startString.get() + "->" + targetString.get() + ")";
		description.set(desc);
	}
}
