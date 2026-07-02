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
package enhancement.education;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import dsa41basis.hero.ProOrCon;
import dsa41basis.ui.hero.MagicAnimalBindingDialog;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsa41basis.util.RKPUtil;
import dsa41basis.util.RequirementsUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.ui.ReactiveComboBox;
import dsatool.util.ErrorLogger;
import dsatool.util.StringUtil;
import dsatool.util.Tuple;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class EducationEnhancement extends Enhancement {

	public static EducationEnhancement fromJSON(final JSONObject enhancement, final JSONObject hero) {
		final String name = enhancement.getString("Art");
		final JSONObject education = ResourceManager.getResource("data/Weiterbildung").getObj(name);
		final ProOrCon newEducation = new ProOrCon(name, hero, education, new JSONObject(null));
		final EducationEnhancement result = new EducationEnhancement(newEducation, hero);
		if (enhancement.containsKey("Auswahl")) {
			newEducation.setDescription(enhancement.getString("Auswahl"), false);
		}
		if (enhancement.containsKey("Freitext")) {
			newEducation.setVariant(enhancement.getString("Freitext"), false);
		}
		if (enhancement.containsKey("Abgestuft")) {
			newEducation.setVariant(enhancement.getIntOrDefault("TaP*", 0).toString(), false);
		}
		if (enhancement.containsKey("Effekte")) {
			result.effects = enhancement.getObj("Effekte");
		}
		result.ap.set(enhancement.getInt("AP"));
		result.cost.set(enhancement.getDoubleOrDefault("Kosten", 0.0));
		result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateFormatter));
		return result;
	}

	private static int getLiturgyGrade(final String liturgyName, final JSONObject liturgies, final String goddess) {
		final JSONObject liturgy = liturgies.getObj(liturgyName);
		final JSONObject actualLiturgy = liturgy.getObj("Gottheiten").getObj(goddess);
		final boolean primaryLiturgies = Settings.getSettingBoolOrDefault(true, "Geweihte", "Primäre Segnungen");
		return Math.max(actualLiturgy.getIntOrDefault("Grad", liturgy.getIntOrDefault("Grad", 1)), primaryLiturgies ? 0 : 1);
	}

	private static String liturgyToDisplayString(final String liturgyName, final JSONObject liturgies, final String goddess) {
		final JSONObject actualLiturgy = liturgies.getObj(liturgyName).getObj("Gottheiten").getObj(goddess);
		return actualLiturgy.getStringOrDefault("Name", liturgyName) + " (Grad "
				+ DSAUtil.romanNumeral(getLiturgyGrade(liturgyName, liturgies, goddess)) + ")";
	}

	private final ProOrCon education;
	private final Collection<String> choices;

	private final Collection<String> variants;

	private JSONObject effects;

	public EducationEnhancement(final ProOrCon education, final JSONObject hero) {
		this.education = education;
		fullDescription.bind(description);
		description.set(education.getDisplayName());
		choices = switch (education.getName()) {
			case "Große Meditation" -> hero.getObj("Talente").getObjOrDefault("Ritualkenntnis", new JSONObject(null)).keySet();
			case "Karmalqueste" -> hero.getObj("Talente").getObjOrDefault("Liturgiekenntnis", new JSONObject(null)).keySet();
			case "Kontakt zum Großen Geist" -> Collections.emptyList();
			case "Spätweihe" -> {
				final ArrayList<String> clerics = new ArrayList<>();
				DSAUtil.foreach(p -> maybeCleric(p), (name, _) -> {
					clerics.add(name);
				}, ResourceManager.getResource("data/Professionen"));
				yield clerics;
			}
			case "Zweitstudium" -> {
				final JSONObject magicians = ResourceManager.getResource("data/Professionen").getObjOrDefault("Magier", new JSONObject(null))
						.getObjOrDefault("Varianten", new JSONObject(null)).clone(null);
				yield magicians.keySet();
			}
			default -> education.getProOrCon().containsKey("Auswahl") || education.getProOrCon().containsKey("Freitext") ? education.getFirstChoiceItems(false)
					: Collections.emptyList();
		};
		if (choices != null && !choices.isEmpty()) {
			education.setDescription(choices.iterator().next(), false);
		}
		variants = getPossibleVariants(education.getDescription());

		if (education.getProOrCon().getBoolOrDefault("Abgestuft", false)) {
			education.setVariant("0", false);
		} else if (variants != null && !variants.isEmpty()) {
			education.setVariant(variants.iterator().next(), false);
		}
		reset(hero);
		recalculateValid(hero);
	}

	@Override
	public void apply(final JSONObject hero) {
		final String name = getName();
		getEffects(name, hero);
		final JSONObject actual = getApplicationActual(hero);
		HeroUtil.applyEffect(hero, name, effects, actual);
	}

	@Override
	public void applyTemporarily(final JSONObject hero) {
		/* We don't apply these temporarily because of their large effects */
	}

	@Override
	protected boolean calculateValid(final JSONObject hero) {
		if ("Zweitstudium".equals(description.get())) {
			if (!violatedTeachingLanguageRequirements(hero).isEmpty())
				return false;
		}
		return education.getValid(false);
	}

	public StringProperty choiceProperty() {
		return education.descriptionProperty();
	}

	@Override
	public Enhancement clone(final JSONObject hero, final Collection<Enhancement> enhancements) {
		final EducationEnhancement result = new EducationEnhancement(new ProOrCon(education.getName(), hero, education.getProOrCon(), new JSONObject(null)),
				hero);
		result.setChoice(education.descriptionProperty().get());
		result.education.variantProperty().set(education.variantProperty().get());
		result.setCost(cost.get());
		result.setAP(ap.get(), hero);
		return result;
	}

	private boolean collectProfessionEffects(final JSONObject base, final JSONObject target, final Collection<String> keys) {
		JSONObject profession = base;
		final Tuple<JSONObject, Integer> professionVariant = getProfessionVariant(profession);
		profession = professionVariant._1;
		int i = professionVariant._2;

		String[] chosenVariants = null;
		if (!variants.isEmpty()) {
			chosenVariants = education.getVariant().split(":");
		}

		for (final String key : keys) {
			RKPUtil.collectObj(profession, key, target);
		}
		boolean generation = RKPUtil.getBool(profession, "Generierung", true);

		if (!variants.isEmpty()) {
			for (; i < chosenVariants.length; ++i) {
				final String variant = chosenVariants[i];
				while (!profession.getObj("Varianten").containsKey(variant)) {
					profession = (JSONObject) profession.getParent().getParent();
				}
				profession = profession.getObj("Varianten").getObj(variant);
				for (final String key : keys) {
					RKPUtil.collectVariantObj(profession, key, target);
				}
				if (!profession.getBoolOrDefault("Generierung", true)) {
					generation = false;
				}
			}
		}

		return generation;
	}

	private JSONObject getApplicationActual(final JSONObject hero) {
		return switch (description.get()) {
			case "Karmalqueste", "Spätweihe", "Kontakt zum Großen Geist" -> {
				final JSONObject professionVariant = getProfessionVariant(
						ResourceManager.getResource("data/Professionen").getObj(education.getDescription()))._1;
				final JSONObject temp = new JSONObject(null);
				RKPUtil.collectObj(professionVariant, "Vorteile", temp);
				RKPUtil.collectObj(professionVariant, "Sonderfertigkeiten", temp);
				final JSONObject result = new JSONObject(null);
				result.put("Auswahl", getGoddess(temp, hero));
				yield result;
			}
			default -> new JSONObject(null);
		};
	}

	private int getAttribute(final JSONObject hero, final String attribute) {
		return hero.getObj("Eigenschaften").getObj(attribute).getIntOrDefault("Wert", 0);
	}

	private JSONObject getBaseValueEffect(final String baseValueName, final int effect) {
		final JSONObject result = new JSONObject(null);

		final JSONObject effects = new JSONObject(result);
		result.put("Effekte", effects);

		final JSONObject baseValues = new JSONObject(effects);
		effects.put("Basiswerte", baseValues);

		baseValues.put(baseValueName, effect);

		return result;
	}

	@Override
	protected int getCalculatedAP(final JSONObject hero) {
		switch (description.get()) {
			case "Zweitstudium":
				if ("Zweitstudium".equals(description.get())) {
					JSONObject profession = ResourceManager.getResource("data/Professionen").getObjOrDefault("Magier", new JSONObject(null))
							.getObjOrDefault("Varianten", new JSONObject(null)).getObjOrDefault(education.getDescription(), new JSONObject(null));
					int cost = profession.getIntOrDefault("Kosten", 0);
					if (!variants.isEmpty()) {
						final String[] chosenVariants = education.getVariant().split(":");
						for (int i = 0; i < chosenVariants.length; ++i) {
							final String variant = chosenVariants[i];
							final JSONObject currentVariants = profession.getObjOrDefault("Varianten", new JSONObject(null));
							if (currentVariants.containsKey(variant)) {
								final JSONObject currentVariant = currentVariants.getObj(variant);
								if (!currentVariant.getBoolOrDefault("kombinierbar", false)) {
									profession = currentVariant;
									if (profession.containsKey("Kosten")) {
										cost = profession.getInt("Kosten");
									}
								} else {
									if (currentVariant.containsKey("Kosten")) {
										cost += currentVariant.getInt("Kosten");
									}
								}
							} else {
								profession = (JSONObject) profession.getParent().getParent();
								--i;
							}
						}
					}
					final int factor = isSameGuild(hero) ? 25 : 30;
					return factor * cost;
				}
			case "Spätweihe", "Kontakt zum Großen Geist":
				final String profession = education.getDescription();
				final JSONObject professionVariant = getProfessionVariant(
						ResourceManager.getResource("data/Professionen").getObj(education.getDescription()))._1;
				final JSONObject temp = new JSONObject(null);
				RKPUtil.collectObj(professionVariant, "Vorteile", temp);
				RKPUtil.collectObj(professionVariant, "Sonderfertigkeiten", temp);
				education.setDescription(getGoddess(temp, hero), false);
				final int cost = education.getCost();
				education.setDescription(profession, false);
				return cost;
			default:
				return education.getCost();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#getCalculatedCost(jsonant.value.JSONObject)
	 */
	@Override
	protected double getCalculatedCost(final JSONObject hero) {
		if ("Zweitstudium".equals(description.get()) && !isSameGuild(hero)) return education.getProOrCon().getDoubleOrDefault("Lehrmeisterkosten", 0.0) * 1.5;
		return education.getProOrCon().getDoubleOrDefault("Lehrmeisterkosten", 0.0);
	}

	public Collection<String> getChoices() {
		return choices;
	}

	public ProOrCon getEducation() {
		return education;
	}

	private void getEffects(final String name, final JSONObject hero) {
		final JSONObject actual = education.getProOrCon();
		effects = switch (name) {
			case "Große Meditation":
				final String ritualKnowledge = education.descriptionProperty().get();
				final String mainAttribute = ResourceManager.getResource("data/Talente").getObj("Ritualkenntnis").getObj(ritualKnowledge)
						.getString("Leiteigenschaft");
				final int mainAttributeValue = Math.max(getAttribute(hero, mainAttribute),
						hero.getObj("Sonderfertigkeiten").containsKey("Gefäß der Sterne") ? getAttribute(hero, "CH") : 0);
				int asp = (int) Math.round(mainAttributeValue / 3.0);
				asp += (int) Math.round(Integer.parseInt(education.variantProperty().get()) / 10.0);
				yield getBaseValueEffect("Astralenergie", asp);
			case "Karmalqueste":
				int kap = (int) Math.round(getAttribute(hero, "IN") / (DSAUtil.isAlveranGod(education.getDescription()) ? 4.0 : 5.0));
				kap += (int) Math.round(Integer.parseInt(education.variantProperty().get()) / 10.0);
				yield getBaseValueEffect("Karmaenergie", kap);
			case "Spätweihe":
				yield getLateConsecrationEffects(hero);
			case "Zweitstudium":
				yield getSecondStudyEffects(hero);
			case "Bindung eines Vertrautentiers":
				showAnimalBindingDialog(hero);
			case "Kontakt zum Großen Geist":
			default:
				final JSONObject result = new JSONObject(null);
				result.put("Effekte", actual.getObj("Effekte").clone(result));
				if ("Kontakt zum Großen Geist".equals(name)) {
					final JSONObject prosConsSkills = result.getObj("Vorteile/Nachteile/Sonderfertigkeiten");
					showLiturgyChoiceDialog(getGoddess(new JSONObject(null), hero), prosConsSkills);
				}
				yield result;
		};
	}

	private String getGoddess(final JSONObject effects, final JSONObject hero) {
		final JSONArray consecratedPro = effects.getObj("Vorteile").getArrOrDefault("Geweiht", null);
		if (consecratedPro != null) return consecratedPro.getObj(0).getString("Auswahl");
		final JSONArray lateConsecration = effects.getObj("Sonderfertigkeiten").getArrOrDefault("Spätweihe", null);
		if (lateConsecration != null)
			return lateConsecration.getObj(0).getString("Auswahl");
		final String profession = hero.getObj("Biografie").getString("Profession");
		final String bgb = hero.getObj("Vorteile").getObjOrDefault("Breitgefächerte Bildung", new JSONObject(null)).getString("Profession");
		if ("Medizinmann".equals(profession) || "Medizinmann".equals(bgb))
			return "Kamaluq";
		else if ("Kasknuk".equals(profession) || "Kasknuk".equals(bgb))
			return "Himmelswölfe";
		else if ("Tairach-Priester".equals(profession) || "Tairach-Priester".equals(bgb))
			return "Tairach";
		return null;
	}

	@Override
	public String getInvalidReason(final JSONObject hero) {
		String unfulfilled = education.getInvalidReason(false);
		if ("Zweitstudium".equals(description.get())) {
			final Collection<JSONArray> violated = violatedTeachingLanguageRequirements(hero);
			for (final JSONArray currentViolation : violated) {
				unfulfilled += "\n" + StringUtil.mkStringString(currentViolation, " o. ", talent -> talent + "≥10");
			}
			if (unfulfilled.startsWith("\n")) {
				unfulfilled = unfulfilled.substring(1);
			}
		}
		return unfulfilled;
	}

	private JSONObject getLateConsecrationEffects(final JSONObject hero) {
		final JSONObject result = new JSONObject(null);
		final JSONObject effects = education.getProOrCon().getObj("Effekte").clone(result);
		result.put("Effekte", effects);

		final JSONObject temp = new JSONObject(null);
		final boolean generation = collectProfessionEffects(ResourceManager.getResource("data/Professionen").getObj(education.getDescription()), temp,
				List.of("Basiswerte", "Talente", "Vorteile", "Nachteile", "Sonderfertigkeiten"));

		final String goddess = getGoddess(temp, hero);

		final JSONObject baseValueEffects = effects.getObj("Basiswerte");

		if (DSAUtil.isAlveranGod(goddess)) {
			baseValueEffects.put("Karmaenergie", 12);
		}

		final JSONObject baseValues = temp.getObj("Basiswerte");

		final JSONObject prosConsSkills = effects.getObj("Vorteile/Nachteile/Sonderfertigkeiten");

		if (generation) {
			showLiturgyChoiceDialog(goddess, prosConsSkills);
		} else {
			final JSONObject talentEffects = effects.getObj("Talente");
			final JSONObject ses = effects.getObj("Spezielle Erfahrungen");
			final JSONObject talents = temp.getObj("Talente");
			for (final String talentName : talents.keySet()) {
				final int effect = talents.getInt(talentName);
				final int taw = ((JSONObject) HeroUtil.findActualTalent(hero, talentName)._1).getIntOrDefault("TaW", 0);
				if (taw < 10) {
					talentEffects.put(talentName, Math.min(effect, 10 - taw));
				}
				if (taw + effect > 10) {
					ses.put(talentName, taw + effect - 10);
				}
			}

			for (final String kind : List.of("Vorteile", "Nachteile", "Sonderfertigkeiten")) {
				final JSONObject current = temp.getObj(kind);
				for (final String name : current.keySet()) {
					final Object proConSkill = current.getUnsafe(name);
					if (proConSkill instanceof final JSONArray proConSkillArray) {
						prosConsSkills.put(name, proConSkillArray.clone(prosConsSkills));
					} else {
						prosConsSkills.put(name, ((JSONObject) proConSkill).clone(prosConsSkills));
					}
				}
			}
			for (final String name : baseValues.keySet()) {
				baseValueEffects.put(name, baseValues.getInt(name));
			}
		}

		final int currentSO = HeroUtil.getCurrentValue(hero.getObj("Basiswerte").getObj("Sozialstatus"), false);
		final int minSO = baseValues.getIntOrDefault("Sozialstatus", 0);

		if (currentSO < minSO - 1) {
			baseValueEffects.put("Sozialstatus", minSO - currentSO - 1);

		}

		return result;
	}

	@Override
	public String getName() {
		return education.getName();
	}

	private Collection<String> getPossibleVariants(final String choice) {
		return switch (education.getName()) {
			case "Große Meditation" -> null;
			case "Karmalqueste" -> null;
			case "Kontakt zum Großen Geist" -> null;
			case "Spätweihe" -> {
				final JSONObject profession = ResourceManager.getResource("data/Professionen").getObj(choice);
				yield getPossibleVariants(choice, profession, true, isCleric(profession));
			}
			case "Zweitstudium" -> {
				final JSONObject profession = ResourceManager.getResource("data/Professionen").getObjOrDefault("Magier", new JSONObject(null))
						.getObjOrDefault("Varianten", new JSONObject(null)).getObjOrDefault(choice, new JSONObject(null));
				if (!profession.containsKey("Varianten")) {
					yield null;
				} else {

					yield getPossibleVariants(choice, profession, false, false);
				}
			}
			default -> education.getProOrCon().containsKey("Auswahl") && education.getProOrCon().containsKey("Freitext")
					? education.getSecondChoiceItems(false) : null;
		};
	}

	private ArrayList<String> getPossibleVariants(final String choice, final JSONObject profession, final boolean filterCleric, final boolean isCleric) {
		final JSONObject variants = profession.getObj("Varianten");
		final ArrayList<String> result = new ArrayList<>();
		final ArrayList<String> combinable = new ArrayList<>();

		for (final String variantName : variants.keySet()) {
			final JSONObject variant = variants.getObj(variantName);
			if (variant.getBoolOrDefault("kombinierbar", false)) {
				combinable.add(variantName);
			} else if (!filterCleric || maybeCleric(variant) || isCleric && !variant.containsKey("Vorteile")) {
				final Collection<String> subVariants = getPossibleVariants(variantName, variant, filterCleric,
						isCleric && !variant.containsKey("Vorteile") || isCleric(variant));
				if (subVariants.isEmpty()) {
					result.add(variantName);
				} else {
					for (final String subVariant : subVariants) {
						if (subVariant.equals(variantName)) {
							result.add(variantName);
						} else {
							result.add(variantName + ":" + subVariant);
						}
					}
				}
			}
		}

		if (!combinable.isEmpty()) {
			@SuppressWarnings("unchecked")
			final ArrayList<String> baseVariants = (ArrayList<String>) result.clone();
			if (!result.contains(choice)) {
				baseVariants.add(0, choice);
				if (!filterCleric || isCleric) {
					result.add(0, choice);
				}
			}
			for (final String base : baseVariants) {
				for (final String variant : combinable) {
					if (base.equals(choice)) {
						result.add(variant);
					} else {
						result.add(base + ":" + variant);
					}
				}
			}
		}

		return result;
	}

	private Tuple<JSONObject, Integer> getProfessionVariant(final JSONObject base) {
		JSONObject profession = base;
		int i = 0;
		String[] chosenVariants = null;
		if (variants != null && !variants.isEmpty()) {
			chosenVariants = education.getVariant().split(":");
			for (i = 0; i < chosenVariants.length; ++i) {
				final String variant = chosenVariants[i];
				final JSONObject currentVariants = profession.getObjOrDefault("Varianten", new JSONObject(null));
				if (currentVariants.containsKey(variant)) {
					final JSONObject currentVariant = currentVariants.getObj(variant);
					if (currentVariant.getBoolOrDefault("kombinierbar", false)) {
						break;
					}
					profession = currentVariant;
				} else {
					break;
				}
			}
		}

		return new Tuple<>(profession, i);
	}

	private JSONObject getSecondStudyEffects(final JSONObject hero) {
		final JSONObject profession = ResourceManager.getResource("data/Professionen").getObjOrDefault("Magier", new JSONObject(null))
				.getObjOrDefault("Varianten", new JSONObject(null)).getObjOrDefault(education.getDescription(), new JSONObject(null));

		final JSONObject result = new JSONObject(null);
		final JSONObject effects = education.getProOrCon().getObj("Effekte").clone(result);
		result.put("Effekte", effects);

		final boolean generation = collectProfessionEffects(profession, effects,
				List.of("Talente", "Hauszauber", "Zauber", "Sonderfertigkeiten", "Verbilligte Sonderfertigkeiten"));

		final JSONObject ses = effects.getObj("Spezielle Erfahrungen");

		final JSONObject talents = effects.getObj("Talente");
		effects.removeKey("Talente");
		for (final String talent : talents.keySet()) {
			if ("Wahl".equals(talent)) {
				// Handled later
			} else if (!generation || "Wissenstalente".equals(HeroUtil.findTalent(talent)._2)) {
				final Object actual = talents.getUnsafe(talent);
				if (actual instanceof final JSONArray actualArray) {
					ses.put(talent, new JSONArray(new ArrayList<>(List.ofLazy(actualArray.size(), _ -> 1)), ses));
				} else {
					ses.put(talent, 1);
				}
			}
		}
		if (talents.containsKey("Wahl")) {
			final JSONArray choiceArray = talents.getArr("Wahl");
			for (final JSONObject choiceObj : choiceArray.getObjs()) {
				JSONArray choices;
				int numSEs = 0;
				if (choiceObj.containsKey("Oder")) {
					choices = new JSONArray(null);
					for (final JSONObject group : choiceObj.getArr("Oder").getObjs()) {
						for (final String talentName : group.getArr("Wahl").getStrings())
							if (!choices.contains(talentName)) {
								choices.add(talentName);
							}
						numSEs = Math.max(numSEs, group.getArr("Werte").size());
					}
				} else {
					choices = choiceObj.getArr("Wahl");
					numSEs = choiceObj.getArr("Werte").size();
				}
				final List<String> remaining = new ArrayList<>(choices.size());
				for (final String talentName : choices.getStrings()) {
					if (!generation || "Wissenstalente".equals(HeroUtil.findTalent(talentName)._2)) {
						if ("Fremdsprache".equals(talentName)) {
							final JSONObject langs = ResourceManager.getResource("data/Talente").getObj("Sprachen und Schriften");
							for (final String lang : langs.keySet()) {
								if (!langs.getObj(lang).getBoolOrDefault("Schrift", false)) {
									remaining.add(lang);
								}
							}
						} else if ("Fremdschrift".equals(talentName)) {
							final JSONObject langs = ResourceManager.getResource("data/Talente").getObj("Sprachen und Schriften");
							for (final String lang : langs.keySet()) {
								if (langs.getObj(lang).getBoolOrDefault("Schrift", false)) {
									remaining.add(lang);
								}
							}
						} else {
							remaining.add(talentName);
						}
					}
				}
				remaining.removeAll(ses.keySet());
				final Collection<String> chosen = numSEs >= remaining.size() ? remaining : showTalentChoiceDialog(remaining, numSEs, ses);
				for (final String talentName : chosen) {
					ses.put(talentName, 1);
				}
			}
		}

		final JSONObject houseSpells = effects.getObj("Hauszauber");
		effects.removeKey("Hauszauber");
		final JSONObject newHouseSpells = effects.getObj("Hauszauber");
		getSpellSEs(ses, houseSpells, newHouseSpells);

		final Set<String> spellsToRemove = new HashSet<>();
		final JSONObject actualSpells = hero.getObj("Zauber");
		for (final String spellName : newHouseSpells.keySet()) {
			if (actualSpells.containsKey(spellName)) {
				final JSONObject actualSpell = actualSpells.getObj(spellName);
				final JSONObject spell = newHouseSpells.getObj(spellName);
				for (final String representation : spell.keySet()) {
					if (actualSpell.containsKey(representation)) {
						final Object actualRepresentation = actualSpell.getUnsafe(representation);
						if (actualRepresentation instanceof final JSONObject spellObj) {
							if (spellObj.getBoolOrDefault("Hauszauber", false)) {
								spellsToRemove.add(spellName);
							}
						}
					}
				}
			}
		}
		for (final String spellName : spellsToRemove) {
			newHouseSpells.removeKey(spellName);
		}

		final JSONObject spells = effects.getObj("Zauber");
		effects.removeKey("Zauber");
		getSpellSEs(ses, spells, null);

		if (ses.size() == 0) {
			effects.removeKey("Spezielle Erfahrungen");
		}

		final JSONObject actualSkills = hero.getObj("Sonderfertigkeiten");

		final JSONObject skills = effects.getObj("Sonderfertigkeiten");
		final JSONObject cheaper = effects.getObj("Verbilligte Sonderfertigkeiten");

		final Set<String> traits = new HashSet<>();
		for (final JSONObject knownTrait : actualSkills.getArrOrDefault("Merkmalskenntnis", new JSONArray(null)).getObjs()) {
			traits.add(knownTrait.getString("Auswahl"));
		}

		final Set<String> traitKnowledgeOptions = new HashSet<>();
		JSONArray traitKnowledge = null;
		final JSONArray traitKnowledgeChoices = new JSONArray(null);
		if (skills.containsKey("Merkmalskenntnis")) {
			traitKnowledge = skills.getArr("Merkmalskenntnis");
			skills.removeKey("Merkmalskenntnis");
			for (final JSONObject variant : traitKnowledge.getObjs()) {
				traitKnowledgeOptions.add(variant.getString("Auswahl"));
			}
		}
		if (skills.containsKey("Wahl")) {
			final JSONArray choices = skills.getArr("Wahl");
			for (final JSONObject choice : choices.getObjs()) {
				if (choice.containsKey("Merkmalskenntnis")) {
					final JSONArray traitKnowledgeChoice = choice.getArr("Merkmalskenntnis");
					traitKnowledgeChoices.add(traitKnowledgeChoice.clone(traitKnowledgeChoices));
					choice.removeKey("Merkmalskenntnis");
					for (final JSONObject variant : traitKnowledgeChoice.getObjs()) {
						traitKnowledgeOptions.add(variant.getString("Auswahl"));
					}
				}
			}
		}
		traitKnowledgeOptions.removeAll(traits);
		final String chosenTraitKnowledge = traitKnowledgeOptions.size() > 1
				? showTraitKnowledgeChoiceDialog(traitKnowledgeOptions, traitKnowledge, traitKnowledgeChoices, skills, traits)
				: traitKnowledgeOptions.isEmpty() ? null : traitKnowledgeOptions.iterator().next();

		effects.removeKey("Verbilligte Sonderfertigkeiten");
		for (final String skillName : cheaper.keySet()) {
			if ("Wahl".equals(skillName)) {
				final JSONArray choices = effects.getObj("Sonderfertigkeiten").getArr("Wahl");
				for (final JSONObject choice : cheaper.getArr("Wahl").getObjs()) {
					choices.add(choice.clone(choices));
				}
			} else {
				final Object skill = cheaper.getUnsafe(skillName);
				if (skill instanceof final JSONArray skillArr) {
					skills.put(skillName, skillArr.clone(skills));
				} else {
					skills.put(skillName, ((JSONObject) skill).clone(skills));
				}
			}
		}

		final Set<Tuple<String, JSONObject>> alreadyChosen = new HashSet<>();
		if (skills.containsKey("Wahl")) {
			final JSONArray choiceArray = skills.getArr("Wahl");
			for (final JSONObject choices : choiceArray.getObjs()) {
				final List<Tuple<String, JSONObject>> actualChoices = new ArrayList<>();
				for (final String skillName : choices.keySet()) {
					final Object skill = choices.getUnsafe(skillName);
					if (skill instanceof final JSONObject skillObject) {
						final Tuple<String, JSONObject> skillTuple = new Tuple<>(skillName, skillObject);
						if (!actualSkills.containsKey(skillName) && !alreadyChosen.contains(skillTuple)) {
							actualChoices.add(skillTuple);
						}
					} else {
						for (final JSONObject variant : ((JSONArray) skill).getObjs()) {
							final Tuple<String, JSONObject> skillTuple = new Tuple<>(skillName, variant);
							if ((!actualSkills.containsKey(skillName) || !hasVariant(actualSkills.getArr(skillName), variant))
									&& !alreadyChosen.contains(skillTuple)) {
								actualChoices.add(skillTuple);
							}
						}
					}
				}
				final Tuple<String, JSONObject> skillTuple = switch (actualChoices.size()) {
					case 0 -> null;
					case 1 -> actualChoices.get(0);
					default -> showSkillChoiceDialog(actualChoices);
				};
				if (skillTuple != null) {
					final JSONObject skill = HeroUtil.findProConOrSkill(skillTuple._1)._1;
					if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
						final JSONArray skillArray = skills.getArr(skillTuple._1);
						skillArray.add(skillTuple._2.clone(skillArray));
					} else {
						skills.put(skillTuple._1, skillTuple._2.clone(skills));
					}
				}
			}
		}
		skills.removeKey("Wahl");
		final Set<String> skillsToRemove = new HashSet<>();
		for (final String skillName : skills.keySet()) {
			final Object skill = skills.getUnsafe(skillName);
			if (skill instanceof final JSONArray skillArr) {
				for (final JSONObject variant : skillArr.getObjs()) {
					if (actualSkills.containsKey(skillName) && hasVariant(actualSkills.getArr(skillName), variant)) {
						skillArr.remove(variant);
						if (skillArr.size() == 0) {
							skillsToRemove.add(skillName);
						}
					} else {
						variant.put("Verbilligung", true);
					}
				}
			} else {
				if (actualSkills.containsKey(skillName)) {
					skillsToRemove.add(skillName);
				} else {
					((JSONObject) skill).put("Verbilligung", true);
				}
			}
		}
		for (final String skillName : skillsToRemove) {
			skills.removeKey(skillName);
		}

		if (chosenTraitKnowledge != null) {
			final JSONArray traitKnowledges = skills.getArr("Merkmalskenntnis");
			final JSONObject newTraitKnowledge = new JSONObject(traitKnowledges);
			traitKnowledges.add(newTraitKnowledge);
			newTraitKnowledge.put("Auswahl", chosenTraitKnowledge);
		}

		effects.remove(skills);
		if (skills.size() > 0) {
			effects.put("Vorteile/Nachteile/Sonderfertigkeiten", skills);
		}

		return result;
	}

	private void getSpellSEs(final JSONObject ses, final JSONObject spells, final JSONObject houseSpells) {
		for (final String spell : spells.keySet()) {
			if ("Wahl".equals(spell)) {
				// Handled later
			} else {
				final JSONObject actualSpell = spells.getObj(spell);
				final JSONObject spellSEs = ses.containsKey(spell) ? ses.getObj(spell) : new JSONObject(ses);
				ses.put(spell, spellSEs);
				JSONObject houseSpell = null;
				if (houseSpells != null) {
					houseSpell = new JSONObject(houseSpells);
					houseSpells.put(spell, houseSpell);
				}
				for (final String representation : actualSpell.keySet()) {
					final Object actual = actualSpell.getUnsafe(representation);
					if (actual instanceof final JSONArray actualArray) {
						final JSONArray representationSEs = spellSEs.getArr(representation);
						final JSONArray representationSpells = houseSpells != null ? houseSpell.getArr(representation) : null;
						for (int i = 0; i < actualArray.size(); ++i) {
							representationSEs.add(1);
							if (houseSpells != null) {
								representationSpells.add(0);
							}
						}
					} else {
						spellSEs.put(representation, 1);
						if (houseSpells != null) {
							houseSpell.put(representation, 0);
						}
					}
				}
			}
		}
		if (spells.containsKey("Wahl")) {
			final JSONArray choiceArray = spells.getArr("Wahl");
			for (final JSONObject choiceObj : choiceArray.getObjs()) {
				final JSONArray choices = new JSONArray(null);
				int numSEs = 0;
				if (choiceObj.containsKey("Oder")) {
					for (final JSONObject group : choiceObj.getArr("Oder").getObjs()) {
						for (final String spell : group.getObj("Wahl").keySet()) {
							final JSONObject representation = new JSONObject(choices);
							final String actualRepresentation = group.getObj("Wahl").getString(spell);
							representation.put(spell, actualRepresentation);
							if (!ses.containsKey(spell) || !ses.getObj(spell).containsKey(actualRepresentation)) {
								choices.add(representation);
							}
						}
						numSEs = Math.max(numSEs, group.getArr("Werte").size());
					}
				} else {
					for (final String spell : choiceObj.getObj("Wahl").keySet()) {
						final JSONObject representation = new JSONObject(choices);
						final String actualRepresentation = choiceObj.getObj("Wahl").getString(spell);
						representation.put(spell, actualRepresentation);
						if (!ses.containsKey(spell) || !ses.getObj(spell).containsKey(actualRepresentation)) {
							choices.add(representation);
						}
					}
					numSEs = choiceObj.getArr("Werte").size();
				}
				// TODO This will be wrong for Adlerschwinge if it should gain multiple SEs
				final Collection<JSONObject> chosen = numSEs >= choices.size() ? choices.getObjs() : showSpellChoiceDialog(choices, numSEs, houseSpells, ses);
				for (final JSONObject spell : chosen) {
					final String spellName = spell.keySet().toArray(new String[0])[0];
					final JSONObject spellSEs = ses.getObj(spellName);
					spellSEs.put(spell.getString(spellName), 1);
					if (houseSpells != null) {
						final JSONObject houseSpell = houseSpells.getObj(spellName);
						houseSpell.put(spell.getString(spellName), 0);
					}
				}
			}
		}
	}

	public Collection<String> getVariants() {
		return variants;
	}

	private boolean hasVariant(final JSONArray actualSkill, final JSONObject variant) {
		for (final JSONObject actualVariant : actualSkill.getObjs()) {
			if (variant.containsKey("Auswahl")) {
				if (!actualVariant.getString("Auswahl").equals(variant.getString("Auswahl"))) {
					continue;
				}
			}
			if (variant.containsKey("Freitext")) {
				if (!actualVariant.getString("Freitext").equals(variant.getString("Freitext"))) {
					continue;
				}
			}
			return true;
		}
		return false;
	}

	private boolean isCleric(final JSONObject profession) {
		return profession.getObjOrDefault("Vorteile", new JSONObject(null)).containsKey("Geweiht")
				|| profession.getObjOrDefault("Sonderfertigkeiten", new JSONObject(null)).containsKey("Spätweihe");
	}

	private boolean isSameGuild(final JSONObject hero) {
		if (!"Magier".equals(hero.getObj("Biografie").getString("Profession"))
				&& !"Magier".equals(hero.getObj("Vorteile").getObjOrDefault("Breitgefächerte Bildung", new JSONObject(null)).getString("Profession")))
			return false;

		final String variant = ("Magier".equals(hero.getObj("Biografie").getString("Profession")) ? hero.getObj("Biografie")
				: hero.getObj("Vorteile").getObj("Breitgefächerte Bildung")).getArr("Profession:Modifikation").getString(0);

		final JSONObject magicians = ResourceManager.getResource("data/Professionen").getObjOrDefault("Magier", new JSONObject(null))
				.getObjOrDefault("Varianten", new JSONObject(null));
		final String ownGuild = magicians.getObj(variant).getStringOrDefault("Gilde", null);
		final String targetGuild = magicians.getObj(education.getDescription()).getStringOrDefault("Gilde", null);

		return ownGuild != null
				&& (targetGuild != null && ownGuild.equals(targetGuild) || "Institut der Arkanen Analysen zu Kuslik".equals(education.getDescription()));
	}

	private boolean maybeCleric(final JSONObject profession) {
		if (isCleric(profession))
			return true;
		if (!profession.containsKey("Varianten"))
			return false;
		final JSONObject variants = profession.getObj("Varianten");
		for (final String variantName : variants.keySet()) {
			if (maybeCleric(variants.getObj(variantName)))
				return true;
		}
		return false;
	}

	public void setChoice(final String choice) {
		education.setDescription(choice, false);
		if (variants != null) {
			variants.clear();
			final Collection<String> newVariants = getPossibleVariants(choice);
			if (newVariants != null) {
				variants.addAll(newVariants);
			}
		}
	}

	private void showAnimalBindingDialog(final JSONObject hero) {
		final JSONArray animals = hero.getArr("Tiere");
		final JSONObject animal = new JSONObject(animals);
		final JSONObject biography = new JSONObject(animal);
		biography.put("Name", "Vertrautentier");
		biography.put("Typ", "Vertrautentier");
		animal.put("Biografie", biography);
		new MagicAnimalBindingDialog(EnhancementController.instance.getRoot().getScene().getWindow(), animal, hero, false);
		final int apCost = biography.getIntOrDefault("Abenteuerpunkte-Kosten", 0);
		biography.removeKey("Abenteuerpunkte-Kosten");
		ap.set(apCost);
		final JSONObject heroBiography = hero.getObj("Biografie");
		heroBiography.put("Abenteuerpunkte-Guthaben", heroBiography.getIntOrDefault("Abenteuerpunkte-Guthaben", 0) - (apCost - getCalculatedAP(hero)));
	}

	private void showChoiceDialog(final Parent node, final double height, final Runnable action, final BooleanExpression invalid) {
		final VBox root = new VBox(2);
		root.setPadding(new Insets(4));

		final HBox buttonBox = new HBox(2);
		final Label spacer = new Label();
		spacer.setMaxWidth(Double.POSITIVE_INFINITY);
		HBox.setHgrow(spacer, Priority.ALWAYS);
		final Button okButton = new Button("Ok");
		okButton.setPrefWidth(90);
		buttonBox.getChildren().addAll(spacer, okButton);
		root.getChildren().addAll(node, buttonBox);

		final Stage stage = GUIUtil.setupStage(root, 300, height + 65, "Auswahl", EnhancementController.instance.getRoot().getScene().getWindow(), true);

		okButton.setOnAction(_ -> {
			action.run();
			stage.close();
		});

		okButton.setDefaultButton(true);

		okButton.disableProperty().bind(invalid);

		stage.showAndWait();
	}

	private void showLiturgyChoiceDialog(final String goddess, final JSONObject prosConsSkills) {
		try {
			final VBox root = new FXMLLoader().load(getClass().getResource("LiturgyChoiceDialog.fxml").openStream());

			final SimpleIntegerProperty grades = new SimpleIntegerProperty(DSAUtil.isAlveranGod(goddess) ? 16 : 8);

			final Label gradeLabel = (Label) root.getChildren().get(0);
			gradeLabel.textProperty().bind(Bindings.createStringBinding(() -> "Wähle Liturgien im Wert von " + grades.get() + " Graden", grades));

			@SuppressWarnings("unchecked")
			final ListView<String> chosenList = (ListView<String>) root.getChildren().get(1);
			final ObservableList<String> chosenItems = chosenList.getItems();
			chosenList.setItems(chosenItems);

			final HBox choiceBox = (HBox) root.getChildren().get(2);
			@SuppressWarnings("unchecked")
			final ReactiveComboBox<String> choices = (ReactiveComboBox<String>) choiceBox.getChildren().get(0);
			final Button addButton = (Button) choiceBox.getChildren().get(1);

			final ObservableList<String> choiceItems = choices.getItems();
			choices.setItems(new SortedList<>(choiceItems, Comparator.comparing(item -> item)));
			final SingleSelectionModel<String> selection = choices.getSelectionModel();

			final JSONObject liturgies = ResourceManager.getResource("data/Liturgien");
			for (final String liturgyName : liturgies.keySet()) {
				if (liturgies.getObj(liturgyName).getObj("Gottheiten").keySet().contains(goddess)) {
					choiceItems.add(liturgyName);
				}
			}
			selection.select(0);

			chosenList.setCellFactory(_ -> {
				final ListCell<String> cell = new ListCell<>() {
					@Override
					public void updateItem(final String item, final boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setText(null);
						} else {
							setText(liturgyToDisplayString(item, liturgies, goddess));
						}
					}
				};

				final ContextMenu contextMenu = new ContextMenu();

				final MenuItem removeItem = new MenuItem("Entfernen");
				contextMenu.getItems().add(removeItem);
				removeItem.setOnAction(_ -> {
					final String item = cell.getItem();
					chosenItems.remove(item);
					choiceItems.add(item);
					grades.set(grades.get() + getLiturgyGrade(item, liturgies, goddess));
				});

				cell.contextMenuProperty().bind(Bindings.when(cell.itemProperty().isNotNull()).then(contextMenu).otherwise((ContextMenu) null));

				return cell;
			});

			choices.setConverter(new StringConverter<String>() {

				@Override
				public String fromString(final String item) {
					return item;
				}

				@Override
				public String toString(final String item) {
					return liturgyToDisplayString(item, liturgies, goddess);
				}
			});

			addButton.setOnAction(_ -> {
				final String chosen = selection.getSelectedItem();
				chosenItems.add(chosen);
				choiceItems.remove(chosen);
				selection.select(0);
				grades.set(grades.get() - getLiturgyGrade(chosen, liturgies, goddess));
			});

			showChoiceDialog(root, 250, () -> {
				for (final String liturgy : chosenItems) {
					prosConsSkills.put(liturgy, new JSONObject(prosConsSkills));
				}
			}, grades.isNotEqualTo(0));
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
	}

	private <T> T showSEDialog(final Collection<String> items, final int numSEs, final Function<ObservableList<String>, ObservableValue<String>> text,
			final BiFunction<Collection<String>, Integer, T> commit) {
		final Object[] result = new Object[1];

		try {
			final VBox root = new FXMLLoader().load(getClass().getResource("ChoiceDialog.fxml").openStream());

			final Label choiceLabel = (Label) root.getChildren().get(0);

			final ObservableList<String> selected = FXCollections.observableArrayList();

			choiceLabel.textProperty().bind(text.apply(selected));

			final ScrollPane pane = (ScrollPane) root.getChildren().get(1);
			final VBox list = (VBox) pane.getContent();
			pane.minHeightProperty().set(8 + 21 * items.size());

			final ToggleGroup toggleGroup = new ToggleGroup();

			for (final String item : items) {
				if (numSEs == 1) {
					final RadioButton talentButton = new RadioButton(item);
					talentButton.setToggleGroup(toggleGroup);
					talentButton.selectedProperty().addListener((_, _, newV) -> {
						if (newV) {
							selected.add(item);
						} else {
							selected.remove(item);
						}
					});
					list.getChildren().add(talentButton);
				} else {
					final CheckBox talentBox = new CheckBox(item);
					talentBox.selectedProperty().addListener((_, _, newV) -> {
						if (newV) {
							selected.add(item);
						} else {
							selected.remove(item);
						}
					});
					list.getChildren().add(talentBox);
				}
			}

			if (numSEs == 1) {
				toggleGroup.getToggles().get(0).setSelected(true);
			}

			showChoiceDialog(root, 45 + 21 * items.size(),
					() -> result[0] = commit.apply(selected, toggleGroup.getToggles().indexOf(toggleGroup.getSelectedToggle())),
					Bindings.size(selected).isNotEqualTo(numSEs));
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}

		@SuppressWarnings("unchecked")
		final T actualResult = (T) result[0];
		return actualResult;
	}

	private Tuple<String, JSONObject> showSkillChoiceDialog(final List<Tuple<String, JSONObject>> actualChoices) {
		return showSEDialog(
				actualChoices.stream().map(skill -> {
					String name = skill._1;
					final JSONObject actualSkill = skill._2;
					if (actualSkill.containsKey("Auswahl") || actualSkill.containsKey("Freitext")) {
						if (actualSkill.containsKey("Auswahl")) {
							if (actualSkill.containsKey("Freitext")) {
								name = name + " (" + actualSkill.getString("Auswahl") + ", " + actualSkill.getString("Freitext") + ")";
							} else {
								name = name + " (" + actualSkill.getString("Auswahl") + ")";
							}
						} else {
							name = name + " (" + actualSkill.getString("Freitext") + ")";
						}
					}
					return name;
				}).toList(),
				1,
				(_) -> new SimpleStringProperty("Wähle eine verbilligte Sonderfertigkeit"),
				(_, index) -> actualChoices.get(index));
	}

	private Collection<JSONObject> showSpellChoiceDialog(final JSONArray spells, final int numSEs, final JSONObject houseSpells, final JSONObject ses) {
		final List<String> mappedSpells = spells.getObjs().stream().map(spell -> {
			final String spellName = spell.keySet().iterator().next();
			return spellName + " (" + spell.getString(spellName) + ")";
		}).toList();

		return showSEDialog(
				mappedSpells,
				numSEs,
				(selected) -> numSEs == 1 ? new SimpleStringProperty(houseSpells == null ? "Wähle eine Spezielle Erfahrung" : "Wähle einen Hauszauber")
						: Bindings.createStringBinding(
								() -> "Wähle " + (numSEs - selected.size()) + (houseSpells == null ? " Spezielle Erfahrungen" : " Hauszauber"), selected),
				(selected, _) -> selected.stream().map(name -> spells.getObj(mappedSpells.indexOf(name))).toList());
	}

	private Collection<String> showTalentChoiceDialog(final List<String> talents, final int numSEs, final JSONObject ses) {
		return showSEDialog(
				talents,
				numSEs,
				(selected) -> numSEs == 1 ? new SimpleStringProperty("Wähle eine Spezielle Erfahrung")
						: Bindings.createStringBinding(() -> "Wähle " + (numSEs - selected.size()) + " Spezielle Erfahrungen", selected),
				(selected, _) -> selected);
	}

	private String showTraitKnowledgeChoiceDialog(final Set<String> traitKnowledgeOptions, final JSONArray traitKnowledge,
			final JSONArray traitKnowledgeChoices, final JSONObject skills, final Set<String> traits) {
		return showSEDialog(
				traitKnowledgeOptions,
				1,
				(_) -> new SimpleStringProperty("Wähle eine Merkmalskenntnis"),
				(selectedOptions, _) -> {
					final String selected = selectedOptions.iterator().next();

					if (traitKnowledge != null) {
						for (final JSONObject currentSkill : traitKnowledge.getObjs()) {
							if (selected.equals(currentSkill.getString("Auswahl"))) {
								traitKnowledge.remove(currentSkill);
							}
						}
						if (traitKnowledge.size() > 0) {
							skills.put("Merkmalskenntnis", traitKnowledge);
						}
					}

					traits.add(selected);

					boolean added = false;
					do {
						for (final JSONArray currentChoice : traitKnowledgeChoices.getArrs()) {
							for (final JSONObject currentSkill : currentChoice.getObjs()) {
								if (traits.contains(currentSkill.getString("Auswahl"))) {
									currentChoice.remove(currentSkill);
								}
							}
							if (currentChoice.size() == 1) {
								final JSONArray target = skills.getArr("Merkmalskenntnis");
								target.add(currentChoice.getObj(0).clone(target));
								traits.add(currentChoice.getObj(0).getString("Auswahl"));
								added = true;
								traitKnowledgeChoices.remove(currentChoice);
							}
						}
					} while (added);

					if (traitKnowledgeChoices.size() > 0) {
						final JSONArray skillChoices = skills.getArr("Wahl");
						for (final JSONArray currentChoice : traitKnowledgeChoices.getArrs()) {
							final JSONObject newChoice = new JSONObject(skillChoices);
							skillChoices.add(newChoice);
							newChoice.put("Merkmalskenntnis", currentChoice.clone(newChoice));
						}
					}

					return selected;
				});
	}

	@Override
	public JSONObject toJSON(final JSONValue parent, final boolean planned) {
		final JSONObject result = new JSONObject(parent);
		result.put("Typ", "Speziell");
		result.put("Art", getName());
		if (!"".equals(education.getDescription())) {
			result.put("Auswahl", education.getDescription());
		}
		if (!"".equals(education.getVariant())) {
			if (education.getProOrCon().containsKey("Abgestuft")) {
				result.put("TaP*", Integer.parseInt(education.getVariant()));
			} else {
				result.put("Freitext", education.getVariant());
			}
		}
		result.put("AP", ap.get());
		if (cost.get() != 0) {
			result.put("Kosten", cost.get());
		}
		if (effects != null) {
			if (List.of("Bindung eines Vertrautentiers", "Kontakt zum Großen Geist", "Spätweihe", "Zweitstudium").contains(getName())) {
				result.put("Effekte", effects.clone(result));
			}
		}
		if (!planned) {
			final LocalDate currentDate = LocalDate.now();
			result.put("Datum", currentDate.toString());
		}
		return result;
	}

	@Override
	public void unapply(final JSONObject hero) {
		final String name = getName();
		final JSONObject actual = getApplicationActual(hero);
		HeroUtil.unapplyEffect(hero, name, effects, actual);
	}

	@Override
	public void unapplyTemporary(final JSONObject hero) {
		/* No need to unapply since we don't apply these temporarily */
	}

	public StringProperty variantProperty() {
		return education.variantProperty();
	}

	private Collection<JSONArray> violatedTeachingLanguageRequirements(final JSONObject hero) {
		final List<JSONArray> violated = new LinkedList<>();

		final JSONObject profession = ResourceManager.getResource("data/Professionen").getObjOrDefault("Magier", new JSONObject(null))
				.getObjOrDefault("Varianten", new JSONObject(null)).getObjOrDefault(education.getDescription(), new JSONObject(null));

		JSONArray teachingLanguages = new JSONArray(null);
		if (profession.containsKey("Sprachen")) {
			teachingLanguages = profession.getObjOrDefault("Sprachen", new JSONObject(null)).getArrOrDefault("Lehrsprache", teachingLanguages);
		} else if (variants != null && !variants.isEmpty()) {
			final JSONObject variant = profession.getObj("Varianten").getObj(education.getVariant().split(":")[0]);
			teachingLanguages = variant.getObjOrDefault("Sprachen", new JSONObject(null)).getArrOrDefault("Lehrsprache", teachingLanguages);
		}
		for (int i = 0; i < teachingLanguages.size(); ++i) {
			final JSONArray teachingLanguageChoice = teachingLanguages.getArr(i);
			boolean fulfilled = false;
			for (int j = 0; j < teachingLanguageChoice.size(); ++j) {
				if (RequirementsUtil.isTalentRequirementFulfilled(hero, teachingLanguageChoice.getString(j), 10)) {
					fulfilled = true;
				}
			}
			if (!fulfilled) {
				violated.add(teachingLanguageChoice);
			}
		}

		return violated;
	}
}
