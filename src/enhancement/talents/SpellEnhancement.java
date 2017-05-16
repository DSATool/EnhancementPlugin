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

import java.util.Collection;

import dsa41basis.hero.Spell;
import enhancement.enhancements.Enhancement;
import jsonant.value.JSONObject;

public class SpellEnhancement extends TalentEnhancement {

	public SpellEnhancement(Spell spell, JSONObject hero) {
		super(spell, "Zauber", hero);
	}

	@Override
	public void apply(JSONObject hero) {
		final JSONObject actual = hero.getObj("Zauber").getObj(talent.getName()).getObj(((Spell) talent).getRepresentation());
		actual.put("ZfW", target.get());
		final int ses = talent.getSes() - (target.get() - start.get());
		if (ses <= 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", ses);
		}
		actual.notifyListeners(null);
	}

	@Override
	public void applyTemporarily(JSONObject hero) {
		final JSONObject spells = hero.getObj("Zauber");
		JSONObject actualSpell = spells.getObjOrDefault(talent.getName(), null);
		if (actualSpell == null) {
			actualSpell = new JSONObject(spells);
			actualSpell.put("Temporär", true);
			spells.put(talent.getName(), actualSpell);
		}
		JSONObject actual = actualSpell.getObjOrDefault(((Spell) talent).getRepresentation(), null);
		if (actual == null) {
			actual = new JSONObject(actualSpell);
			actual.put("Temporär", true);
			actualSpell.put(((Spell) talent).getRepresentation(), actual);
		}
		actual.put("ZfW", target.get());
	}

	@Override
	public SpellEnhancement clone(JSONObject hero, Collection<Enhancement> enhancements) {
		final SpellEnhancement result = new SpellEnhancement((Spell) talent, hero);
		result.setTarget(target.get(), hero, enhancements);
		return result;
	}

	@Override
	public void unapply(JSONObject hero) {
		final JSONObject spells = hero.getObj("Zauber");
		final JSONObject actualSpell = spells.getObj(talent.getName());
		if (actualSpell.getBoolOrDefault("Temporär", false)) {
			spells.removeKey(talent.getName());
			return;
		}
		final JSONObject actual = actualSpell.getObj(((Spell) talent).getRepresentation());
		if (actual.getBoolOrDefault("Temporär", false)) {
			actualSpell.removeKey(((Spell) talent).getRepresentation());
			return;
		}
		actual.put("ZfW", start.get());
	}

	@Override
	protected void updateDescription() {
		description.set(talent.getName() + "(" + ((Spell) talent).getRepresentation() + ") (" + startString.get() + "->" + target.get() + ")");
	}
}
