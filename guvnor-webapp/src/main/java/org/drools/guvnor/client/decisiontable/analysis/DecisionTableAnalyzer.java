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

package org.drools.guvnor.client.decisiontable.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.ide.common.client.modeldriven.SuggestionCompletionEngine;
import org.drools.ide.common.client.modeldriven.dt52.Analysis;
import org.drools.ide.common.client.modeldriven.dt52.ConditionCol52;
import org.drools.ide.common.client.modeldriven.dt52.DTCellValue52;
import org.drools.ide.common.client.modeldriven.dt52.GuidedDecisionTable52;
import org.drools.ide.common.client.modeldriven.dt52.LimitedEntryCol;
import org.drools.ide.common.client.modeldriven.dt52.Pattern52;

public class DecisionTableAnalyzer {

    private SuggestionCompletionEngine sce;

    public DecisionTableAnalyzer(SuggestionCompletionEngine sce) {
        this.sce = sce;
    }

    public List<Analysis> analyze(GuidedDecisionTable52 modelWithWrongData, List<List<DTCellValue52>> data) {
        return detectImpossibleMatches(modelWithWrongData, data);
    }

    private List<Analysis> detectImpossibleMatches(GuidedDecisionTable52 modelWithWrongData, List<List<DTCellValue52>> data) {
        List<Analysis> analysisData = new ArrayList<Analysis>(data.size());
        List<RowDetector> rowDetectorList = new ArrayList<RowDetector>(data.size());
        for (List<DTCellValue52> row : data) {
            RowDetector rowDetector = new RowDetector(row.get(0).getNumericValue().longValue() - 1);
            for (Pattern52 pattern : modelWithWrongData.getConditionPatterns()) {
                List<ConditionCol52> conditions = pattern.getConditions();
                for (ConditionCol52 conditionCol : conditions) {
                    int columnIndex = modelWithWrongData.getAllColumns().indexOf(conditionCol);
                    DTCellValue52 value = row.get(columnIndex);
                    // Blank cells are ignored
                    if (value.hasValue()) {
                        FieldDetector fieldDetector = buildDetector(modelWithWrongData, conditionCol, value);
                        String factField = conditionCol.getFactField();
                        rowDetector.putOrMerge(pattern, factField, fieldDetector);
                    }
                }
            }
            rowDetectorList.add(rowDetector);
        }
        for (RowDetector rowDetector : rowDetectorList) {
            analysisData.add(rowDetector.buildAnalysis(rowDetectorList));
        }
        return analysisData;
    }

    private FieldDetector buildDetector(GuidedDecisionTable52 model, ConditionCol52 conditionCol,
            DTCellValue52 value) {
        FieldDetector newDetector;
        String operator = conditionCol.getOperator();
        if (conditionCol instanceof LimitedEntryCol) {
            newDetector = new BooleanFieldDetector(value.getBooleanValue(), operator);
        } else {
            // Extended Entry...
            String type = model.getType( conditionCol, sce );
            // Retrieve "Guvnor" enums
            String[] allValueList = model.getValueList( conditionCol, sce );
            if (allValueList.length != 0) {
                // Guvnor enum
                newDetector = new EnumFieldDetector(Arrays.asList(allValueList), value.getStringValue(), operator);
            } else if ( type == null ) {
                // type null means the field is free-format
                newDetector = new UnrecognizedFieldDetector(operator);
            } else if ( type.equals( SuggestionCompletionEngine.TYPE_STRING ) ) {
                newDetector = new StringFieldDetector(value.getStringValue(), operator);
            } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC ) ) {
                newDetector = new NumericFieldDetector(value.getNumericValue(), operator);
            } else if ( type.equals( SuggestionCompletionEngine.TYPE_BOOLEAN ) ) {
                newDetector = new BooleanFieldDetector(value.getBooleanValue(), operator);
            } else if ( type.equals( SuggestionCompletionEngine.TYPE_DATE ) ) {
                newDetector = new DateFieldDetector(value.getDateValue(), operator);
            } else {
                newDetector = new UnrecognizedFieldDetector(operator);
            }
        }
        return newDetector;
    }

}
