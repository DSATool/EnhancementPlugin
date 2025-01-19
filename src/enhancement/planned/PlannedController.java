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
package enhancement.planned;

import java.util.HashMap;
import java.util.Map;

import dsa41basis.ui.hero.HeroController;
import dsatool.gui.GUIUtil;
import dsatool.resources.Settings;
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
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class PlannedController extends EnhancementTabController {

	@FXML
	private VBox root;
	@FXML
	private TableView<Enhancement> table;
	@FXML
	private TableColumn<Enhancement, String> descriptionColumn;
	@FXML
	private TableColumn<Enhancement, Double> costColumn;
	@FXML
	private TableColumn<Enhancement, Integer> apColumn;
	@FXML
	private TableColumn<Enhancement, Boolean> validColumn;
	@FXML
	private TableColumn<Enhancement, Boolean> cheaperColumn;
	@FXML
	private TextField filter;

	private final EnhancementController controller;

	private final ObservableList<Enhancement> items = FXCollections.observableArrayList();
	private final Map<Enhancement, JSONObject> itemObjectMap = new HashMap<>();

	public PlannedController(final EnhancementController controller, final TabPane tabPane) {
		super(tabPane);

		this.controller = controller;
	}

	@Override
	protected Node getControl() {
		return root;
	}

	@Override
	protected String getText() {
		return "Vorgemerkt";
	}

	@Override
	protected void init() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("Planned.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		table.prefWidthProperty().bind(root.widthProperty().subtract(20));

		if (!Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten")) {
			costColumn.setMinWidth(0);
			costColumn.setPrefWidth(0);
			costColumn.setMaxWidth(0);
		}

		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("fullDescription"));
		costColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
		apColumn.setCellValueFactory(new PropertyValueFactory<>("ap"));

		validColumn.setCellValueFactory(new PropertyValueFactory<>("valid"));
		validColumn.setCellFactory(tableColumn -> new TableCell<>() {
			@Override
			public void updateItem(final Boolean valid, final boolean empty) {
				super.updateItem(valid, empty);
				@SuppressWarnings("all")
				final TableRow<Enhancement> row = getTableRow();
				row.getStyleClass().remove("invalid");
				row.setTooltip(null);
				if (!empty && !valid) {
					row.getStyleClass().remove("valid");
					row.getStyleClass().add("invalid");
					final Tooltip tooltip = new Tooltip();
					tooltip.setOnShowing(o -> {
						tooltip.setText(row.getItem().getInvalidReason(hero));
					});
					row.setTooltip(tooltip);
				}
			}
		});

		cheaperColumn.setCellValueFactory(new PropertyValueFactory<>("cheaper"));
		cheaperColumn.setCellFactory(tableColumn -> new TableCell<>() {
			@Override
			public void updateItem(final Boolean cheaper, final boolean empty) {
				super.updateItem(cheaper, empty);
				@SuppressWarnings("all")
				final TableRow<Enhancement> row = getTableRow();
				final Enhancement item = row.getItem();
				row.getStyleClass().remove("valid");
				if (!empty && item != null && item.isValid() && cheaper) {
					row.getStyleClass().remove("invalid");
					row.getStyleClass().add("valid");
				}
			}
		});

		table.setRowFactory(t -> {
			final TableRow<Enhancement> row = new TableRow<>();

			final ContextMenu contextMenu = new ContextMenu();

			final MenuItem applyItem = new MenuItem("Steigern");
			contextMenu.getItems().add(applyItem);
			applyItem.setOnAction(o -> {
				final Enhancement item = row.getItem();
				hero.getArr("Vorgemerkte Steigerungen").remove(itemObjectMap.get(item));
				items.remove(item);
				EnhancementController.instance.addEnhancement(item.clone(hero, EnhancementController.instance.getEnhancements()));
			});

			final MenuItem removeItem = new MenuItem("Entfernen");
			contextMenu.getItems().add(removeItem);
			removeItem.setOnAction(o -> {
				final Enhancement removed = row.getItem();
				for (final HeroController controller : EnhancementController.instance.getControllers()) {
					if (((EnhancementTabController) controller).removeEnhancement(removed)) {
						break;
					}
				}
				hero.getArr("Vorgemerkte Steigerungen").remove(itemObjectMap.get(removed));
				items.remove(removed);
			});

			row.contextMenuProperty().bind(Bindings.when(row.itemProperty().isNotNull()).then(contextMenu).otherwise((ContextMenu) null));

			return row;
		});

		final FilteredList<Enhancement> filtered = items.filtered(i -> true);
		table.setItems(filtered);
		GUIUtil.autosizeTable(table);

		filter.textProperty().addListener((o, oldV, newV) -> filtered.setPredicate(i -> i.getFullDescription().toLowerCase().contains(newV.toLowerCase())));
	}

	@Override
	public void recalculate(final JSONObject hero) {}

	@Override
	public void recalculateValid(final JSONObject hero) {}

	@Override
	protected void registerListeners() {
		// TODO!
		hero.getArr("Vorgemerkte Steigerungen").addListener(heroListener);
	}

	@Override
	public boolean removeEnhancement(final Enhancement enhancement) {
		return false;
	}

	@Override
	protected void unregisterListeners() {
		// TODO!
		hero.getArr("Vorgemerkte Steigerungen").removeListener(heroListener);
	}

	@Override
	public void update() {
		items.clear();
		itemObjectMap.clear();

		final JSONArray planned = hero.getArr("Vorgemerkte Steigerungen");
		for (int i = planned.size() - 1; i >= 0; --i) {
			final JSONObject entry = planned.getObj(i);
			final Enhancement enhancement = switch (entry.getString("Typ")) {
				case "Eigenschaft" -> AttributeEnhancement.fromJSON(entry, hero, true);
				case "Basiswert" -> EnergyEnhancement.fromJSON(entry, hero, true);
				case "Schlechte Eigenschaft" -> QuirkEnhancement.fromJSON(entry, hero, controller.getEnhancements(), true);
				case "Sonderfertigkeit" -> SkillEnhancement.fromJSON(entry, hero, true);
				case "Talent" -> TalentEnhancement.fromJSON(entry, hero, true);
				case "Zauber" -> SpellEnhancement.fromJSON(entry, hero, true);
				default -> null;
			};
			items.add(enhancement);
			itemObjectMap.put(enhancement, entry);
		}
	}
}
