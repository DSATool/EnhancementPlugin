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
package enhancement.history;

import dsatool.gui.GUIUtil;
import dsatool.util.ErrorLogger;
import enhancement.attributes.AttributeEnhancement;
import enhancement.attributes.EnergyEnhancement;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import enhancement.enhancements.EnhancementTabController;
import enhancement.pros_cons.QuirkEnhancement;
import enhancement.skills.SkillEnhancement;
import enhancement.talents.SpellEnhancement;
import enhancement.talents.TalentEnhancement;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class HistoryController extends EnhancementTabController {
	@FXML
	private ScrollPane pane;
	@FXML
	private TableView<Enhancement> table;
	@FXML
	private TableColumn<Enhancement, String> descriptionColumn;
	@FXML
	private TableColumn<Enhancement, Integer> costColumn;
	@FXML
	private TableColumn<Enhancement, String> dateColumn;

	private final EnhancementController controller;

	public HistoryController(final EnhancementController controller, final TabPane tabPane) {
		this.controller = controller;

		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("History.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		setTab(tabPane);

		table.prefWidthProperty().bind(pane.widthProperty().subtract(20));

		GUIUtil.autosizeTable(table, 0, 2);
		GUIUtil.cellValueFactories(table, "fullDescription", "cost", "date");

		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem undoItem = new MenuItem("Rückgängig");
		contextMenu.getItems().add(undoItem);
		undoItem.setOnAction(o -> {
			undo(table.getSelectionModel().getSelectedIndex());
		});
		contextMenu.setOnShowing(e -> {
			final Enhancement enhancement = table.getSelectionModel().getSelectedItem();
			undoItem.setVisible(enhancement != null);
		});
		table.setContextMenu(contextMenu);
	}

	@Override
	protected Node getControl() {
		return pane;
	}

	@Override
	protected String getText() {
		return "Historie";
	}

	@Override
	public void recalculateCost(final JSONObject hero) {}

	@Override
	public void recalculateValid(final JSONObject hero) {}

	private void undo(final int index) {
		final JSONArray history = hero.getArr("Steigerungshistorie");
		int ap = 0;
		int sum = 0;
		for (int i = 0; i <= index; ++i) {
			final Enhancement enhancement = table.getItems().get(i);
			if (enhancement instanceof APEnhancement) {
				sum -= enhancement.getCost();
				ap += enhancement.getCost();
			} else {
				sum += enhancement.getCost();
			}
		}

		final JSONObject bio = hero.getObj("Biografie");

		String text = "Alle Steigerungen bis zur ausgewählten rückgängig machen.\nDas wird ";
		if (index == 0) {
			text += "eine Steigerung rückgängig machen.\nDabei werden ";
		} else {
			text += index + 1 + " Steigerungen rückgängig machen.\nDabei werden ";
		}
		if (ap > 0) {
			text += ap + " AP entfernt und ";
		} else if (ap < 0) {
			text += -ap + " AP wiederhergestellt und ";
		}
		if (sum < 0) {
			text += "die freien AP um " + -sum + " reduziert.";
		} else {
			text += sum + " freie AP rückerstattet.";
		}

		final Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Steigerungen rückgängig machen");
		alert.setHeaderText(text);
		alert.setContentText("Sollen die Steigerungen wirklich rückgängig gemacht werden?");
		alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
		alert.showAndWait().filter(response -> response.equals(ButtonType.OK)).ifPresent(response -> {
			int cost = 0;
			for (int i = 0; i <= index; ++i) {
				final Enhancement enhancement = table.getItems().get(i);
				enhancement.unapply(hero);
				history.removeAt(history.size() - 1);
				if (enhancement instanceof APEnhancement) {
					cost -= enhancement.getCost();
				} else {
					cost += enhancement.getCost();
				}
			}
			bio.put("Abenteuerpunkte-Guthaben", bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0) + cost);
			bio.notifyListeners(null);

			controller.update();
		});
	}

	@Override
	public void update() {
		table.getItems().clear();

		final JSONArray history = hero.getArr("Steigerungshistorie");
		for (int i = history.size() - 1; i >= 0; --i) {
			final JSONObject entry = history.getObj(i);
			switch (entry.getString("Typ")) {
			case "Eigenschaft":
				table.getItems().add(AttributeEnhancement.fromJSON(entry, hero));
				break;
			case "Basiswert":
				table.getItems().add(EnergyEnhancement.fromJSON(entry, hero));
				break;
			case "Schlechte Eigenschaft":
				table.getItems().add(QuirkEnhancement.fromJSON(entry, hero, controller.getEnhancements()));
				break;
			case "Sonderfertigkeit":
				table.getItems().add(SkillEnhancement.fromJSON(entry, hero));
				break;
			case "Talent":
				table.getItems().add(TalentEnhancement.fromJSON(entry, hero, controller.getEnhancements()));
				break;
			case "Zauber":
				table.getItems().add(SpellEnhancement.fromJSON(entry, hero, controller.getEnhancements()));
				break;
			case "Abenteuerpunkte":
				table.getItems().add(APEnhancement.fromJSON(entry));
				break;
			}
		}

		table.setPrefHeight(table.getItems().size() * 28 + 27);
	}
}
