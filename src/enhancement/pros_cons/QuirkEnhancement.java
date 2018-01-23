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
package enhancement.pros_cons;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Stack;

import dsa41basis.hero.ProOrCon;
import dsa41basis.util.DSAUtil;
import dsatool.resources.ResourceManager;
import enhancement.enhancements.Enhancement;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class QuirkEnhancement extends Enhancement {
	public static QuirkEnhancement fromJSON(final JSONObject enhancement, final JSONObject hero, final Collection<Enhancement> enhancements) {
		final String quirkName = enhancement.getString("Schlechte Eigenschaft");
		final JSONObject con = ResourceManager.getResource("data/Nachteile").getObj(quirkName);

		final ProOrCon newQuirk = new ProOrCon(quirkName, hero, con, new JSONObject(null));
		final QuirkEnhancement result = new QuirkEnhancement(newQuirk, hero);
		if (con.containsKey("Auswahl")) {
			newQuirk.setDescription(enhancement.getString("Auswahl"));
			if (con.containsKey("Freitext")) {
				newQuirk.setVariant(enhancement.getString("Freitext"));
			}
		} else if (con.containsKey("Freitext")) {
			newQuirk.setDescription(enhancement.getString("Freitext"));
		}
		result.start.set(enhancement.getInt("Von"));
		result.setTarget(enhancement.getInt("Auf"), hero, enhancements);
		result.ses.set(result.seMin + enhancement.getIntOrDefault("SEs", 0));
		result.ap.set(enhancement.getInt("AP"));
		result.date.set(LocalDate.parse(enhancement.getString("Datum")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu")));
		result.updateDescription();
		return result;
	}

	private final ProOrCon quirk;
	private final IntegerProperty start;
	private final IntegerProperty target;
	private final IntegerProperty ses = new SimpleIntegerProperty(0);
	private int seMin;

	public QuirkEnhancement(final ProOrCon quirk, final JSONObject hero) {
		this.quirk = quirk;
		start = new SimpleIntegerProperty(quirk.getValue());
		target = new SimpleIntegerProperty(start.get() - 1);
		fullDescription.bind(description);
		cheaper.bind(ses.greaterThan(0));
		updateDescription();
		recalculateValid(hero);
	}

	@Override
	public void apply(final JSONObject hero) {
		applyTemporarily(hero);
		hero.getObj("Nachteile").notifyListeners(null);
	}

	@Override
	public void applyTemporarily(final JSONObject hero) {
		final JSONObject actual = quirk.getActual();
		final JSONObject cons = hero.getObj("Nachteile");
		final JSONObject con = quirk.getProOrCon();
		final String name = quirk.getName();
		if (target.get() == 0) {
			if (con.containsKey("Auswahl") || con.containsKey("Freitext")) {
				cons.getArr(name).remove(quirk.getActual());
			} else {
				cons.removeKey(name);
			}
		} else {
			if (con.containsKey("Auswahl") || con.containsKey("Freitext")) {
				final JSONArray conArray = cons.getArr(name);
				for (int i = 0; i < conArray.size(); ++i) {
					final JSONObject actualCon = conArray.getObj(i);
					if (actualCon.equals(actual)) {
						actualCon.put("Stufe", target.get());
						break;
					}
				}
			} else {
				cons.getObj(name).put("Stufe", target.get());
			}
		}
	}

	@Override
	protected boolean calculateValid(final JSONObject hero) {
		return true;
	}

	public QuirkEnhancement clone(final JSONObject hero, final Collection<Enhancement> enhancements) {
		final QuirkEnhancement result = new QuirkEnhancement(quirk, hero);
		result.start.set(start.get());
		result.setTarget(target.get(), hero, enhancements);
		result.ses.set(ses.get());
		result.seMin = seMin;
		result.updateDescription();
		return result;
	}

	@Override
	protected int getCalculatedAP(final JSONObject hero) {
		final int SELevel = start.get() - Math.min(ses.get(), start.get() - target.get());
		return (int) (((start.get() - SELevel) * 50 + (SELevel - target.get()) * 75) * quirk.getProOrCon().getDoubleOrDefault("Kosten", 1.0));
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
	public String getName() {
		return quirk.getName();
	}

	public ProOrCon getQuirk() {
		return quirk;
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

	public void setSes(final int ses, final JSONObject hero) {
		this.ses.set(ses);
		reset(hero);
	}

	public void setTarget(final int target, final JSONObject hero, final Collection<Enhancement> enhancements) {
		final Stack<Enhancement> enhancementStack = new Stack<>();
		for (final Enhancement e : enhancements) {
			e.applyTemporarily(hero);
			enhancementStack.push(e);
		}

		this.target.set(target);
		updateDescription();
		recalculateValid(hero);
		reset(hero);

		for (final Enhancement e : enhancementStack) {
			e.unapply(hero);
		}
	}

	public IntegerProperty startProperty() {
		return start;
	}

	public IntegerProperty targetProperty() {
		return target;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see enhancement.enhancements.Enhancement#toJSON()
	 */
	@Override
	public JSONObject toJSON() {
		final JSONObject result = new JSONObject(null);
		result.put("Typ", "Schlechte Eigenschaft");
		result.put("Schlechte Eigenschaft", quirk.getName());
		final JSONObject con = quirk.getProOrCon();
		if (con.containsKey("Auswahl")) {
			result.put("Auswahl", quirk.getActual().getString("Auswahl"));
		}
		if (con.containsKey("Freitext")) {
			result.put("Freitext", quirk.getActual().getString("Freitext"));
		}
		result.put("Von", start.get());
		result.put("Auf", target.get());
		final int resultSes = ses.get() - (target.get() - start.get());
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
		final JSONObject actual = quirk.getActual();
		final JSONObject cons = hero.getObj("Nachteile");
		final JSONObject con = quirk.getProOrCon();
		final String name = quirk.getName();
		if (target.get() == 0) {
			if (con.containsKey("Auswahl") || con.containsKey("Freitext")) {
				final JSONArray conArray = cons.getArr(name);
				final JSONObject newCon = actual.clone(conArray);
				conArray.add(newCon);
				newCon.put("SEs", ses.get());
			} else {
				cons.put(name, actual.clone(cons));
				cons.getObj(name).put("SEs", ses.get());
			}
		} else {
			if (con.containsKey("Auswahl") || con.containsKey("Freitext")) {
				final JSONArray conArray = cons.getArr(name);
				final JSONObject cloned = actual.clone(null);
				cloned.put("Stufe", target.get());
				for (int i = 0; i < conArray.size(); ++i) {
					final JSONObject actualCon = conArray.getObj(i);
					if (actualCon.equals(cloned)) {
						actualCon.put("Stufe", target.get());
						actualCon.put("SEs", ses.get());
						break;
					}
				}
			} else {
				cons.getObj(name).put("Stufe", start.get());
				cons.getObj(name).put("SEs", ses.get());
			}
		}
	}

	private void updateDescription() {
		final String desc = DSAUtil.printProOrCon(quirk.getActual(), quirk.getName(), quirk.getProOrCon(), false) + " (" + start.get() + "->" + target.get()
				+ ")";
		description.set(desc);
		recalculateCosts(null);
	}

}
