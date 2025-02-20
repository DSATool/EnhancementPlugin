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
package enhancement.attributes;

import java.util.HashSet;
import java.util.Set;

import dsa41basis.hero.Attribute;
import dsa41basis.hero.Energy;
import dsa41basis.util.HeroUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.ui.IntegerSpinnerTableCell;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import enhancement.enhancements.EnhancementTabController;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class AttributesController extends EnhancementTabController {
	@FXML
	private ScrollPane pane;
	@FXML
	private TableView<AttributeEnhancement> attributesTable;
	@FXML
	private TableColumn<AttributeEnhancement, String> attributesNameColumn;
	@FXML
	private TableColumn<AttributeEnhancement, Integer> attributesSesColumn;
	@FXML
	private TableColumn<AttributeEnhancement, Integer> attributesStartColumn;
	@FXML
	private TableColumn<AttributeEnhancement, Integer> attributesTargetColumn;
	@FXML
	private TableColumn<AttributeEnhancement, Integer> attributesAPColumn;
	@FXML
	private TableColumn<AttributeEnhancement, Boolean> attributesValidColumn;
	@FXML
	private TableColumn<AttributeEnhancement, Boolean> attributesCheaperColumn;
	@FXML
	private TableView<EnergyEnhancement> energiesTable;
	@FXML
	private TableColumn<EnergyEnhancement, String> energiesNameColumn;
	@FXML
	private TableColumn<EnergyEnhancement, Integer> energiesSesColumn;
	@FXML
	private TableColumn<EnergyEnhancement, Integer> energiesStartColumn;
	@FXML
	private TableColumn<EnergyEnhancement, Integer> energiesTargetColumn;
	@FXML
	private TableColumn<EnergyEnhancement, Integer> energiesAPColumn;
	@FXML
	private TableColumn<EnergyEnhancement, Boolean> energiesValidColumn;
	@FXML
	private TableColumn<EnergyEnhancement, Boolean> energiesCheaperColumn;

	private final Set<String> alreadyEnhanced = new HashSet<>();

	public AttributesController(final EnhancementController controller, final TabPane tabPane) {
		super(tabPane);
	}

	@Override
	protected Node getControl() {
		return pane;
	}

	@Override
	protected String getText() {
		return "Eigenschaften";
	}

	@Override
	protected void init() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("Attributes.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		attributesTable.prefWidthProperty().bind(pane.widthProperty().subtract(22).divide(2));

		GUIUtil.autosizeTable(attributesTable);
		GUIUtil.cellValueFactories(attributesTable, "description", "ses", "start", "target", "ap", "valid", "cheaper");

		attributesSesColumn.setCellFactory(IntegerSpinnerTableCell.<AttributeEnhancement> forTableColumn(0, 0, 1, false,
				(final IntegerSpinnerTableCell<AttributeEnhancement> cell, final Boolean empty) -> {
					if (empty) return new Tuple<>(0, 0);
					return new Tuple<>(0, 99);
				}));
		attributesSesColumn.setOnEditCommit(t -> {
			if (t.getRowValue() != null) {
				t.getRowValue().setSes(t.getNewValue(), hero);
			}
		});

		attributesTargetColumn
				.setCellFactory(IntegerSpinnerTableCell.forTableColumn(0, 50, 1, false,
						(final IntegerSpinnerTableCell<AttributeEnhancement> cell, final Boolean empty) -> {
							if (empty) return new Tuple<>(0, 0);
							return new Tuple<>(cell.getTableView().getItems().get(cell.getIndex()).getStart() + 1, 50);
						}));
		attributesTargetColumn.setOnEditCommit(t -> {
			if (t.getRowValue() != null) {
				t.getRowValue().setTarget(t.getNewValue(), hero);
			}
		});

		attributesTable.setRowFactory(t -> {
			final TableRow<AttributeEnhancement> row = new TableRow<>();

			final ContextMenu attributesContextMenu = new ContextMenu();

			final MenuItem attributesApplyItem = new MenuItem("Steigern");
			attributesContextMenu.getItems().add(attributesApplyItem);
			attributesApplyItem.setOnAction(o -> {
				final AttributeEnhancement item = row.getItem();
				alreadyEnhanced.add(item.getName());
				EnhancementController.instance.addEnhancement(item.clone(hero, EnhancementController.instance.getEnhancements()));
				update();
			});

			final MenuItem planItem = new MenuItem("Vormerken");
			attributesContextMenu.getItems().add(planItem);
			planItem.setOnAction(o -> {
				final AttributeEnhancement item = row.getItem();
				alreadyEnhanced.add(item.getName());
				final JSONArray planned = hero.getArr("Vorgemerkte Steigerungen");
				planned.add(item.clone(hero, EnhancementController.instance.getEnhancements()).toJSON(planned, true));
				planned.notifyListeners(null);
				update();
			});

			row.contextMenuProperty().bind(Bindings.when(row.itemProperty().isNotNull()).then(attributesContextMenu).otherwise((ContextMenu) null));

			return row;
		});

		attributesValidColumn.setCellFactory(tableColumn -> new TextFieldTableCell<>() {
			@Override
			public void updateItem(final Boolean valid, final boolean empty) {
				super.updateItem(valid, empty);
				@SuppressWarnings("all")
				final TableRow<AttributeEnhancement> row = getTableRow();
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

		attributesCheaperColumn.setCellFactory(tableColumn -> new TextFieldTableCell<>() {
			@Override
			public void updateItem(final Boolean cheaper, final boolean empty) {
				super.updateItem(cheaper, empty);
				@SuppressWarnings("all")
				final TableRow<AttributeEnhancement> row = getTableRow();
				final AttributeEnhancement item = row.getItem();
				row.getStyleClass().remove("valid");
				if (!empty && item != null && item.isValid() && cheaper) {
					row.getStyleClass().remove("invalid");
					row.getStyleClass().add("valid");
				}
			}
		});

		energiesTable.prefWidthProperty().bind(pane.widthProperty().subtract(22).divide(2));

		GUIUtil.autosizeTable(energiesTable);
		GUIUtil.cellValueFactories(energiesTable, "description", "ses", "start", "target", "ap", "valid", "cheaper");

		energiesSesColumn.setCellFactory(
				IntegerSpinnerTableCell.<EnergyEnhancement> forTableColumn(0, 0, 1, false,
						(final IntegerSpinnerTableCell<EnergyEnhancement> cell, final Boolean empty) -> {
							if (empty) return new Tuple<>(0, 0);
							return new Tuple<>(0, 99);
						}));
		energiesSesColumn.setOnEditCommit(t -> {
			if (t.getRowValue() != null) {
				t.getRowValue().setSes(t.getNewValue(), hero);
			}
		});

		energiesTargetColumn
				.setCellFactory(IntegerSpinnerTableCell.forTableColumn(0, 99, 1, false,
						(final IntegerSpinnerTableCell<EnergyEnhancement> cell, final Boolean empty) -> {
							if (empty) return new Tuple<>(0, 0);
							return new Tuple<>(cell.getTableView().getItems().get(cell.getIndex()).getStart() + 1, 99);
						}));
		energiesTargetColumn.setOnEditCommit(t -> {
			if (t.getRowValue() != null) {
				t.getRowValue().setTarget(t.getNewValue(), hero);
			}
			energiesTable.refresh();
		});

		energiesTable.setRowFactory(t -> {
			final TableRow<EnergyEnhancement> row = new TableRow<>();

			final ContextMenu energiesContextMenu = new ContextMenu();

			final MenuItem energiesApplyItem = new MenuItem("Steigern");
			energiesContextMenu.getItems().add(energiesApplyItem);
			energiesApplyItem.setOnAction(o -> {
				final EnergyEnhancement item = row.getItem();
				alreadyEnhanced.add(item.getName());
				EnhancementController.instance.addEnhancement(item.clone(hero, EnhancementController.instance.getEnhancements()));
				update();
			});

			final MenuItem planItem = new MenuItem("Vormerken");
			energiesContextMenu.getItems().add(planItem);
			planItem.setOnAction(o -> {
				final EnergyEnhancement item = row.getItem();
				alreadyEnhanced.add(item.getName());
				final JSONArray planned = hero.getArr("Vorgemerkte Steigerungen");
				planned.add(item.clone(hero, EnhancementController.instance.getEnhancements()).toJSON(planned, true));
				planned.notifyListeners(null);
				update();
			});

			row.contextMenuProperty().bind(Bindings.when(row.itemProperty().isNotNull()).then(energiesContextMenu).otherwise((ContextMenu) null));

			return row;
		});

		energiesValidColumn.setCellFactory(tableColumn -> new TextFieldTableCell<>() {
			@Override
			public void updateItem(final Boolean valid, final boolean empty) {
				super.updateItem(valid, empty);
				@SuppressWarnings("all")
				final TableRow<EnergyEnhancement> row = getTableRow();
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

		energiesCheaperColumn.setCellFactory(tableColumn -> new TextFieldTableCell<>() {
			@Override
			public void updateItem(final Boolean cheaper, final boolean empty) {
				super.updateItem(cheaper, empty);
				@SuppressWarnings("all")
				final TableRow<EnergyEnhancement> row = getTableRow();
				final EnergyEnhancement item = row.getItem();
				row.getStyleClass().remove("valid");
				if (!empty && item != null && item.isValid() && cheaper) {
					row.getStyleClass().remove("invalid");
					row.getStyleClass().add("valid");
				}
			}
		});
	}

	@Override
	public void recalculate(final JSONObject hero) {
		for (final AttributeEnhancement enhancement : attributesTable.getItems()) {
			enhancement.reset(hero);
		}
		for (final EnergyEnhancement enhancement : energiesTable.getItems()) {
			enhancement.reset(hero);
		}
	}

	@Override
	public void recalculateValid(final JSONObject hero) {
		update();
	}

	@Override
	protected void registerListeners() {
		hero.getObj("Eigenschaften").addListener(heroListener);
		hero.getObj("Basiswerte").addListener(heroListener);
		hero.getObj("Vorteile").addListener(heroListener);
	}

	@Override
	public boolean removeEnhancement(final Enhancement enhancement) {
		if (enhancement instanceof final AttributeEnhancement ae) {
			attributesTable.getItems().add(ae);
			alreadyEnhanced.remove(ae.getName());
			return true;
		} else if (enhancement instanceof final EnergyEnhancement ee) {
			energiesTable.getItems().add(ee);
			alreadyEnhanced.remove(ee.getName());
			return true;
		} else
			return false;
	}

	@Override
	public void setHero(final JSONObject hero) {
		alreadyEnhanced.clear();
		super.setHero(hero);
	}

	@Override
	protected void unregisterListeners() {
		hero.getObj("Eigenschaften").removeListener(heroListener);
		hero.getObj("Basiswerte").removeListener(heroListener);
		hero.getObj("Vorteile").removeListener(heroListener);
		attributesTable.getItems().clear();
		energiesTable.getItems().clear();
	}

	@Override
	public void update() {
		attributesTable.getItems().clear();
		energiesTable.getItems().clear();

		final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
		final JSONObject actualAttributes = hero.getObj("Eigenschaften");
		final JSONObject derivedValues = ResourceManager.getResource("data/Basiswerte");

		for (final String attribute : attributes.keySet()) {
			if (!alreadyEnhanced.contains(attribute)) {
				attributesTable.getItems().add(new AttributeEnhancement(new Attribute(attribute, actualAttributes.getObj(attribute)), hero));
			}
		}

		for (final String derivedValue : new String[] { "Lebensenergie", "Ausdauer", "Magieresistenz", "Astralenergie" }) {
			if ("Astralenergie".equals(derivedValue) && !HeroUtil.isMagical(hero)) {
				continue;
			}
			if (!alreadyEnhanced.contains(derivedValue)) {
				energiesTable.getItems().add(new EnergyEnhancement(new Energy(derivedValue, derivedValues.getObj(derivedValue), hero), hero));
			}
		}
	}
}
