/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ksfx.util;

import io.ebean.ExpressionList;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;
import java.util.List;


public class EbeanGridDataSource implements GridDataSource
{
    private ExpressionList expressionList;
    private List preparedResults;
    private int startIndex;
    private Class clazz;

    public EbeanGridDataSource(ExpressionList expressionList, Class clazz)
    {
        this.expressionList = expressionList;
        this.clazz = clazz;
    }

    @Override
    public int getAvailableRows()
    {
        return expressionList.findCount();
    }

    @Override
    public void prepare(int startIndex, int endIndex, List<SortConstraint> sortConstraints)
    {
        expressionList.setFirstRow(startIndex).setMaxRows(endIndex - startIndex + 1);

        for (SortConstraint constraint : sortConstraints)
        {

            String propertyName = constraint.getPropertyModel().getPropertyName();

            switch (constraint.getColumnSort())
            {

                case ASCENDING:

                    expressionList.order().asc(propertyName);
                    break;

                case DESCENDING:
                    expressionList.order().desc(propertyName);
                    break;

                default:
            }
        }

        this.startIndex = startIndex;
        preparedResults = expressionList.findList();
    }

    @Override
    public Object getRowValue(int index)
    {
        return preparedResults.get(index - startIndex);
    }

    @Override
    public Class getRowType()
    {
        return clazz;
    }
}
