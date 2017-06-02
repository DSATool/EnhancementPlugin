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
import dsatool.util.ErrorLogger;
import enhancement.enhancements.EnhancementController;
import enhancement.enhancements.EnhancementTabController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public class SpellsController extends EnhancementTabController {
	@FXML
	private ScrollPane pane;
	@FXML
	private VBox box;

	private Tab tab;
	private final TabPane tabPane;
	private TalentGroupController controller;
	private final JSONListener listener;
	private final EnhancementController enhancementController;

	public SpellsController(final TabPane tabPane, final EnhancementController controller) {
		this.tabPane = tabPane;
		enhancementController = controller;
		listener = o -> setTab(tabPane);
		setTab(tabPane);
	}

	private void createPane() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("Talents.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final JSONObject talents = ResourceManager.getResource("data/Zauber");
		controller = new TalentGroupController(pane, enhancementController, "Zauber", talents);
		box.getChildren().add(controller.getControl());
	}

	@Override
	protected Node getControl() {
		return pane;
	}

	@Override
	protected String getText() {
		return "Zauber";
	}

	@Override
	public void recalculateCost(final JSONObject hero) {
		if (controller != null) {
			controller.recalculateCost(hero);
		}
	}

	@Override
	public void recalculateValid(final JSONObject hero) {
		if (controller != null) {
			controller.recalculateValid(hero);
		}
	}

	@Override
	public void setHero(final JSONObject hero) {
		if (this.hero != null) {
			this.hero.getObj("Vorteile").removeListener(listener);
		}
		super.setHero(hero);
	}

	@Override
	protected void setTab(final TabPane tabPane) {
		if (HeroUtil.isMagical(hero)) {
			if (tab == null) {
				tab = new Tab(getText());
			}
			if (pane == null) {
				createPane();
			}
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
	public void update() {
		setTab(tabPane);
		hero.getObj("Vorteile").removeListener(listener);
		if (controller != null) {
			controller.setHero(hero);
		}
		hero.getObj("Vorteile").addListener(listener);
	}

}
