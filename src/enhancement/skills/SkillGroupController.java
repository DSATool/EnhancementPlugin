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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import dsa41basis.hero.ProOrCon;
import dsa41basis.hero.ProOrCon.ChoiceOrTextEnum;
import dsa41basis.util.DSAUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.Settings;
import dsatool.ui.GraphicTableCell;
import dsatool.ui.ReactiveComboBox;
import dsatool.util.ErrorLogger;
import dsatool.util.Util;
import enhancement.enhancements.EnhancementController;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public class SkillGroupController {
	@FXML
	private TableColumn<SkillEnhancement, String> descColumn;
	@FXML
	private TableColumn<SkillEnhancement, String> nameColumn;
	@FXML
	private TitledPane pane;
	@FXML
	private TableView<SkillEnhancement> table;
	@FXML
	private TableColumn<SkillEnhancement, String> variantColumn;
	@FXML
	private TableColumn<SkillEnhancement, Double> costColumn;
	@FXML
	private TableColumn<SkillEnhancement, Integer> apColumn;
	@FXML
	private TableColumn<SkillEnhancement, Boolean> validColumn;
	@FXML
	private TableColumn<SkillEnhancement, Boolean> cheaperColumn;

	private final JSONObject skills;
	private final BooleanProperty showAll;
	private JSONObject hero;
	private final Set<String> alreadyEnhanced = new HashSet<>();

	private final ObservableSet<SkillEnhancement> valid = FXCollections.observableSet();
	private final ObservableList<SkillEnhancement> allItems = FXCollections.observableArrayList(item -> new Observable[] { valid });

	private final JSONListener listener = o -> {
		recalculateValid(hero);
	};

	public SkillGroupController(final ScrollPane parent, final String name, final JSONObject skills, final BooleanProperty showAll) {
		this.skills = skills;
		this.showAll = showAll;

		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("SkillGroup.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		pane.setText(name);

		table.prefWidthProperty().bind(parent.widthProperty().subtract(17));

		if (!Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten")) {
			costColumn.setMinWidth(0);
			costColumn.setPrefWidth(0);
			costColumn.setMaxWidth(0);
		}

		nameColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		nameColumn.setCellFactory(c -> new TextFieldTableCell<>() {
			@Override
			public void updateItem(final String item, final boolean empty) {
				super.updateItem(item, empty);
				if (getTableRow() != null) {
					final SkillEnhancement skill = getTableRow().getItem();
					if (skill != null) {
						Util.addReference(this, skill.getSkill().getProOrCon(), 15, nameColumn.widthProperty());
					}
				}
			}
		});

		descColumn.setCellValueFactory(new PropertyValueFactory<>("skillDescription"));
		descColumn.setCellFactory(c -> new GraphicTableCell<>(false) {
			@Override
			protected void createGraphic() {
				final ObservableList<String> items = FXCollections
						.<String> observableArrayList(getTableView().getItems().get(getIndex()).getSkill().getFirstChoiceItems(false));
				switch (getTableView().getItems().get(getIndex()).getSkill().firstChoiceOrText()) {
					case TEXT:
						if (items.size() > 0) {
							final ComboBox<String> c = new ReactiveComboBox<>(items);
							c.setEditable(true);
							createGraphic(c, c.getSelectionModel()::getSelectedItem, c.getSelectionModel()::select);
						} else {
							final TextField t = new TextField();
							createGraphic(t, t::getText, t::setText);
						}
						break;
					case CHOICE:
						final ComboBox<String> c = new ReactiveComboBox<>(items);
						createGraphic(c, c.getSelectionModel()::getSelectedItem, c.getSelectionModel()::select);
						break;
					case NONE:
						final Label l = new Label();
						createGraphic(l, () -> "", s -> {});
						break;
				}
			}
		});
		descColumn.setOnEditCommit(t -> {
			final SkillEnhancement enhancement = t.getRowValue();
			if (enhancement != null) {
				final ProOrCon skill = enhancement.getSkill();
				skill.setDescription(t.getNewValue(), false);
				if (skill.secondChoiceOrText() != ChoiceOrTextEnum.NONE) {
					final Set<String> variants = skill.getSecondChoiceItems(true);
					skill.setVariant(variants.isEmpty() ? "Spezialisierung" : variants.iterator().next(), false);
				}
				enhancement.reset(hero);
			}
		});

		variantColumn.setCellValueFactory(new PropertyValueFactory<>("skillVariant"));
		variantColumn.setCellFactory(c -> new GraphicTableCell<>(false) {
			@Override
			protected void createGraphic() {
				final ObservableList<String> items = FXCollections
						.<String> observableArrayList(getTableView().getItems().get(getIndex()).getSkill().getSecondChoiceItems(false));
				switch (getTableView().getItems().get(getIndex()).getSkill().secondChoiceOrText()) {
					case TEXT:
						if (items.size() > 0) {
							final ComboBox<String> c = new ReactiveComboBox<>(items);
							c.setEditable(true);
							createGraphic(c, c.getSelectionModel()::getSelectedItem, c.getSelectionModel()::select);
						} else {
							final TextField t = new TextField();
							createGraphic(t, t::getText, t::setText);
						}
						break;
					case CHOICE:
						final ComboBox<String> c = new ReactiveComboBox<>(items);
						createGraphic(c, c.getSelectionModel()::getSelectedItem, c.getSelectionModel()::select);
						break;
					case NONE:
						final Label l = new Label();
						createGraphic(l, () -> "", s -> {});
						break;
				}
			}
		});
		variantColumn.setOnEditCommit(t -> {
			final SkillEnhancement enhancement = t.getRowValue();
			if (enhancement != null) {
				enhancement.getSkill().setVariant(t.getNewValue(), false);
				enhancement.reset(hero);
			}
		});

		costColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
		apColumn.setCellValueFactory(new PropertyValueFactory<>("ap"));

		validColumn.setCellValueFactory(new PropertyValueFactory<>("valid"));
		validColumn.setCellFactory(tableColumn -> new TableCell<>() {
			@Override
			public void updateItem(final Boolean valid, final boolean empty) {
				super.updateItem(valid, empty);
				@SuppressWarnings("all")
				final TableRow<SkillEnhancement> row = getTableRow();
				row.getStyleClass().remove("invalid");
				if (!empty && !valid) {
					row.getStyleClass().remove("valid");
					row.getStyleClass().add("invalid");
				}
			}
		});

		cheaperColumn.setCellValueFactory(new PropertyValueFactory<SkillEnhancement, Boolean>("cheaper"));
		cheaperColumn.setCellFactory(tableColumn -> new TableCell<>() {
			@Override
			public void updateItem(final Boolean cheaper, final boolean empty) {
				super.updateItem(cheaper, empty);
				@SuppressWarnings("all")
				final TableRow<SkillEnhancement> row = getTableRow();
				final SkillEnhancement item = row.getItem();
				row.getStyleClass().remove("valid");
				if (!empty && item != null && item.isValid() && cheaper) {
					row.getStyleClass().remove("invalid");
					row.getStyleClass().add("valid");
				}
			}
		});

		table.setRowFactory(t -> {
			final TableRow<SkillEnhancement> row = new TableRow<>();

			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem contextMenuItem = new MenuItem("Erlernen");
			contextMenu.getItems().add(contextMenuItem);
			contextMenuItem.setOnAction(o -> {
				final SkillEnhancement item = row.getItem();
				final ProOrCon skill = item.getSkill();
				if (skill.firstChoiceOrText() == ChoiceOrTextEnum.NONE) {
					allItems.remove(item);
				}
				alreadyEnhanced.add(item.getName());
				EnhancementController.instance.addEnhancement(item.clone(hero));
			});
			row.contextMenuProperty().bind(Bindings.when(row.itemProperty().isNotNull()).then(contextMenu).otherwise((ContextMenu) null));

			return row;
		});

		valid.addListener((final SetChangeListener.Change<?> o) -> {
			final boolean hasItems = !valid.isEmpty();
			pane.setVisible(hasItems);
			pane.setManaged(hasItems);
		});

		pane.setVisible(false);
		pane.setManaged(false);

		table.setItems(new SortedList<>(new FilteredList<>(allItems, valid::contains), Comparator.comparing(SkillEnhancement::getName)));

		GUIUtil.autosizeTable(table);

		showAll.addListener((o, oldV, newV) -> {
			if (newV) {
				allItems.forEach(valid::add);
			} else {
				allItems.forEach(item -> {
					if (!item.isValid()) {
						valid.remove(item);
					}
				});
			}
		});
	}

	protected void fillTable() {
		valid.clear();
		allItems.clear();

		final JSONObject actual = hero.getObj("Sonderfertigkeiten");

		DSAUtil.foreach(skill -> true, (skillName, skill) -> {
			if (!actual.containsKey(skillName) || skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
				final SkillEnhancement newEnhancement = new SkillEnhancement(new ProOrCon(skillName, hero, skill, new JSONObject(null)), hero);
				if (skill.containsKey("Auswahl") || skill.containsKey("Freitext") || !alreadyEnhanced.contains(skillName)) {
					if (showAll.get() || newEnhancement.isValid()) {
						valid.add(newEnhancement);
						allItems.add(newEnhancement);
					} else if (!actual.containsKey(skillName)) {
						allItems.add(newEnhancement);
					}
				}
			}
		}, skills);
	}

	public Node getControl() {
		return pane;
	}

	public void recalculate(final JSONObject hero) {
		for (final SkillEnhancement enhancement : allItems) {
			enhancement.reset(hero);
		}
	}

	public void recalculateValid(final JSONObject hero) {
		valid.clear();
		final JSONObject actual = hero.getObj("Sonderfertigkeiten");
		for (final SkillEnhancement enhancement : allItems) {
			enhancement.recalculateValid(hero);
			if (showAll.get() || enhancement.isValid()) {
				final JSONObject skill = enhancement.getSkill().getProOrCon();
				if (!actual.containsKey(enhancement.getName()) || skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
					valid.add(enhancement);
				}
			}
		}

	}

	public void registerListeners() {
		hero.getObj("Sonderfertigkeiten").addListener(listener);
	}

	public boolean removeEnhancement(final SkillEnhancement enhancement) {
		if (skills.containsKey(enhancement.getName())) {
			alreadyEnhanced.remove(enhancement.getName());
			allItems.add(enhancement);
			return true;
		} else
			return false;
	}

	public void setHero(final JSONObject hero) {
		if (hero != this.hero || EnhancementController.instance.getEnhancements().isEmpty()) {
			alreadyEnhanced.clear();
		}
		this.hero = hero;
		fillTable();
	}

	public void unregisterListeners() {
		hero.getObj("Sonderfertigkeiten").removeListener(listener);
		valid.clear();
		allItems.clear();
	}
}
