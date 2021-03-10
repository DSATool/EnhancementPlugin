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
package enhancement.history;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.ui.IntegerSpinnerTableCell;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple3;
import enhancement.enhancements.Enhancement;
import enhancement.history.AdventureEnhancement.Type;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class AdventureDialog {
	@FXML
	private VBox root;
	@FXML
	private TextField name;
	@FXML
	private TextField notes;
	@FXML
	private DatePicker date;
	@FXML
	private ReactiveSpinner<Integer> ap;
	@FXML
	private ReactiveSpinner<Double> money;
	@FXML
	private TableView<Tuple3<String, Integer, Type>> sesTable;
	@FXML
	private TableColumn<Tuple3<String, Integer, Type>, Integer> seAmountColumn;
	@FXML
	private ComboBox<String> attributeList;
	@FXML
	private ComboBox<String> energyList;
	@FXML
	private ComboBox<String> talentList;
	@FXML
	private ComboBox<String> spellList;
	@FXML
	private ComboBox<String> representationsList;
	@FXML
	private Button addSpellButton;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;

	private final AdventureEnhancement enhancement;
	private final Set<String> chosen = new HashSet<>();

	private final Stage stage;

	public AdventureDialog(final Window window, final JSONObject hero) {
		this(window, hero, new AdventureEnhancement());

		if (!HeroUtil.isMagical(hero)) {
			removeList(spellList);
		}

		final String[] lastAdventureName = { "" };
		DSAUtil.foreach(e -> "Abenteuer".equals(e.getString("Typ")), (final JSONObject e) -> lastAdventureName[0] = e.getStringOrDefault("Name", ""),
				hero.getArr("Historie"));

		name.setText(lastAdventureName[0]);
		date.setValue(LocalDate.now());
		enhancement.dateProperty().bind(Bindings.createStringBinding(() -> date.getValue().format(Enhancement.DateFormatter), date.valueProperty()));

		okButton.setOnAction(event -> {
			final JSONArray history = hero.getArr("Historie");
			history.add(enhancement.toJSON(hero).clone(history));
			history.notifyListeners(null);
			enhancement.apply(hero);
			stage.close();
		});

		sesTable.setRowFactory(table -> {
			final TableRow<Tuple3<String, Integer, Type>> row = new TableRow<>();
			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem deleteItem = new MenuItem("Entfernen");
			deleteItem.setOnAction(event -> {
				final Tuple3<String, Integer, Type> item = row.getItem();
				final String name = item._1;
				chosen.remove(name);
				sesTable.getItems().remove(item);
				switch (item._3) {
					case ATTRIBUTE -> {
						initAttributeList();
						attributeList.getSelectionModel().select(name);
					}
					case ENERGY -> {
						initEnergyList(hero);
						energyList.getSelectionModel().select(name);
					}
					case TALENT -> {
						initTalentList();
						talentList.getSelectionModel().select(name);
					}
					case SPELL -> {
						final int repIndex = name.lastIndexOf('(');
						final String spell = name.substring(0, repIndex - 1);
						if (spell.equals(spellList.getSelectionModel().getSelectedItem())) {
							final String rep = name.substring(repIndex + 1, name.length() - 1);
							representationsList.getItems().add(rep);
							representationsList.getItems().sort(null);
							representationsList.getSelectionModel().select(rep);
						}
					}
				}
			});
			contextMenu.getItems().add(deleteItem);
			row.contextMenuProperty().bind(Bindings.when(row.itemProperty().isNotNull()).then(contextMenu).otherwise((ContextMenu) null));
			return row;
		});
	}

	private AdventureDialog(final Window window, final JSONObject hero, final AdventureEnhancement enhancement) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("AdventureDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		this.enhancement = enhancement;

		stage = new Stage();
		stage.setTitle("Abenteuerabschluss");
		stage.setScene(new Scene(root, 290, 420));
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setResizable(false);
		stage.initOwner(window);

		cancelButton.setOnAction(event -> stage.close());

		okButton.setDefaultButton(true);
		okButton.disableProperty().bind(name.textProperty().isEmpty());
		cancelButton.setCancelButton(true);

		sesTable.setItems(enhancement.getSes());
		GUIUtil.autosizeTable(sesTable);
		GUIUtil.cellValueFactories(sesTable, "_1", "_2");

		seAmountColumn.setCellFactory(c -> new IntegerSpinnerTableCell<>(0, 9));
		seAmountColumn.setOnEditCommit(t -> {
			final Tuple3<String, Integer, Type> oldV = t.getRowValue();
			sesTable.getItems().set(t.getTablePosition().getRow(), new Tuple3<>(oldV._1, t.getNewValue(), oldV._3));
		});

		addSpellButton.disableProperty().bind(Bindings.createBooleanBinding(() -> representationsList.getItems().isEmpty(), representationsList.getItems()));

		name.setText(enhancement.getName());
		enhancement.descriptionProperty().bind(name.textProperty());

		ap.getValueFactory().setValue(enhancement.getAP());
		enhancement.apProperty().bind(ap.valueProperty());

		money.getValueFactory().setValue(enhancement.getCost());
		enhancement.costProperty().bind(money.valueProperty());

		notes.setText(enhancement.getNotes());
		enhancement.notesProperty().bind(notes.textProperty());

		initLists(hero);

		stage.show();
	}

	public AdventureDialog(final Window window, final JSONObject hero, final JSONObject enhancement) {
		this(window, hero, AdventureEnhancement.fromJSON(enhancement));

		ap.setDisable(true);
		sesTable.setEditable(false);
		removeList(attributeList);
		removeList(energyList);
		removeList(talentList);
		removeList(spellList);

		date.setValue(LocalDate.parse(this.enhancement.getDate(), Enhancement.DateFormatter));
		this.enhancement.dateProperty().bind(Bindings.createStringBinding(() -> date.getValue().format(Enhancement.DateFormatter), date.valueProperty()));

		okButton.setOnAction(event -> {
			final JSONArray history = hero.getArr("Historie");
			history.set(history.indexOf(enhancement), this.enhancement.toJSON(hero));
			history.notifyListeners(null);
			HeroUtil.addMoney(hero, (int) ((money.getValue() - enhancement.getDouble("Silber")) * 100));
			stage.close();
		});
	}

	@FXML
	private void addSE(final ActionEvent event) {
		@SuppressWarnings("unchecked")
		final ComboBox<String> list = (ComboBox<String>) ((Button) event.getSource()).getParent().getChildrenUnmodifiable().get(0);

		String item = list.getSelectionModel().getSelectedItem();

		Type tpe;
		if (list == attributeList) {
			tpe = Type.ATTRIBUTE;
			item = getAttributeAbbreviation(item);
		} else if (list == energyList) {
			tpe = Type.ENERGY;
		} else if (list == talentList) {
			tpe = Type.TALENT;
		} else {
			tpe = Type.SPELL;
		}
		final SingleSelectionModel<String> representationSelection = representationsList.getSelectionModel();
		final String fullName = tpe == Type.SPELL ? item + " (" + representationSelection.getSelectedItem() + ")" : item;
		chosen.add(fullName);
		sesTable.getItems().add(new Tuple3<>(fullName, 1, tpe));
		if (tpe == Type.SPELL) {
			representationsList.getItems().remove(representationSelection.getSelectedItem());
			representationSelection.select(representationSelection.getSelectedIndex() + 1);
		} else {
			list.getItems().remove(item);
			updateList(list);
		}
	}

	private String getAttributeAbbreviation(final String attribute) {
		final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
		for (final String abbreviation : attributes.keySet()) {
			if (attribute.equals(attributes.getObj(abbreviation).getString("Name"))) return abbreviation;
		}
		throw new IllegalArgumentException();
	}

	private void initAttributeList() {
		final ObservableList<String> items = attributeList.getItems();
		items.clear();
		items.addAll(HeroUtil.getChoices(null, "Eigenschaft", null));
		items.removeAll(chosen);
		updateList(attributeList);
	}

	private void initEnergyList(final JSONObject hero) {
		final ObservableList<String> items = energyList.getItems();
		items.clear();
		items.add("Lebensenergie");
		items.add("Ausdauer");
		items.add("Magieresistenz");
		if (HeroUtil.isMagical(hero)) {
			items.add("Astralenergie");
		}
		items.removeAll(chosen);
		updateList(energyList);
	}

	private void initLists(final JSONObject hero) {
		initAttributeList();
		initEnergyList(hero);
		initTalentList();

		final JSONObject spells = ResourceManager.getResource("data/Zauber");
		spellList.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
			if (newValue != null) {
				representationsList.getItems().clear();
				final JSONObject spell = spells.getObj(newValue);
				for (final String representation : spell.getObj("Repräsentationen").keySet()) {
					if (!chosen.contains(newValue + " (" + representation + ")")) {
						representationsList.getItems().add(representation);
					}
				}
				representationsList.getSelectionModel().select(0);
			}
		});

		spellList.getItems().addAll(HeroUtil.getChoices(null, "Zauber", null));
		spellList.getSelectionModel().select(0);
	}

	private void initTalentList() {
		final ObservableList<String> items = talentList.getItems();
		items.clear();
		items.addAll(HeroUtil.getChoices(null, "Talent", null));
		items.removeAll(chosen);
		updateList(talentList);
	}

	private void removeList(final ComboBox<String> list) {
		final Parent box = list.getParent();
		box.setVisible(false);
		box.setManaged(false);
	}

	private void updateList(final ComboBox<String> list) {
		final Button button = (Button) list.getParent().getChildrenUnmodifiable().get(1);
		final boolean isEmpty = list.getItems().isEmpty();
		button.setDisable(isEmpty);
		if (!isEmpty) {
			list.getSelectionModel().select(list.getSelectionModel().getSelectedIndex() + 1);
		}
	}
}
