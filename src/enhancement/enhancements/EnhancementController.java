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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import dsa41basis.ui.hero.HeroController;
import dsa41basis.ui.hero.HeroSelector;
import dsa41basis.util.HeroUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.Settings;
import dsatool.ui.DoubleSpinnerTableCell;
import dsatool.ui.IntegerSpinnerTableCell;
import dsatool.util.ErrorLogger;
import enhancement.attributes.AttributesController;
import enhancement.history.AdventureDialog;
import enhancement.history.HistoryController;
import enhancement.planned.PlannedController;
import enhancement.pros_cons.QuirksController;
import enhancement.skills.SkillController;
import enhancement.talents.SpellsController;
import enhancement.talents.TalentController;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import jsonant.event.JSONListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class EnhancementController extends HeroSelector {

	public static List<Class<? extends EnhancementTabController>> tabControllers = new ArrayList<>(List.of(AttributesController.class, QuirksController.class,
			SkillController.class, TalentController.class, SpellsController.class, PlannedController.class, HistoryController.class));

	public static BooleanProperty usesChargenRules = new SimpleBooleanProperty();

	public static EnhancementController instance;

	private VBox pane;
	@FXML
	private TabPane tabPane;
	@FXML
	private TableView<Enhancement> enhancementTable;
	@FXML
	private TableColumn<Enhancement, String> descriptionColumn;
	@FXML
	private TableColumn<Enhancement, Double> costColumn;
	@FXML
	private TableColumn<Enhancement, Integer> apColumn;
	@FXML
	private Label apLabel;
	@FXML
	private Label availableApLabel;
	@FXML
	private HBox costBox;
	@FXML
	private Label costLabel;
	@FXML
	private Label availableMoneyLabel;
	@FXML
	private CheckBox chargenRules;

	private JSONObject hero;

	private final JSONListener apListener = o -> availableApLabel
			.setText(Integer.toString(hero.getObj("Biografie").getIntOrDefault("Abenteuerpunkte-Guthaben", 0)) + " AP");

	private final JSONListener moneyListener = o -> {
		final JSONObject money = hero.getObj("Besitz").getObj("Geld");
		int availableMoney = 0;
		for (final String unit : new String[] { "Dukaten", "Silbertaler", "Heller", "Kreuzer" }) {
			availableMoney *= 10;
			availableMoney += money.getIntOrDefault(unit, 0);
		}

		availableMoneyLabel.setText(Double.toString(availableMoney / 100.0) + " Silber");
	};

	public EnhancementController() {
		super(false);

		final FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setController(this);

		try {
			pane = fxmlLoader.load(EnhancementController.class.getResource("Enhancement.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		pane.getStylesheets().add(getClass().getResource("enhancement.css").toExternalForm());

		instance = this;

		setContent(pane);

		load();
	}

	@FXML
	private void addAdventure() {
		new AdventureDialog(pane.getScene().getWindow(), hero);
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

		final int ap = calculateAP();
		final double cost = calculateCost();

		final JSONObject bio = hero.getObj("Biografie");

		String text = "Die ausgewählten Steigerungen kosten " + ap + " AP (" + bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0) + " AP verfügbar).";
		if (Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten") && cost != 0) {
			text += "\nEs werden " + cost + " Silber an Lehrmeisterkosten fällig (" + availableMoneyLabel.getText() + " verfügbar).";
		}

		final Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Steigerungen anwenden");
		alert.setHeaderText(text);
		alert.setContentText("Sollen die Steigerungen wirklich angewendet werden?");
		alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
		alert.showAndWait().filter(response -> response.equals(ButtonType.OK)).ifPresent(response -> {
			final int index = Math.max(list.getSelectionModel().getSelectedIndex(), 0);
			final JSONArray history = hero.getArr("Historie");
			bio.put("Abenteuerpunkte-Guthaben", bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0) - ap);
			if (Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten") && cost != 0) {
				HeroUtil.addMoney(hero, (int) cost * -100);
			}
			final ArrayList<Enhancement> enhancements = new ArrayList<>(enhancementTable.getItems());
			enhancementTable.getItems().clear();
			for (final Enhancement enhancement : enhancements) {
				history.add(enhancement.toJSON(history, false));
				enhancement.apply(hero);
			}
			bio.notifyListeners(null);
			history.notifyListeners(null);
			setHero(index);
		});
	}

	private boolean applyChargenRules(final JSONObject hero) {
		final JSONObject bio = hero.getObj("Biografie");
		return bio.getIntOrDefault("Abenteuerpunkte", 0).equals(bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0));
	}

	private int calculateAP() {
		int ap = 0;
		for (final Enhancement enhancement : enhancementTable.getItems()) {
			ap += enhancement.getAP();
		}
		return ap;
	}

	private double calculateCost() {
		int cost = 0;
		for (final Enhancement enhancement : enhancementTable.getItems()) {
			cost += enhancement.getCost() * 100;
		}
		return cost / 100.0;
	}

	@FXML
	private void clear() {
		final Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Zurücksetzen");
		alert.setHeaderText("Dies wird die ausgewählten Steigerungen löschen.");
		alert.setContentText("Sollen die Steigerungen wirklich zurückgesetzt werden?");
		alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
			setHero(Math.max(list.getSelectionModel().getSelectedIndex(), 0));
		});
	}

	public List<HeroController> getControllers() {
		return Collections.unmodifiableList(controllers);
	}

	public Collection<Enhancement> getEnhancements() {
		return enhancementTable.getItems();
	}

	@Override
	public void load() {
		try {
			for (final Class<? extends EnhancementTabController> controller : tabControllers) {
				controllers.add(controller.getDeclaredConstructor(EnhancementController.class, TabPane.class).newInstance(this, tabPane));
			}
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		tabPane.prefHeightProperty().bind(pane.heightProperty().divide(2));
		enhancementTable.prefHeightProperty().bind(pane.heightProperty().divide(2).subtract(40));

		if (!Settings.getSettingBoolOrDefault(true, "Steigerung", "Lehrmeisterkosten")) {
			costColumn.setMinWidth(0);
			costColumn.setPrefWidth(0);
			costColumn.setMaxWidth(0);
			costBox.setVisible(false);
			costBox.setManaged(false);
		}

		DoubleBinding width = enhancementTable.widthProperty().subtract(2);
		width = width.subtract(costColumn.widthProperty());
		width = width.subtract(apColumn.widthProperty());
		descriptionColumn.prefWidthProperty().bind(width);

		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("fullDescription"));

		costColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
		costColumn.setCellFactory(o -> new DoubleSpinnerTableCell<>(0, 9999, 0.1, false));
		costColumn.setOnEditCommit(t -> {
			if (t.getRowValue() != null) {
				t.getRowValue().setCost(t.getNewValue());
				recalculate(false);
			}
		});

		apColumn.setCellValueFactory(new PropertyValueFactory<>("ap"));
		apColumn.setCellFactory(o -> new IntegerSpinnerTableCell<>(0, 9999));
		apColumn.setOnEditCommit(t -> {
			if (t.getRowValue() != null) {
				t.getRowValue().setAP(t.getNewValue(), hero);
				recalculate(false);
			}
		});

		final boolean[] reordering = { false };

		enhancementTable.setRowFactory(tableView -> {
			final TableRow<Enhancement> row = new TableRow<>() {
				@Override
				protected void updateItem(final Enhancement enhancement, final boolean empty) {
					super.updateItem(enhancement, empty);
					getStyleClass().remove("invalid");
					setTooltip(null);
					if (!empty && !enhancement.isValid()) {
						getStyleClass().add("invalid");
						final Tooltip tooltip = new Tooltip();
						tooltip.setOnShowing(o -> {
							tooltip.setText(getItem().getInvalidReason(hero));
						});
						setTooltip(tooltip);
					}
				}
			};

			GUIUtil.dragDropReorder(row, () -> reordering[0] = true, () -> {}, moved -> {
				reordering[0] = false;
				if (moved.length > 0) {
					recalculate(true);
				}
			}, tableView);

			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem resetItem = new MenuItem("Zurücksetzen");
			contextMenu.getItems().add(resetItem);
			resetItem.setOnAction(o -> {
				row.getItem().reset(hero);
				recalculate(false);
			});

			final MenuItem planItem = new MenuItem("Vormerken");
			contextMenu.getItems().add(planItem);
			planItem.setOnAction(o -> {
				final Enhancement item = row.getItem();
				final JSONArray planned = hero.getArr("Vorgemerkte Steigerungen");
				planned.add(item.toJSON(planned, true));
				planned.notifyListeners(null);
				enhancementTable.getItems().remove(item);
			});

			final MenuItem removeItem = new MenuItem("Entfernen");
			contextMenu.getItems().add(removeItem);
			removeItem.setOnAction(o -> {
				final Enhancement removed = row.getItem();
				for (final HeroController controller : controllers) {
					if (((EnhancementTabController) controller).removeEnhancement(removed)) {
						break;
					}
				}
				enhancementTable.getItems().remove(removed);
			});

			row.contextMenuProperty().bind(Bindings.when(row.itemProperty().isNotNull()).then(contextMenu).otherwise((ContextMenu) null));

			return row;
		});

		apLabel.setText("0");
		costLabel.setText("0");

		enhancementTable.getItems().addListener((final Change<? extends Enhancement> c) -> {
			if (!reordering[0]) {
				recalculate(true);
			}
		});

		super.load();

		final EnhancementTabController firstPage = (EnhancementTabController) controllers.get(0);
		firstPage.init();
		firstPage.update();
		tabPane.getTabs().get(0).setContent(firstPage.getControl());

		usesChargenRules.bindBidirectional(chargenRules.selectedProperty());
		usesChargenRules.addListener((o, oldV, newV) -> recalculate(false));
	}

	private void recalculate(final boolean recalculateValid) {
		final Stack<Enhancement> enhancements = new Stack<>();
		for (final Enhancement e : enhancementTable.getItems()) {
			e.recalculateCosts(hero);
			e.applyTemporarily(hero);
			enhancements.push(e);
		}

		for (final HeroController controller : controllers) {
			final EnhancementTabController current = (EnhancementTabController) controller;
			if (current.tab.isSelected()) {
				if (recalculateValid) {
					current.recalculateValid(hero);
				}
				current.recalculate(hero);
			}
		}
		for (final Enhancement e : enhancements) {
			e.unapplyTemporary(hero);
		}
		apLabel.setText(String.valueOf(calculateAP()));
		costLabel.setText(String.valueOf(calculateCost()));
	}

	@Override
	protected void setHero(final int index) {
		enhancementTable.getItems().clear();
		if (hero != null) {
			hero.getObj("Biografie").removeListener(apListener);
			hero.getObj("Besitz").getObj("Geld").removeListener(moneyListener);
		}

		hero = heroes.get(index);
		chargenRules.setSelected(applyChargenRules(hero));

		hero.getObj("Biografie").addListener(apListener);
		apListener.notifyChanged(null);

		hero.getObj("Besitz").getObj("Geld").addListener(moneyListener);
		moneyListener.notifyChanged(null);

		super.setHero(index);
	}

}
