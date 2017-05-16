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

import java.util.Collection;
import java.util.Stack;

import dsa41basis.hero.ProOrCon;
import dsa41basis.util.DSAUtil;
import enhancement.enhancements.Enhancement;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class QuirkEnhancement extends Enhancement {

	private final ProOrCon quirk;
	private final IntegerProperty start;
	private final IntegerProperty target;
	private final IntegerProperty ses = new SimpleIntegerProperty(0);

	public QuirkEnhancement(ProOrCon quirk, JSONObject hero) {
		this.quirk = quirk;
		start = new SimpleIntegerProperty(quirk.getValue());
		target = new SimpleIntegerProperty(start.get() - 1);
		fullDescription.bind(description);
		cheaper.bind(ses.greaterThan(0));
		updateDescription();
		recalculateValid(hero);
	}

	@Override
	public void apply(JSONObject hero) {
		applyTemporarily(hero);
		final JSONObject cons = hero.getObj("Nachteile");
		cons.notifyListeners(null);
	}

	@Override
	public void applyTemporarily(JSONObject hero) {
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
	protected boolean calculateValid(JSONObject hero) {
		return true;
	}

	public QuirkEnhancement clone(JSONObject hero, Collection<Enhancement> enhancements) {
		final QuirkEnhancement result = new QuirkEnhancement(quirk, hero);
		result.setTarget(target.get(), hero, enhancements);
		return result;
	}

	@Override
	protected int getCalculatedCost(JSONObject hero) {
		final int SELevel = start.get() - Math.min(ses.get(), start.get() - target.get());
		return (int) (((start.get() - SELevel) * 50 + (SELevel - target.get()) * 75) * quirk.getProOrCon().getDoubleOrDefault("Kosten", 1.0));
	}

	@Override
	public String getName() {
		return quirk.getName();
	}

	public ProOrCon getQuirk() {
		return quirk;
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

	public void setTarget(int target, JSONObject hero, Collection<Enhancement> enhancements) {
		final Stack<Enhancement> enhancementStack = new Stack<>();
		for (final Enhancement e : enhancements) {
			e.applyTemporarily(hero);
			enhancementStack.push(e);
		}

		this.target.set(target);
		updateDescription();
		recalculateValid(hero);
		resetCost(hero);

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

	@Override
	public void unapply(JSONObject hero) {
		final JSONObject actual = quirk.getActual();
		final JSONObject cons = hero.getObj("Nachteile");
		final JSONObject con = quirk.getProOrCon();
		final String name = quirk.getName();
		if (target.get() == 0) {
			if (con.containsKey("Auswahl") || con.containsKey("Freitext")) {
				final JSONArray conArray = cons.getArr(name);
				conArray.add(actual.clone(conArray));
			} else {
				cons.put(name, actual.clone(cons));
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
						break;
					}
				}
			} else {
				cons.getObj(name).put("Stufe", start.get());
			}
		}
	}

	private void updateDescription() {
		final String desc = DSAUtil.printProOrCon(quirk.getActual(), quirk.getName(), quirk.getProOrCon(), false) + " (" + start.get() + "->" + target.get()
				+ ")";
		description.set(desc);
		cost.set(getCalculatedCost(null));
	}

}
