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

	public SpellEnhancement(final Spell spell, final JSONObject hero) {
		super(spell, "Zauber", hero);
	}

	@Override
	public SpellEnhancement clone(final JSONObject hero, final Collection<Enhancement> enhancements) {
		final SpellEnhancement result = new SpellEnhancement((Spell) talent, hero);
		result.start.set(start.get());
		result.startString.set(startString.get());
		result.target.set(target.get());
		result.targetString.set(targetString.get());
		result.basis = basis;
		result.method = method;
		result.ses = ses;
		result.seMin = seMin;
		result.temporary = temporary;
		return result;
	}

	@Override
	protected void updateDescription() {
		description.set(talent.getDisplayName() + " (" + ((Spell) talent).getRepresentation() + ") (" + startString.get() + "->" + target.get() + ")");
	}
}
