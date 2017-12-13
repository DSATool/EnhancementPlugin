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

import dsa41basis.hero.ProOrCon;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.ErrorLogger;
import dsatool.util.IntegerSpinnerTableCell;
import dsatool.util.Tuple;
import dsatool.util.Util;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import enhancement.enhancements.EnhancementTabController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class QuirksController extends EnhancementTabController {
	@FXML
	private ScrollPane pane;
	@FXML
	private TableView<QuirkEnhancement> table;
	@FXML
	private TableColumn<QuirkEnhancement, String> nameColumn;
	@FXML
	private TableColumn<QuirkEnhancement, Integer> sesColumn;
	@FXML
	private TableColumn<QuirkEnhancement, Integer> startColumn;
	@FXML
	private TableColumn<QuirkEnhancement, Integer> targetColumn;
	@FXML
	private TableColumn<QuirkEnhancement, Integer> apColumn;
	@FXML
	private TableColumn<QuirkEnhancement, Boolean> validColumn;
	@FXML
	private TableColumn<QuirkEnhancement, Boolean> cheaperColumn;

	public QuirksController(final EnhancementController controller, final TabPane tabPane) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("Quirks.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		setTab(tabPane);

		table.prefWidthProperty().bind(pane.widthProperty().subtract(20));

		GUIUtil.autosizeTable(table, 0, 2);
		GUIUtil.cellValueFactories(table, "description", "ses", "start", "target", "ap", "valid", "cheaper");

		nameColumn.setCellFactory(c -> new TextFieldTableCell<QuirkEnhancement, String>() {
			@Override
			public void updateItem(final String item, final boolean empty) {
				super.updateItem(item, empty);
				final QuirkEnhancement quirk = (QuirkEnhancement) getTableRow().getItem();
				if (quirk != null) {
					Util.addReference(this, quirk.getQuirk().getProOrCon(), 50);
				}
			}
		});

		sesColumn.setCellFactory(
				IntegerSpinnerTableCell.<QuirkEnhancement> forTableColumn(0, 0, 1, false,
						(final IntegerSpinnerTableCell<QuirkEnhancement> cell, final Boolean empty) -> {
							if (empty) return new Tuple<>(0, 0);
							final int seMin = cell.getTableView().getItems().get(cell.getIndex()).getSeMin();
							final int seMax = cell.getTableView().getItems().get(cell.getIndex()).getStart();
							return new Tuple<>(seMin, seMax);
						}));
		sesColumn.setOnEditCommit(t -> {
			t.getRowValue().setSes(t.getNewValue(), hero);
		});

		targetColumn.setCellFactory(
				IntegerSpinnerTableCell.forTableColumn(0, 50, 1, false, (final IntegerSpinnerTableCell<QuirkEnhancement> cell, final Boolean empty) -> {
					if (empty) return new Tuple<>(0, 0);
					return new Tuple<>(0, cell.getTableView().getItems().get(cell.getIndex()).getStart() - 1);
				}));
		targetColumn.setOnEditCommit(t -> {
			t.getRowValue().setTarget(t.getNewValue(), hero, EnhancementController.instance.getEnhancements());
		});

		final ContextMenu attributesContextMenu = new ContextMenu();
		final MenuItem attributesContextMenuItem = new MenuItem("Senken");
		attributesContextMenu.getItems().add(attributesContextMenuItem);
		attributesContextMenuItem.setOnAction(o -> {
			final QuirkEnhancement item = table.getSelectionModel().getSelectedItem();
			if (item != null) {
				EnhancementController.instance.addEnhancement(item.clone(hero, EnhancementController.instance.getEnhancements()));
				update();
			}
		});
		table.setContextMenu(attributesContextMenu);

		validColumn.setCellFactory(tableColumn -> new TextFieldTableCell<QuirkEnhancement, Boolean>() {
			@Override
			public void updateItem(final Boolean valid, final boolean empty) {
				super.updateItem(valid, empty);
				@SuppressWarnings("all")
				final TableRow<QuirkEnhancement> row = getTableRow();
				row.getStyleClass().remove("invalid");
				if (!empty && !valid) {
					row.getStyleClass().remove("valid");
					row.getStyleClass().add("invalid");
				}
			}
		});

		cheaperColumn.setCellFactory(tableColumn -> new TextFieldTableCell<QuirkEnhancement, Boolean>() {
			@Override
			public void updateItem(final Boolean cheaper, final boolean empty) {
				super.updateItem(cheaper, empty);
				@SuppressWarnings("all")
				final TableRow<QuirkEnhancement> row = getTableRow();
				final QuirkEnhancement item = row.getItem();
				row.getStyleClass().remove("valid");
				if (!empty && item != null && item.isValid() && cheaper) {
					row.getStyleClass().remove("invalid");
					row.getStyleClass().add("valid");
				}
			}
		});
	}

	@Override
	protected Node getControl() {
		return pane;
	}

	@Override
	protected String getText() {
		return "Schl. Eigenschaften";
	}

	@Override
	public void recalculate(final JSONObject hero) {
		for (final QuirkEnhancement enhancement : table.getItems()) {
			enhancement.reset(hero);
		}
	}

	@Override
	public void recalculateValid(final JSONObject hero) {
		update();
		for (final QuirkEnhancement enhancement : table.getItems()) {
			enhancement.recalculateValid(hero);
		}
	}

	@Override
	public void setHero(final JSONObject hero) {
		if (hero != null) {
			hero.getObj("Nachteile").removeListener(heroListener);
		}
		this.hero = hero;
		hero.getObj("Nachteile").addListener(heroListener);
		update();
	}

	@Override
	public void update() {
		table.getItems().clear();

		final JSONObject cons = ResourceManager.getResource("data/Nachteile");
		final JSONObject actualCons = hero.getObj("Nachteile");

		quirks: for (final String conName : actualCons.keySet()) {
			final JSONObject con = cons.getObj(conName);
			if (con.getBoolOrDefault("Schlechte Eigenschaft", false)) {
				if (con.containsKey("Auswahl") || con.containsKey("Freitext")) {
					final JSONArray conArray = actualCons.getArr(conName);
					for (int i = 0; i < conArray.size(); ++i) {
						final JSONObject actualCon = conArray.getObj(i);
						for (final Enhancement enhancement : EnhancementController.instance.getEnhancements()) {
							if (enhancement instanceof QuirkEnhancement && conName.equals(enhancement.getName())) {
								final QuirkEnhancement quirkEnhancement = (QuirkEnhancement) enhancement;
								if (con.containsKey("Auswahl")
										&& !quirkEnhancement.getQuirk().getActual().getString("Auswahl").equals(actualCon.getString("Auswahl"))) {
									continue;
								}
								if (con.containsKey("Freitext")
										&& !quirkEnhancement.getQuirk().getActual().getString("Freitext").equals(actualCon.getString("Freitext"))) {
									continue;
								}
								continue quirks;
							}
						}
						table.getItems().add(new QuirkEnhancement(new ProOrCon(conName, hero, con, actualCon), hero));
					}
				} else {
					for (final Enhancement enhancement : EnhancementController.instance.getEnhancements()) {
						if (enhancement instanceof QuirkEnhancement && conName.equals(enhancement.getName())) {
							continue quirks;
						}
					}
					table.getItems().add(new QuirkEnhancement(new ProOrCon(conName, hero, con, actualCons.getObj(conName)), hero));
				}
			}
		}

		table.setMaxHeight(table.getItems().size() * 28 + 27);
	}
}
