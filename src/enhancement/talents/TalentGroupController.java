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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dsa41basis.hero.Spell;
import dsa41basis.hero.Talent;
import dsa41basis.util.DSAUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.ErrorLogger;
import dsatool.util.GraphicTableCell;
import dsatool.util.IntegerSpinnerTableCell;
import dsatool.util.ReactiveSpinner;
import dsatool.util.Tuple;
import dsatool.util.Util;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Region;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class TalentGroupController {

	@FXML
	private Region pane;
	@FXML
	private TableView<TalentEnhancement> table;
	@FXML
	private TableColumn<TalentEnhancement, String> nameColumn;
	@FXML
	private TableColumn<TalentEnhancement, Integer> sesColumn;
	@FXML
	private TableColumn<TalentEnhancement, String> startColumn;
	@FXML
	private TableColumn<TalentEnhancement, String> targetColumn;
	@FXML
	private TableColumn<TalentEnhancement, String> methodColumn;
	@FXML
	private TableColumn<TalentEnhancement, Integer> costColumn;
	@FXML
	private TableColumn<TalentEnhancement, Boolean> validColumn;
	@FXML
	private TableColumn<TalentEnhancement, Boolean> cheaperColumn;
	@FXML
	private ComboBox<String> representationsList;
	@FXML
	private ComboBox<String> talentsList;
	@FXML
	private Button addButton;

	private final EnhancementController controller;

	private final JSONObject talents;
	private final String talentGroup;
	protected JSONObject hero;

	public TalentGroupController(final ScrollPane parent, final EnhancementController controller, final String name, final JSONObject talents) {
		this.controller = controller;
		this.talents = talents;
		talentGroup = name;

		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			if ("Zauber".equals(name)) {
				fxmlLoader.load(getClass().getResource("Spells.fxml").openStream());
			} else {
				fxmlLoader.load(getClass().getResource("TalentGroup.fxml").openStream());
			}
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		if (!"Zauber".equals(name)) {
			((TitledPane) pane).setText(name);
		}

		table.prefWidthProperty().bind(pane.widthProperty().subtract(2));

		nameColumn.getStyleClass().add("left-aligned");
		if ("Zauber".equals(name)) {
			nameColumn.setText("Zauber");
		}

		GUIUtil.autosizeTable(table, 0, "Zauber".equals(name) ? 2 : 0);
		GUIUtil.cellValueFactories(table, "description", "ses", "startString", "targetString", "method", "cost", "valid", "cheaper");

		nameColumn.setCellFactory(c -> new TextFieldTableCell<TalentEnhancement, String>() {
			@Override
			public void updateItem(final String item, final boolean empty) {
				super.updateItem(item, empty);
				final TalentEnhancement talent = (TalentEnhancement) getTableRow().getItem();
				if (talent != null) {
					Util.addReference(this, talent.getTalent().getTalent(), 50);
				}
			}
		});

		sesColumn.setCellFactory(
				IntegerSpinnerTableCell.<TalentEnhancement> forTableColumn(0, 0, 1, false,
						(final IntegerSpinnerTableCell<TalentEnhancement> cell, final Boolean empty) -> {
							if (empty) return new Tuple<>(0, 0);
							final int seMin = cell.getTableView().getItems().get(cell.getIndex()).getSeMin();
							return new Tuple<>(seMin, 99);
						}));
		sesColumn.setOnEditCommit(t -> {
			t.getTableView().getItems().get(t.getTablePosition().getRow()).setSes(t.getNewValue(), hero);
		});

		targetColumn.setCellFactory(o -> new GraphicTableCell<TalentEnhancement, String>(false) {
			@Override
			protected void createGraphic() {
				final List<String> entries = new LinkedList<>();
				final TalentEnhancement item = getTableView().getItems().get(getIndex());
				int cur = item.getStart() + 1;
				if (cur < 0) {
					if (!item.isBasis()) {
						++cur;
					}
					for (; cur < 0; ++cur) {
						entries.add(String.valueOf(cur));
					}
					if (!item.isBasis()) {
						entries.add("n.a.");
					}
				}
				for (; cur <= 50; ++cur) {
					entries.add(String.valueOf(cur));
				}
				final ReactiveSpinner<String> spinner = new ReactiveSpinner<>(FXCollections.observableList(entries));
				spinner.setEditable(true);
				createGraphic(spinner, () -> spinner.getValue(), t -> spinner.getValueFactory().setValue(t));
			}

			@Override
			public void startEdit() {
				if (getItem() == null || getItem() == "50") return;
				super.startEdit();
			}

			@Override
			public void updateItem(final String item, final boolean empty) {
				if (empty) {
					setText("");
					setGraphic(null);
				} else {
					super.updateItem(item, empty);
				}
			}
		});
		targetColumn.setOnEditCommit(t -> {
			t.getTableView().getItems().get(t.getTablePosition().getRow()).setTarget(t.getNewValue(), hero, controller.getEnhancements());
		});

		methodColumn.setCellFactory(ComboBoxTableCell.forTableColumn("Lehrmeister", "Gegenseitiges Lehren", "Selbststudium"));
		methodColumn.setOnEditCommit((final CellEditEvent<TalentEnhancement, String> t) -> {
			t.getTableView().getItems().get(t.getTablePosition().getRow()).setMethod(t.getNewValue(), hero);
		});

		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem contextMenuItem = new MenuItem("Steigern");
		contextMenu.getItems().add(contextMenuItem);
		contextMenuItem.setOnAction(o -> {
			final TalentEnhancement item = table.getSelectionModel().getSelectedItem();
			if (item != null) {
				controller.addEnhancement(item.clone(hero, controller.getEnhancements()));
				fillTable();
			}
		});
		table.setContextMenu(contextMenu);

		validColumn.setCellFactory(tableColumn -> new TextFieldTableCell<TalentEnhancement, Boolean>() {
			@Override
			public void updateItem(final Boolean valid, final boolean empty) {
				super.updateItem(valid, empty);
				@SuppressWarnings("all")
				final TableRow<TalentEnhancement> row = getTableRow();
				row.getStyleClass().remove("invalid");
				if (!empty && !valid) {
					row.getStyleClass().remove("valid");
					row.getStyleClass().add("invalid");
				}
			}
		});

		cheaperColumn.setCellFactory(tableColumn -> new TextFieldTableCell<TalentEnhancement, Boolean>() {
			@Override
			public void updateItem(final Boolean cheaper, final boolean empty) {
				super.updateItem(cheaper, empty);
				@SuppressWarnings("all")
				final TableRow<TalentEnhancement> row = getTableRow();
				final TalentEnhancement item = row.getItem();
				row.getStyleClass().remove("valid");
				if (!empty && item != null && item.isValid() && cheaper) {
					row.getStyleClass().remove("invalid");
					row.getStyleClass().add("valid");
				}
			}
		});

		if (representationsList != null) {
			talentsList.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
				if (newValue != null) {
					representationsList.getItems().clear();
					final JSONObject spell = talents.getObj(newValue);
					final Set<String> representations = spell.getObj("Repräsentationen").keySet();
					final JSONObject actual = hero.getObj("Zauber");
					final JSONObject actualSpell = actual.getObjOrDefault(newValue, null);
					if (actualSpell == null) {
						representationsList.getItems().setAll(representations);
					} else {
						for (final String representation : representations) {
							if (!actualSpell.containsKey(representation) || spell.containsKey("Auswahl") || spell.containsKey("Freitext")) {
								representationsList.getItems().add(representation);
							}
						}
					}
					representationsList.getSelectionModel().select(0);
				}
			});
		}
	}

	@FXML
	private void addTalent() {
		final String talentName = talentsList.getSelectionModel().getSelectedItem();
		final JSONObject talentGroups = ResourceManager.getResource("data/Talente");
		final JSONObject group = "Zauber".equals(talentGroup) ? ResourceManager.getResource("data/Zauber") : talentGroups.getObj(talentGroup);
		final JSONObject actual = "Zauber".equals(talentGroup) ? hero.getObj("Zauber") : hero.getObj("Talente").getObj(talentGroup);

		if ("Zauber".equals(talentGroup)) {
			final String representation = representationsList.getSelectionModel().getSelectedItem();
			table.getItems()
					.add(new SpellEnhancement(new Spell(talentName, group.getObj(talentName), null, actual.getObj(talentName), actual, representation), hero));
		} else {
			table.getItems()
					.add(new TalentEnhancement(new Talent(talentName, group, group.getObj(talentName), null, actual), talentGroup, hero));
		}
		table.setPrefHeight(table.getItems().size() * 28 + 26);
	}

	protected void fillTable() {
		talentsList.getItems().clear();
		table.getItems().forEach(e -> e.unregister());
		table.getItems().clear();

		final JSONObject talentGroups = ResourceManager.getResource("data/Talentgruppen");
		final JSONObject actualGroup = "Zauber".equals(talentGroup) ? hero.getObj("Zauber") : hero.getObj("Talente").getObj(talentGroup);

		DSAUtil.foreach(talent -> true, (talentName, talent) -> {
			final List<JSONObject> actualTalents;
			if (actualGroup == null) {
				actualTalents = null;
			} else if (!"Zauber".equals(talentGroup) && actualGroup.containsKey(talentName)
					&& (talent.containsKey("Auswahl") || talent.containsKey("Freitext"))) {
				actualTalents = new LinkedList<>();
				final JSONArray choiceTalent = actualGroup.getArrOrDefault(talentName, null);
				if (choiceTalent != null) {
					for (int i = 0; i < choiceTalent.size(); ++i) {
						actualTalents.add(choiceTalent.getObj(i));
					}
				}
				talentsList.getItems().add(talentName);
			} else if (actualGroup.containsKey(talentName)) {
				actualTalents = Collections.singletonList(actualGroup.getObj(talentName));
			} else {
				actualTalents = null;
			}
			if (actualTalents == null) {
				talentsList.getItems().add(talentName);
				return;
			}

			talents: for (final JSONObject actualTalent : actualTalents) {
				if ("Zauber".equals(talentGroup)) {
					boolean notFound = false;
					reps: for (final String rep : talent.getObj("Repräsentationen").keySet()) {
						if (actualTalent.containsKey(rep)) {
							if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
								final JSONArray choiceTalent = actualTalent.getArrOrDefault(rep, null);
								if (choiceTalent != null) {
									choices: for (int i = 0; i < choiceTalent.size(); ++i) {
										for (final Enhancement enhancement : controller.getEnhancements()) {
											if (enhancement instanceof SpellEnhancement
													&& ((SpellEnhancement) enhancement).getTalent().getActual() == choiceTalent.getObj(i)) {
												continue choices;
											}
										}
										table.getItems().add(new SpellEnhancement(
												new Spell(talentName, talent, choiceTalent.getObj(i), actualTalent, actualGroup, rep), hero));
									}
								}
							} else {
								for (final Enhancement enhancement : controller.getEnhancements()) {
									if (enhancement instanceof SpellEnhancement
											&& ((SpellEnhancement) enhancement).getTalent().getActual() == actualTalent.getObj(rep)) {
										continue reps;
									}
								}
								table.getItems().add(
										new SpellEnhancement(new Spell(talentName, talent, actualTalent.getObj(rep), actualTalent, actualGroup, rep), hero));
							}
						} else {
							notFound = true;
						}
					}
					if (notFound) {
						talentsList.getItems().add(talentName);
					}
				} else {
					for (final Enhancement enhancement : controller.getEnhancements()) {
						if (enhancement instanceof TalentEnhancement && ((TalentEnhancement) enhancement).getTalent().getActual() == actualTalent) {
							continue talents;
						}
					}
					table.getItems().add(new TalentEnhancement(
							new Talent(talentName, talentGroups.getObj(talentGroup), talents.getObj(talentName), actualTalent, actualGroup),
							talentGroup, hero));
				}
			}
		}, talents);

		if (talentsList.getItems().size() > 0) {
			talentsList.getSelectionModel().select(0);
			addButton.setDisable(false);
		} else {
			addButton.setDisable(true);
		}

		table.setPrefHeight(table.getItems().size() * 28 + 26);
	}

	public Node getControl() {
		return pane;
	}

	public void recalculateCost(final JSONObject hero2) {
		for (final TalentEnhancement enhancement : table.getItems()) {
			enhancement.resetCost(hero);
		}
	}

	public void recalculateValid(final JSONObject hero) {
		for (final TalentEnhancement enhancement : table.getItems()) {
			enhancement.recalculateValid(hero);
		}
	}

	public void setHero(final JSONObject hero) {
		this.hero = hero;
		fillTable();
	}
}
