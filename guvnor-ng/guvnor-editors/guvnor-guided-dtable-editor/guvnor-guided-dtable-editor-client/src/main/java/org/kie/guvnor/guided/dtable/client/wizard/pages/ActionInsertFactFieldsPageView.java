/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.guvnor.guided.dtable.client.wizard.pages;

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;
import org.kie.guvnor.guided.dtable.client.widget.DTCellValueWidgetFactory;
import org.kie.guvnor.guided.dtable.model.ActionInsertFactCol52;
import org.kie.guvnor.guided.dtable.model.ActionInsertFactFieldsPattern;
import org.kie.guvnor.guided.dtable.model.GuidedDecisionTable52;

/**
 * View and Presenter definitions for the ActionInsertFactsFields page
 */
public interface ActionInsertFactFieldsPageView
        extends
        IsWidget {

    interface Presenter {

        void addPattern( ActionInsertFactFieldsPattern pattern );

        void removePattern( ActionInsertFactFieldsPattern pattern );

        void selectPattern( ActionInsertFactFieldsPattern pattern );

        void stateChanged();

        GuidedDecisionTable52.TableFormat getTableFormat();

        boolean hasEnums( ActionInsertFactCol52 selectedAction );

        void assertDefaultValue( ActionInsertFactCol52 selectedAction );

    }

    /**
     * Set the Presenter for the View to callback to
     * @param presenter
     */
    void setPresenter( Presenter presenter );

    void setDTCellValueWidgetFactory( DTCellValueWidgetFactory factory );

    void setAvailableFactTypes( List<String> availableTypes );

    void setChosenPatterns( List<ActionInsertFactFieldsPattern> patterns );

    void setAvailableFields( List<AvailableField> fields );

    void setChosenFields( List<ActionInsertFactCol52> fields );

    void setArePatternBindingsUnique( boolean arePatternBindingsUnique );

    void setAreActionInsertFactFieldsDefined( boolean areActionInsertFactFieldsDefined );

}
