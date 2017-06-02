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
package enhancement.enhancements;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import dsa41basis.ui.hero.HeroController;
import dsa41basis.ui.hero.HeroSelector;
import dsatool.util.ErrorLogger;
import dsatool.util.IntegerSpinnerTableCell;
import enhancement.attributes.AttributesController;
import enhancement.pros_cons.QuirksController;
import enhancement.skills.SkillController;
import enhancement.talents.SpellsController;
import enhancement.talents.TalentController;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import jsonant.value.JSONObject;

public class EnhancementController extends HeroSelector {

	public static List<Class<? extends EnhancementTabController>> tabControllers = Arrays.asList(AttributesController.class, QuirksController.class,
			SkillController.class, TalentController.class, SpellsController.class);

	private static final DataFormat SERIALIZED = new DataFormat("application/x-java-serialized-object");

	public static BooleanProperty usesChargenRules = new SimpleBooleanProperty();

	private VBox pane;
	@FXML
	private TableView<Enhancement> enhancementTable;
	@FXML
	private TableColumn<Enhancement, String> descriptionColumn;
	@FXML
	private TableColumn<Enhancement, Integer> costColumn;
	@FXML
	private Label costLabel;
	@FXML
	private CheckBox chargenRules;

	private JSONObject hero;

	public EnhancementController() {
		super(false);

		final FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setController(this);

		try {
			pane = fxmlLoader.load(EnhancementController.class.getResource("Enhancement.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		setContent(pane);

		load();
	}

	public void addEnhancement(final Enhancement enhancement) {
		enhancementTable.getItems().add(enhancement);
	}

	@FXML
	private void apply() {
		for (final Enhancement enhancement : enhancementTable.getItems()) {
			if (!enhancement.isValid()) {
				final Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Voraussetzungen nicht erfüllt");
				alert.setHeaderText("Die Voraussetzungen für eine oder mehrere ausgewählte Steigerung sind nicht erfüllt.");
				alert.setContentText("Sollen die Steigerungen wirklich angewendet werden?");
				alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
				final Optional<ButtonType> result = alert.showAndWait();
				if (!result.isPresent() || !result.get().equals(ButtonType.OK)) return;
				break;
			}
		}

		final JSONObject bio = hero.getObj("Biografie");
		final Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Steigerungen anwenden");
		alert.setHeaderText(
				"Die ausgewählten Steigerungen kosten " + calculateCost() + " AP (" + bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0) + " AP verfügbar).");
		alert.setContentText("Sollen die Steigerungen wirklich angewendet werden?");
		alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
		alert.showAndWait().filter(response -> response.equals(ButtonType.OK)).ifPresent(response -> {
			for (final Enhancement enhancement : enhancementTable.getItems()) {
				enhancement.apply(hero);
			}
			bio.put("Abenteuerpunkte-Guthaben", bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0) - calculateCost());
			bio.notifyListeners(null);
			enhancementTable.getItems().clear();
			for (final HeroController controller : controllers) {
				((EnhancementTabController) controller).update();
			}
		});
	}

	private boolean applyChargenRules(final JSONObject hero) {
		final JSONObject bio = hero.getObj("Biografie");
		final JSONObject attributes = hero.getObj("Eigenschaften");
		final JSONObject educated = hero.getObj("Vorteile").getObjOrDefault("Gebildet", null);
		final JSONObject uneducated = hero.getObj("Nachteile").getObjOrDefault("Ungebildet", null);
		int expectedAP = (attributes.getObj("KL").getIntOrDefault("Start", 0) + attributes.getObj("IN").getIntOrDefault("Start", 0)) * 20;
		if (educated != null) {
			expectedAP += 40 * educated.getIntOrDefault("Stufe", 0);
		}
		if (uneducated != null) {
			expectedAP -= 40 * uneducated.getIntOrDefault("Stufe", 0);
		}
		return bio.getIntOrDefault("Abenteuerpunkte", 0) == expectedAP;
	}

	private int calculateCost() {
		int cost = 0;
		for (final Enhancement enhancement : enhancementTable.getItems()) {
			cost += enhancement.getCost();
		}
		return cost;
	}

	@FXML
	private void clear() {
		final Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Zurücksetzen");
		alert.setHeaderText("Dies wird die ausgewählten Steigerungen löschen.");
		alert.setContentText("Sollen die Steigerungen wirklich zurückgesetzt werden?");
		alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
			enhancementTable.getItems().clear();
			for (final HeroController controller : controllers) {
				((EnhancementTabController) controller).update();
			}
		});
	}

	public Collection<Enhancement> getEnhancements() {
		return enhancementTable.getItems();
	}

	@Override
	public void load() {
		final TabPane tabs = new TabPane();
		pane.getChildren().add(0, tabs);
		VBox.setVgrow(tabs, Priority.ALWAYS);

		try {
			for (final Class<? extends EnhancementTabController> controller : tabControllers) {
				controllers.add(controller.getDeclaredConstructor(TabPane.class, EnhancementController.class).newInstance(tabs, this));
			}
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		tabs.prefHeightProperty().bind(pane.heightProperty().divide(2));
		enhancementTable.prefHeightProperty().bind(pane.heightProperty().divide(2).subtract(40));

		descriptionColumn.getStyleClass().add("left-aligned");

		DoubleBinding width = enhancementTable.widthProperty().subtract(2);
		width = width.subtract(costColumn.widthProperty());
		descriptionColumn.prefWidthProperty().bind(width);

		descriptionColumn.setCellValueFactory(new PropertyValueFactory<Enhancement, String>("fullDescription"));

		costColumn.setCellValueFactory(new PropertyValueFactory<Enhancement, Integer>("cost"));
		costColumn.setCellFactory(o -> new IntegerSpinnerTableCell<>(0, 9999, 1, false));
		costColumn.setOnEditCommit(t -> {
			t.getTableView().getItems().get(t.getTablePosition().getRow()).setCost(t.getNewValue());
			recalculateCost();
		});

		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem resetItem = new MenuItem("Zurücksetzen");
		contextMenu.getItems().add(resetItem);
		resetItem.setOnAction(o -> {
			enhancementTable.getSelectionModel().getSelectedItem().resetCost(hero);
			recalculateCost();
		});
		final MenuItem removeItem = new MenuItem("Entfernen");
		contextMenu.getItems().add(removeItem);
		removeItem.setOnAction(o -> {
			enhancementTable.getItems().remove(enhancementTable.getSelectionModel().getSelectedItem());
			for (final HeroController controller : controllers) {
				((EnhancementTabController) controller).update();
			}
		});
		contextMenu.setOnShowing(e -> {
			final Enhancement enhancement = enhancementTable.getSelectionModel().getSelectedItem();
			resetItem.setVisible(enhancement != null);
			removeItem.setVisible(enhancement != null);
		});
		enhancementTable.setContextMenu(contextMenu);

		enhancementTable.setRowFactory(tableView -> {
			final TableRow<Enhancement> row = new TableRow<Enhancement>() {
				@Override
				protected void updateItem(final Enhancement enhancement, final boolean empty) {
					super.updateItem(enhancement, empty);
					getStyleClass().remove("invalid");
					if (!empty && !enhancement.isValid()) {
						getStyleClass().add("invalid");
					}
				}
			};

			row.setOnDragDetected(event -> {
				if (!row.isEmpty()) {
					final Integer index = row.getIndex();
					final Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
					db.setDragView(row.snapshot(null, null));
					final ClipboardContent cc = new ClipboardContent();
					cc.put(SERIALIZED, index);
					db.setContent(cc);
					event.consume();
				}
			});

			row.setOnDragOver(event -> {
				final Dragboard db = event.getDragboard();
				if (db.hasContent(SERIALIZED)) {
					if (row.getIndex() != ((Integer) db.getContent(SERIALIZED)).intValue()) {
						event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
						event.consume();
					}
				}
			});

			row.setOnDragDropped(event -> {
				final Dragboard db = event.getDragboard();
				if (db.hasContent(SERIALIZED)) {
					final int draggedIndex = (Integer) db.getContent(SERIALIZED);
					final Enhancement draggedItem = enhancementTable.getItems().remove(draggedIndex);

					int dropIndex;

					if (row.isEmpty()) {
						dropIndex = tableView.getItems().size();
					} else {
						dropIndex = row.getIndex();
					}

					tableView.getItems().add(dropIndex, draggedItem);

					event.setDropCompleted(true);
					tableView.getSelectionModel().select(dropIndex);
					event.consume();
				}
			});

			return row;
		});

		costLabel.setText("0");
		enhancementTable.getItems().addListener((final Change<? extends Enhancement> c) -> {
			recalculateValid();
			recalculateCost();
		});

		usesChargenRules.bindBidirectional(chargenRules.selectedProperty());

		super.load();
	}

	private void recalculateCost() {
		final Stack<Enhancement> enhancements = new Stack<>();
		for (final Enhancement e : enhancementTable.getItems()) {
			e.recalculateCost(hero);
			e.applyTemporarily(hero);
			enhancements.push(e);
		}
		for (final HeroController controller : controllers) {
			((EnhancementTabController) controller).recalculateCost(hero);
		}
		for (final Enhancement e : enhancements) {
			e.unapply(hero);
		}
		costLabel.setText(String.valueOf(calculateCost()));
	}

	private void recalculateValid() {
		final Stack<Enhancement> enhancements = new Stack<>();
		for (final Enhancement e : enhancementTable.getItems()) {
			e.recalculateValid(hero);
			e.applyTemporarily(hero);
			enhancements.push(e);
		}
		for (final HeroController controller : controllers) {
			((EnhancementTabController) controller).recalculateValid(hero);
		}
		for (final Enhancement e : enhancements) {
			e.unapply(hero);
		}
	}

	@Override
	protected void setHero(final int index) {
		enhancementTable.getItems().clear();
		hero = heroes.get(index);
		chargenRules.setSelected(applyChargenRules(hero));
		super.setHero(index);
	}

}
