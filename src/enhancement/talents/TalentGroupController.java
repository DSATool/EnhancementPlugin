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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dsa41basis.hero.Spell;
import dsa41basis.hero.Talent;
import dsa41basis.util.DSAUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.util.ErrorLogger;
import dsatool.util.GraphicTableCell;
import dsatool.util.IntegerSpinnerTableCell;
import dsatool.util.ReactiveComboBoxTableCell;
import dsatool.util.ReactiveSpinner;
import dsatool.util.Tuple;
import dsatool.util.Util;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Region;
import jsonant.event.JSONListener;
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
	private TableColumn<TalentEnhancement, Double> costColumn;
	@FXML
	private TableColumn<TalentEnhancement, Integer> apColumn;
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

	private final JSONObject talents;
	private final String talentGroup;
	protected JSONObject hero;

	private final JSONListener listener = o -> {
		recalculateValid(hero);
	};

	private final List<TalentEnhancement> alreadyEnhanced = new ArrayList<>();

	public TalentGroupController(final String name, final JSONObject talents) {
		this.talents = talents;
		talentGroup = name;

		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			if ("Zauber".equals(name)) {
				fxmlLoader.load(getClass().getResource("SpellsGroup.fxml").openStream());
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
		table.getSortOrder().add(nameColumn);

		if ("Zauber".equals(name)) {
			nameColumn.setText("Zauber");
		}

		if (!Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten")) {
			costColumn.setMinWidth(0);
			costColumn.setPrefWidth(0);
			costColumn.setMaxWidth(0);
		}

		GUIUtil.autosizeTable(table, 0, "Zauber".equals(name) ? 2 : 0);
		GUIUtil.cellValueFactories(table, "description", "ses", "startString", "targetString", "method", "cost", "ap", "valid", "cheaper");

		nameColumn.setCellFactory(c -> new TextFieldTableCell<TalentEnhancement, String>() {
			@Override
			public void updateItem(final String item, final boolean empty) {
				super.updateItem(item, empty);
				final TalentEnhancement talent = (TalentEnhancement) getTableRow().getItem();
				if (talent != null) {
					Util.addReference(this, talent.getTalent().getTalent(), 15, nameColumn.widthProperty());
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
			t.getRowValue().setSes(t.getNewValue(), hero);
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
			t.getRowValue().setTarget(t.getNewValue(), hero);
		});

		methodColumn.setCellFactory(ReactiveComboBoxTableCell.forTableColumn(false, "Lehrmeister", "Gegenseitiges Lehren", "Selbststudium"));
		methodColumn.setOnEditCommit((final CellEditEvent<TalentEnhancement, String> t) -> {
			t.getRowValue().setMethod(t.getNewValue(), hero);
		});

		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem contextMenuItem = new MenuItem("Steigern");
		contextMenu.getItems().add(contextMenuItem);
		contextMenuItem.setOnAction(o -> {
			final TalentEnhancement item = table.getSelectionModel().getSelectedItem();
			if (item != null) {
				table.getItems().remove(item);
				alreadyEnhanced.add(item);
				EnhancementController.instance.addEnhancement(item.clone(hero));
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
		final String representation = "Zauber".equals(talentGroup) ? representationsList.getSelectionModel().getSelectedItem() : null;
		if ("Zauber".equals(talentGroup)) {
			representationsList.getItems().remove(representation);
			if (representationsList.getItems().size() == 0) {
				talentsList.getItems().remove(talentName);
			}
		} else {
			talentsList.getItems().remove(talentName);
		}

		if (talentsList.getItems().size() > 0) {
			talentsList.getSelectionModel().select(0);
		} else {
			addButton.setDisable(true);
		}

		final JSONObject talentGroups = ResourceManager.getResource("data/Talente");
		final JSONObject group = "Zauber".equals(talentGroup) ? ResourceManager.getResource("data/Zauber") : talentGroups.getObj(talentGroup);
		final JSONObject actualGroup = "Zauber".equals(talentGroup) ? hero.getObj("Zauber") : hero.getObj("Talente").getObj(talentGroup);

		if ("Zauber".equals(talentGroup)) {
			table.getItems()
					.add(new SpellEnhancement(Spell.getSpell(talentName, group.getObj(talentName), null, null, actualGroup, representation), hero));
		} else {
			table.getItems()
					.add(new TalentEnhancement(Talent.getTalent(talentName, group, group.getObj(talentName), null, actualGroup), talentGroup, hero));
		}
		table.setPrefHeight(table.getItems().size() * 28 + 26);
		table.sort();
	}

	protected void fillTable() {
		alreadyEnhanced.clear();
		talentsList.getItems().clear();
		table.getItems().forEach(e -> e.unregister());
		table.getItems().clear();

		final JSONObject talentGroups = ResourceManager.getResource("data/Talentgruppen");
		final JSONObject actualGroup = "Zauber".equals(talentGroup) ? hero.getObj("Zauber") : hero.getObj("Talente").getObj(talentGroup);

		DSAUtil.foreach(talent -> true, (talentName, talent) -> {
			if (actualGroup.containsKey(talentName)) {
				if ("Zauber".equals(talentGroup)) {
					final JSONObject actualSpell = actualGroup.getObj(talentName);
					boolean notFound = false;
					for (final String rep : talent.getObj("Repräsentationen").keySet()) {
						if (actualSpell.containsKey(rep)) {
							if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
								final JSONArray choiceTalent = actualSpell.getArr(rep);
								for (int i = 0; i < choiceTalent.size(); ++i) {
									table.getItems().add(new SpellEnhancement(
											Spell.getSpell(talentName, talent, choiceTalent.getObj(i), actualSpell, actualGroup, rep), hero));
								}
							} else {
								table.getItems().add(
										new SpellEnhancement(Spell.getSpell(talentName, talent, actualSpell.getObj(rep), actualSpell, actualGroup, rep), hero));
							}
						} else {
							notFound = true;
						}
					}
					if (notFound) {
						talentsList.getItems().add(talentName);
					}
				} else {
					if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
						final JSONArray choiceTalent = actualGroup.getArr(talentName);
						for (int i = 0; i < choiceTalent.size(); ++i) {
							table.getItems().add(new TalentEnhancement(
									Talent.getTalent(talentName, talentGroups.getObj(talentGroup), talents.getObj(talentName), choiceTalent.getObj(i),
											actualGroup),
									talentGroup, hero));
						}
						talentsList.getItems().add(talentName);
					} else {
						table.getItems().add(new TalentEnhancement(
								Talent.getTalent(talentName, talentGroups.getObj(talentGroup), talents.getObj(talentName), actualGroup.getObj(talentName),
										actualGroup),
								talentGroup, hero));
					}
				}
			} else {
				talentsList.getItems().add(talentName);
			}
		}, talents);

		if (talentsList.getItems().size() > 0) {
			talentsList.getSelectionModel().select(0);
			addButton.setDisable(false);
		} else {
			addButton.setDisable(true);
		}

		table.setPrefHeight(table.getItems().size() * 28 + 26);
		table.sort();
	}

	public Node getControl() {
		return pane;
	}

	public void recalculate(final JSONObject hero) {
		for (final TalentEnhancement enhancement : table.getItems()) {
			enhancement.reset(hero);
		}
	}

	public void recalculateValid(final JSONObject hero) {
		for (final TalentEnhancement enhancement : table.getItems()) {
			enhancement.recalculateValid(hero);
		}

		table.setPrefHeight(table.getItems().size() * 28 + 26);
		table.sort();
	}

	public boolean removeEnhancement(final TalentEnhancement enhancement) {
		if (enhancement.talentGroupName.equals(talentGroup)) {
			table.getItems().add(enhancement);
			return true;
		} else
			return false;
	}

	public void setHero(final JSONObject hero) {
		if (hero != null) {
			if ("Zauber".equals(talentGroup) && hero.containsKey("Zauber")) {
				hero.getObj("Zauber").removeListener(listener);
			} else {
				hero.getObj("Talente").removeListener(listener);
			}
		}
		this.hero = hero;
		if ("Zauber".equals(talentGroup) && hero.containsKey("Zauber")) {
			hero.getObj("Zauber").addListener(listener);
		} else {
			hero.getObj("Talente").addListener(listener);
		}
		fillTable();
	}
}
