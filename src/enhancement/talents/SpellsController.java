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

import dsa41basis.util.HeroUtil;
import dsatool.resources.ResourceManager;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import enhancement.enhancements.EnhancementTabController;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public class SpellsController extends EnhancementTabController {
	private final TabPane tabPane;
	private TalentGroupController controller;
	private final JSONListener listener;

	public SpellsController(final EnhancementController controller, final TabPane tabPane) {
		super(tabPane);
		this.tabPane = tabPane;
		listener = o -> setTab();
	}

	@Override
	protected Node getControl() {
		return controller != null ? controller.getControl() : null;
	}

	@Override
	protected String getText() {
		return "Zauber";
	}

	@Override
	protected void init() {
		final JSONObject talents = ResourceManager.getResource("data/Zauber");
		controller = new TalentGroupController("Zauber", talents);

		setTab();
	}

	@Override
	public void recalculate(final JSONObject hero) {
		if (controller != null) {
			controller.recalculate(hero);
		}
	}

	@Override
	public void recalculateValid(final JSONObject hero) {}

	@Override
	protected void registerListeners() {
		hero.getObj("Vorteile").addListener(listener);
		controller.registerListeners();
	}

	@Override
	public boolean removeEnhancement(final Enhancement enhancement) {
		if (enhancement instanceof SpellEnhancement) {
			controller.removeEnhancement((TalentEnhancement) enhancement);
			return true;
		} else
			return false;
	}

	@Override
	public void setHero(final JSONObject hero) {
		super.setHero(hero);
		setTab();
		if (controller != null) {
			controller.setHero(hero);
		}
	}

	private void setTab() {
		if (HeroUtil.isMagical(hero)) {
			if (!tabPane.getTabs().contains(tab)) {
				tab.setContent(getControl());
				tab.setClosable(false);
				tabPane.getTabs().add(4, tab);
			}
		} else {
			tabPane.getTabs().remove(tab);
		}
	}

	@Override
	protected void unregisterListeners() {
		hero.getObj("Vorteile").removeListener(listener);
		if (controller != null) {
			controller.unregisterListeners();
		}
	}

	@Override
	public void update() {
		setTab();
		if (controller != null) {
			controller.setHero(hero);
		}
	}

}
