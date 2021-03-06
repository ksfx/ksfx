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

package ch.ksfx.web.pages;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Value;
import org.apache.tapestry5.services.Request;


public class Login
{
    @Inject
    private GenericDataStoreDAO genericDataStoreDAO;

    private boolean failed = false;

    public static final String LOGIN_PAGE_INFORMATION_KEY = "LOGIN_PAGE_INFORMATION";

    public Object onActivate()
    {
        return null;
    }

    public boolean isFailed()
    {
        return failed;
    }

    void onActivate(String extra)
    {
        if (extra.equals("failed")) {
            failed = true;
        }
    }

    public String getLoginPageInformation()
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(LOGIN_PAGE_INFORMATION_KEY);

        if (genericDataStore == null) {
            return null;
        }

        return genericDataStore.getDataValue();
    }
}
