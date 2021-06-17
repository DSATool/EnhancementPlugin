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
package enhancement;

import java.util.List;

import dsatool.gui.Main;
import dsatool.plugins.Plugin;
import dsatool.resources.Settings;
import dsatool.settings.BooleanSetting;
import dsatool.settings.StringChoiceSetting;
import enhancement.enhancements.EnhancementController;

/**
 * A plugin to use AP to enhance heroes
 *
 * @author Dominik Helm
 */
public class Enhancement extends Plugin {

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#getPluginName()
	 */
	@Override
	public String getPluginName() {
		return "Enhancement";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#initialize()
	 */
	@Override
	public void initialize() {
		Main.addDetachableToolComposite("Helden", "Steigern", 300, 200, () -> new EnhancementController().getRoot());
		Settings.addSetting(new StringChoiceSetting("Lernmethode", "Gegenseitiges Lehren",
				List.of("Lehrmeister", "Gegenseitiges Lehren", "Selbststudium"), "Steigerung", "Lernmethode"));
		Settings.addSetting(new BooleanSetting("Lehrmeisterkosten", true, "Steigerung", "Lehrmeisterkosten"));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#load()
	 */
	@Override
	public void load() {}
}
