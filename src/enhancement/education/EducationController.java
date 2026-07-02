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
package enhancement.education;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dsa41basis.hero.ProOrCon;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.RequirementsUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.ui.GraphicTableCell;
import dsatool.ui.ReactiveComboBox;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import dsatool.util.Util;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import enhancement.enhancements.EnhancementTabController;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class EducationController extends EnhancementTabController {
	@FXML
	private ScrollPane pane;
	@FXML
	private TableView<EducationEnhancement> table;
	@FXML
	private TableColumn<EducationEnhancement, String> nameColumn;
	@FXML
	private TableColumn<EducationEnhancement, String> descColumn;
	@FXML
	private TableColumn<EducationEnhancement, String> variantColumn;
	@FXML
	private TableColumn<EducationEnhancement, Double> costColumn;
	@FXML
	private TableColumn<EducationEnhancement, Integer> apColumn;
	@FXML
	private TableColumn<EducationEnhancement, Boolean> validColumn;

	private final ObservableSet<EducationEnhancement> valid = FXCollections.observableSet();
	private final ObservableList<EducationEnhancement> allItems = FXCollections.observableArrayList(_ -> new Observable[] { valid });

	public EducationController(final EnhancementController controller, final TabPane tabPane) {
		super(tabPane);
	}

	protected void fillTable() {
		valid.clear();
		allItems.clear();

		DSAUtil.foreach(_ -> true, (educationName, education) -> {
			final JSONObject requirements = education.getObj("Voraussetzungen").clone(null);
			final Set<String> keys = new HashSet<>(requirements.keySet());
			for (final String key : keys) {
				if (!List.of("Vorteile/Nachteile/Sonderfertigkeiten", "Rassen", "Kulturen", "Professionen").contains(key)) {
					requirements.removeKey(key);
				}
			}
			if (RequirementsUtil.isRequirementFulfilled(hero, requirements, null, null, false)) {
				final EducationEnhancement newEnhancement = new EducationEnhancement(new ProOrCon(educationName, hero, education, new JSONObject(null)), hero);
				if (newEnhancement.isValid()) {
					valid.add(newEnhancement);
				}
				allItems.add(newEnhancement);
			}
		}, ResourceManager.getResource("data/Weiterbildung"));
	}

	@Override
	protected Node getControl() {
		return pane;
	}

	@Override
	protected String getText() {
		return "Sonstiges";
	}

	@Override
	protected void init() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("Education.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		table.prefWidthProperty().bind(pane.widthProperty().subtract(20));

		nameColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		nameColumn.setCellFactory(_ -> new TextFieldTableCell<>() {
			@Override
			public void updateItem(final String item, final boolean empty) {
				super.updateItem(item, empty);
				if (getTableRow() != null) {
					final EducationEnhancement education = getTableRow().getItem();
					if (education != null) {
						Util.addReference(this, education.getEducation().getProOrCon(), 15, nameColumn.widthProperty());
					}
				}
			}
		});

		descColumn.setCellValueFactory(new PropertyValueFactory<>("choice"));
		descColumn.setCellFactory(_ -> new GraphicTableCell<>(false) {
			@Override
			protected void createGraphic() {
				final EducationEnhancement enhancement = getTableView().getItems().get(getIndex());
				final Collection<String> choices = enhancement.getChoices();
				if (choices != null && !choices.isEmpty()) {
					final ObservableList<String> items = FXCollections.observableArrayList(enhancement.getChoices());
					final ComboBox<String> c = new ReactiveComboBox<>(items);
					createGraphic(c, c.getSelectionModel()::getSelectedItem, c.getSelectionModel()::select);
				} else {
					final Label l = new Label();
					createGraphic(l, () -> "", _ -> {});
				}
			}
		});
		descColumn.setOnEditCommit(t -> {
			final EducationEnhancement enhancement = t.getRowValue();
			if (enhancement != null) {
				final Collection<String> choices = enhancement.getChoices();
				if (choices != null && !choices.isEmpty()) {
					enhancement.setChoice(t.getNewValue());
					final Collection<String> variants = enhancement.getVariants();
					if (!enhancement.getEducation().getProOrCon().getBoolOrDefault("Abgestuft", false)) {
						enhancement.variantProperty().set(variants == null || variants.isEmpty() ? "" : variants.iterator().next());
					}
					enhancement.reset(hero);
					enhancement.recalculateValid(hero);
				}
			}
		});

		variantColumn.setCellValueFactory(new PropertyValueFactory<>("variant"));
		variantColumn.setCellFactory(_ -> new GraphicTableCell<>(false) {
			@Override
			protected void createGraphic() {
				final EducationEnhancement enhancement = getTableView().getItems().get(getIndex());
				final Collection<String> variants = enhancement.getVariants();
				if (variants != null && !variants.isEmpty()) {
					final ObservableList<String> items = FXCollections.observableArrayList(enhancement.getVariants());
					final ComboBox<String> c = new ReactiveComboBox<>(items);
					createGraphic(c, c.getSelectionModel()::getSelectedItem, c.getSelectionModel()::select);
				} else if (enhancement.getEducation().getProOrCon().getBoolOrDefault("Abgestuft", false)) {
					final ReactiveSpinner<Integer> s = new ReactiveSpinner<>(0, 99);
					s.setEditable(true);
					createGraphic(s, () -> Integer.toString(s.getValue()), v -> {
						if (v != null) {
							s.getValueFactory().setValue(Integer.parseInt(v));
						}
					});
				} else {
					final Label l = new Label();
					createGraphic(l, () -> "", _ -> {});
				}
			}
		});
		variantColumn.setOnEditCommit(t -> {
			final EducationEnhancement enhancement = t.getRowValue();
			if (enhancement != null && enhancement.variantProperty() != null) {
				enhancement.variantProperty().set(t.getNewValue());
				enhancement.reset(hero);
				enhancement.recalculateValid(hero);
			}
		});

		costColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
		apColumn.setCellValueFactory(new PropertyValueFactory<>("ap"));

		validColumn.setCellValueFactory(new PropertyValueFactory<>("valid"));
		validColumn.setCellFactory(_ -> new TableCell<>() {
			@Override
			public void updateItem(final Boolean valid, final boolean empty) {
				super.updateItem(valid, empty);
				@SuppressWarnings("all")
				final TableRow<EducationEnhancement> row = getTableRow();
				row.getStyleClass().remove("invalid");
				row.setTooltip(null);
				if (!empty && !valid) {
					row.getStyleClass().remove("valid");
					row.getStyleClass().add("invalid");
					final Tooltip tooltip = new Tooltip();
					tooltip.setOnShowing(_ -> {
						tooltip.setText(row.getItem().getInvalidReason(hero));
					});
					row.setTooltip(tooltip);
				}
			}
		});

		table.setRowFactory(_ -> {
			final TableRow<EducationEnhancement> row = new TableRow<>();

			final ContextMenu contextMenu = new ContextMenu();

			final MenuItem applyItem = new MenuItem("Durchführen");
			contextMenu.getItems().add(applyItem);
			applyItem.setOnAction(_ -> {
				final EducationEnhancement item = row.getItem();
				EnhancementController.instance.addEnhancement(item.clone(hero, EnhancementController.instance.getEnhancements()));
			});

			final MenuItem planItem = new MenuItem("Vormerken");
			contextMenu.getItems().add(planItem);
			planItem.setOnAction(_ -> {
				final EducationEnhancement item = row.getItem();
				final JSONArray planned = hero.getArr("Vorgemerkte Steigerungen");
				planned.add(item.clone(hero, EnhancementController.instance.getEnhancements()).toJSON(planned, true));
				planned.notifyListeners(null);
			});

			row.contextMenuProperty().bind(Bindings.when(row.itemProperty().isNotNull()).then(contextMenu).otherwise((ContextMenu) null));

			return row;
		});

		table.setItems(new SortedList<>(allItems, Comparator.comparing(EducationEnhancement::getFullDescription)));

		GUIUtil.autosizeTable(table);
	}

	@Override
	public void recalculate(final JSONObject hero) {
		for (final EducationEnhancement enhancement : table.getItems()) {
			enhancement.reset(hero);
		}
	}

	@Override
	public void recalculateValid(final JSONObject hero) {
		valid.clear();
		for (final EducationEnhancement enhancement : allItems) {
			enhancement.recalculateValid(hero);
			if (enhancement.isValid()) {
				valid.add(enhancement);
			}
		}
	}

	@Override
	protected void registerListeners() {
		/* No listeners used here */
	}

	@Override
	public boolean removeEnhancement(final Enhancement enhancement) {
		return enhancement instanceof EducationEnhancement;
	}

	@Override
	protected void unregisterListeners() {
		/* No listeners used here */
	}

	@Override
	public void update() {
		fillTable();
	}
}
