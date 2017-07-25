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

import dsa41basis.hero.ProOrCon;
import dsa41basis.util.DSAUtil;
import dsatool.gui.GUIUtil;
import dsatool.util.ErrorLogger;
import dsatool.util.GraphicTableCell;
import dsatool.util.Util;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
	private TableColumn<SkillEnhancement, Integer> costColumn;
	@FXML
	private TableColumn<SkillEnhancement, Boolean> validColumn;
	@FXML
	private TableColumn<SkillEnhancement, Boolean> cheaperColumn;

	private final JSONObject skills;
	private final BooleanProperty showAll;
	private JSONObject hero;

	private final JSONListener listener = o -> {
		fillTable();
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

		nameColumn.getStyleClass().add("left-aligned");
		descColumn.getStyleClass().add("left-aligned");
		variantColumn.getStyleClass().add("left-aligned");

		GUIUtil.autosizeTable(table, 2, 0);

		nameColumn.setCellValueFactory(new PropertyValueFactory<SkillEnhancement, String>("description"));
		nameColumn.setCellFactory(c -> new TextFieldTableCell<SkillEnhancement, String>() {
			@Override
			public void updateItem(final String item, final boolean empty) {
				super.updateItem(item, empty);
				if (getTableRow() != null) {
					final SkillEnhancement skill = (SkillEnhancement) getTableRow().getItem();
					if (skill != null) {
						Util.addReference(this, skill.getSkill().getProOrCon(), 50);
					}
				}
			}
		});

		descColumn.setCellValueFactory(new PropertyValueFactory<SkillEnhancement, String>("skillDescription"));
		descColumn.setCellFactory(c -> new GraphicTableCell<SkillEnhancement, String>(false) {
			@Override
			protected void createGraphic() {
				final ObservableList<String> items = FXCollections
						.<String> observableArrayList(getTableView().getItems().get(getIndex()).getSkill().getFirstChoiceItems(false));
				switch (getTableView().getItems().get(getIndex()).getSkill().firstChoiceOrText()) {
				case TEXT:
					if (items.size() > 0) {
						final ComboBox<String> c = new ComboBox<>(items);
						c.setEditable(true);
						createGraphic(c, () -> c.getSelectionModel().getSelectedItem(), s -> c.getSelectionModel().select(s));
					} else {
						final TextField t = new TextField();
						createGraphic(t, () -> t.getText(), s -> t.setText(s));
					}
					break;
				case CHOICE:
					final ComboBox<String> c = new ComboBox<>(items);
					createGraphic(c, () -> c.getSelectionModel().getSelectedItem(), s -> c.getSelectionModel().select(s));
					break;
				case NONE:
					final Label l = new Label();
					createGraphic(l, () -> "", s -> {});
					break;
				}
			}
		});
		descColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).getSkill().setDescription(t.getNewValue()));

		variantColumn.setCellValueFactory(new PropertyValueFactory<SkillEnhancement, String>("skillVariant"));
		variantColumn.setCellFactory(c -> new GraphicTableCell<SkillEnhancement, String>(false) {
			@Override
			protected void createGraphic() {
				final ObservableList<String> items = FXCollections
						.<String> observableArrayList(getTableView().getItems().get(getIndex()).getSkill().getSecondChoiceItems(false));
				switch (getTableView().getItems().get(getIndex()).getSkill().secondChoiceOrText()) {
				case TEXT:
					if (items.size() > 0) {
						final ComboBox<String> c = new ComboBox<>(items);
						c.setEditable(true);
						createGraphic(c, () -> c.getSelectionModel().getSelectedItem(), s -> c.getSelectionModel().select(s));
					} else {
						final TextField t = new TextField();
						createGraphic(t, () -> t.getText(), s -> t.setText(s));
					}
					break;
				case CHOICE:
					final ComboBox<String> c = new ComboBox<>(items);
					createGraphic(c, () -> c.getSelectionModel().getSelectedItem(), s -> c.getSelectionModel().select(s));
					break;
				case NONE:
					final Label l = new Label();
					createGraphic(l, () -> "", s -> {});
					break;
				}
			}
		});
		variantColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).getSkill().setVariant(t.getNewValue()));

		costColumn.setCellValueFactory(new PropertyValueFactory<SkillEnhancement, Integer>("cost"));

		validColumn.setCellValueFactory(new PropertyValueFactory<SkillEnhancement, Boolean>("valid"));
		validColumn.setCellFactory(tableColumn -> new TableCell<SkillEnhancement, Boolean>() {
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
		cheaperColumn.setCellFactory(tableColumn -> new TableCell<SkillEnhancement, Boolean>() {
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

		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem contextMenuItem = new MenuItem("Erlernen");
		contextMenu.getItems().add(contextMenuItem);
		contextMenuItem.setOnAction(o -> {
			final SkillEnhancement item = table.getSelectionModel().getSelectedItem();
			if (item != null) {
				EnhancementController.instance.addEnhancement(item.clone(hero));
				fillTable();
			}
		});
		table.setContextMenu(contextMenu);

		showAll.addListener((o, oldV, newV) -> fillTable());
	}

	protected void fillTable() {
		table.getItems().clear();

		final JSONObject actual = hero.getObj("Sonderfertigkeiten");

		DSAUtil.foreach(skill -> true, (skillName, skill) -> {
			if (!actual.containsKey(skillName) || skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
				if (!skill.containsKey("Auswahl") && !skill.containsKey("Freitext")) {
					for (final Enhancement enhancement : EnhancementController.instance.getEnhancements()) {
						if (enhancement instanceof SkillEnhancement && skillName.equals(enhancement.getName())) return;
					}
				}
				final SkillEnhancement enhancement = new SkillEnhancement(new ProOrCon(skillName, hero, skill, new JSONObject(null)), hero);
				if (showAll.get() || enhancement.isValid()) {
					table.getItems().add(enhancement);
				}
			}
		}, skills);

		pane.setVisible(!table.getItems().isEmpty());
		pane.setManaged(!table.getItems().isEmpty());

		table.setPrefHeight(table.getItems().size() * 28 + 26);
	}

	public Node getControl() {
		return pane;
	}

	public void recalculateCost(final JSONObject hero2) {
		for (final SkillEnhancement enhancement : table.getItems()) {
			enhancement.resetCost(hero);
		}
	}

	public void recalculateValid(final JSONObject hero) {
		for (final SkillEnhancement enhancement : table.getItems()) {
			enhancement.recalculateValid(hero);
		}
	}

	public void setHero(final JSONObject hero) {
		if (this.hero != null) {
			this.hero.getObj("Sonderfertigkeiten").removeListener(listener);
		}
		this.hero = hero;
		fillTable();
		this.hero.getObj("Sonderfertigkeiten").addListener(listener);
	}
}
