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

	public TalentController(final TabPane tabPane) {
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
			case "Ritualkenntnis":
				ritualKnowledge = talentController.getControl();
				break;
			case "Liturgiekenntnis":
				liturgyKnowledge = talentController.getControl();
				break;
			}
			box.getChildren().add(talentController.getControl());
		}

		setTab(tabPane);
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
	public void recalculateCost(final JSONObject hero) {
		for (final TalentGroupController controller : talentControllers) {
			controller.recalculateCost(hero);
		}
	}

	@Override
	public void recalculateValid(final JSONObject hero) {
		for (final TalentGroupController controller : talentControllers) {
			controller.recalculateValid(hero);
		}
	}

	@Override
	public void setHero(final JSONObject hero) {
		if (this.hero != null) {
			this.hero.getObj("Vorteile").removeListener(listener);
			this.hero.getObj("Sonderfertigkeiten").removeListener(listener);
		}
		super.setHero(hero);
		updateVisibility();
	}

	@Override
	public void update() {
		hero.getObj("Vorteile").removeListener(listener);
		hero.getObj("Sonderfertigkeiten").removeListener(listener);
		for (final TalentGroupController controller : talentControllers) {
			controller.setHero(hero);
		}
		hero.getObj("Vorteile").addListener(listener);
		hero.getObj("Sonderfertigkeiten").addListener(listener);
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
