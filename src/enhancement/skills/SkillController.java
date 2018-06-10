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
package enhancement.skills;

import java.util.ArrayList;
import java.util.List;

import dsatool.resources.ResourceManager;
import dsatool.util.ErrorLogger;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import enhancement.enhancements.EnhancementTabController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import jsonant.value.JSONObject;

public class SkillController extends EnhancementTabController {
	@FXML
	private VBox box;
	@FXML
	private ScrollPane pane;
	@FXML
	private CheckBox showAll;

	private final List<SkillGroupController> skillControllers = new ArrayList<>();

	public SkillController(final EnhancementController controller, final TabPane tabPane) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("SpecialSkills.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final JSONObject specialSkills = ResourceManager.getResource("data/Sonderfertigkeiten");
		for (final String skillGroup : specialSkills.keySet()) {
			final SkillGroupController groupController = new SkillGroupController(pane, skillGroup, specialSkills.getObj(skillGroup),
					showAll.selectedProperty());
			skillControllers.add(groupController);
			box.getChildren().add(groupController.getControl());
		}
		final JSONObject rituals = ResourceManager.getResource("data/Rituale");
		for (final String skillGroup : rituals.keySet()) {
			final SkillGroupController groupController = new SkillGroupController(pane, skillGroup, rituals.getObj(skillGroup),
					showAll.selectedProperty());
			skillControllers.add(groupController);
			box.getChildren().add(groupController.getControl());
		}
		SkillGroupController groupController = new SkillGroupController(pane, "Liturgien", ResourceManager.getResource("data/Liturgien"),
				showAll.selectedProperty());
		skillControllers.add(groupController);
		box.getChildren().add(groupController.getControl());
		groupController = new SkillGroupController(pane, "Schamanen-Rituale", ResourceManager.getResource("data/Schamanenrituale"),
				showAll.selectedProperty());
		skillControllers.add(groupController);
		box.getChildren().add(groupController.getControl());

		setTab(tabPane);
	}

	@Override
	protected Node getControl() {
		return pane;
	}

	@Override
	protected String getText() {
		return "Sonderfertigkeiten";
	}

	@Override
	public void recalculate(final JSONObject hero) {
		for (final SkillGroupController controller : skillControllers) {
			controller.recalculate(hero);
		}
	}

	@Override
	public void recalculateValid(final JSONObject hero) {
		for (final SkillGroupController controller : skillControllers) {
			controller.recalculateValid(hero);
		}
	}

	@Override
	public boolean removeEnhancement(final Enhancement enhancement) {
		if (enhancement instanceof SkillEnhancement) {
			for (final SkillGroupController controller : skillControllers) {
				if (controller.removeEnhancement((SkillEnhancement) enhancement)) {
					break;
				}
			}
			return true;
		} else
			return false;
	}

	@Override
	public void update() {
		for (final SkillGroupController controller : skillControllers) {
			controller.setHero(hero);
		}
	}
}
