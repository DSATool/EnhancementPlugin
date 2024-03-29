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

import java.util.ArrayList;
import java.util.List;

import dsa41basis.util.HeroUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.ErrorLogger;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import enhancement.enhancements.EnhancementTabController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public class TalentController extends EnhancementTabController {

	@FXML
	private ScrollPane pane;
	@FXML
	private VBox box;

	private final List<TalentGroupController> talentControllers = new ArrayList<>();
	private Node ritualKnowledge;
	private Node liturgyKnowledge;

	private final JSONListener listener = o -> updateVisibility();

	public TalentController(final EnhancementController controller, final TabPane tabPane) {
		super(tabPane);
	}

	@Override
	protected Node getControl() {
		return pane;
	}

	@Override
	protected String getText() {
		return "Talente";
	}

	@Override
	protected void init() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("Talents.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final JSONObject talents = ResourceManager.getResource("data/Talente");

		for (final String talentGroup : talents.keySet()) {
			if ("Meta-Talente".equals(talentGroup)) {
				continue;
			}
			final TalentGroupController talentController = new TalentGroupController(talentGroup, talents.getObj(talentGroup));
			talentControllers.add(talentController);
			switch (talentGroup) {
				case "Ritualkenntnis" -> ritualKnowledge = talentController.getControl();
				case "Liturgiekenntnis" -> liturgyKnowledge = talentController.getControl();
			}
			box.getChildren().add(talentController.getControl());
		}
	}

	@Override
	public void recalculate(final JSONObject hero) {
		for (final TalentGroupController controller : talentControllers) {
			controller.recalculate(hero);
		}
	}

	@Override
	public void recalculateValid(final JSONObject hero) {}

	@Override
	protected void registerListeners() {
		hero.getObj("Vorteile").addListener(listener);
		hero.getObj("Sonderfertigkeiten").addListener(listener);
		for (final TalentGroupController controller : talentControllers) {
			controller.registerListeners();
		}
	}

	@Override
	public boolean removeEnhancement(final Enhancement enhancement) {
		if (enhancement instanceof final TalentEnhancement te && !(enhancement instanceof SpellEnhancement)) {
			for (final TalentGroupController controller : talentControllers) {
				if (controller.removeEnhancement(te)) {
					break;
				}
			}
			return true;
		} else
			return false;
	}

	@Override
	public void setHero(final JSONObject hero) {
		super.setHero(hero);
		if (box != null) {
			updateVisibility();
		}
	}

	@Override
	protected void unregisterListeners() {
		hero.getObj("Vorteile").removeListener(listener);
		hero.getObj("Sonderfertigkeiten").removeListener(listener);
		for (final TalentGroupController controller : talentControllers) {
			controller.unregisterListeners();
		}
	}

	@Override
	public void update() {
		for (final TalentGroupController controller : talentControllers) {
			controller.setHero(hero);
		}
	}

	private void updateVisibility() {
		if (HeroUtil.isMagical(hero)) {
			if (!box.getChildren().contains(ritualKnowledge)) {
				box.getChildren().add(ritualKnowledge);
			}
		} else {
			box.getChildren().remove(ritualKnowledge);
		}
		if (HeroUtil.isClerical(hero, false)) {
			if (!box.getChildren().contains(liturgyKnowledge)) {
				box.getChildren().add(liturgyKnowledge);
			}
		} else {
			box.getChildren().remove(liturgyKnowledge);
		}
	}

}
