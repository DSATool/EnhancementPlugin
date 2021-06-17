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
package enhancement.enhancements;

import dsa41basis.ui.hero.HeroController;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public abstract class EnhancementTabController implements HeroController {

	protected JSONListener heroListener = o -> {
		update();
	};

	protected JSONObject hero;
	protected Tab tab;

	private boolean stopInit = true;

	public EnhancementTabController(final TabPane pane) {
		tab = new Tab(getText());
		tab.setClosable(false);
		tab.setOnSelectionChanged(e -> {
			if (tab.isSelected()) {
				if (getControl() == null && !stopInit) {
					init();
					tab.setContent(getControl());
					update();
				}
				if (hero != null) {
					update();
					registerListeners();
				}
			} else if (hero != null) {
				unregisterListeners();
			}
		});
		pane.getTabs().add(tab);
		stopInit = false;
	}

	protected abstract Node getControl();

	protected abstract String getText();

	protected void init() {}

	public abstract void recalculate(JSONObject hero);

	public abstract void recalculateValid(JSONObject hero);

	protected abstract void registerListeners();

	public abstract boolean removeEnhancement(Enhancement enhancement);

	@Override
	public void setHero(final JSONObject hero) {
		if (this.hero != null && getControl() != null) {
			unregisterListeners();
		}
		this.hero = hero;
		if (getControl() != null) {
			update();
			if (hero != null && tab.isSelected()) {
				registerListeners();
			}
		}
	}

	protected abstract void unregisterListeners();

	public abstract void update();
}
