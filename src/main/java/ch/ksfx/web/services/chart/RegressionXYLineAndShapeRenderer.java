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

package ch.ksfx.web.services.chart;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * Created by Kejo on 20.10.2015.
 */
public class RegressionXYLineAndShapeRenderer extends XYLineAndShapeRenderer
{
    public RegressionXYLineAndShapeRenderer()
    {
        super();
    }
    @Override
    protected boolean isLinePass(int pass) {
        return pass == 1;
    }
    /**
     * Returns <code>true</code> if the specified pass is the one for drawing
     * items.
     *
     * @param pass  the pass.
     *
     * @return A boolean.
     */
    @Override
    protected boolean isItemPass(int pass) {
        return pass == 0;
    }
}
