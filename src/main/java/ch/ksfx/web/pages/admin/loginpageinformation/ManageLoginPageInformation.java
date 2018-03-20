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

package ch.ksfx.web.pages.admin.loginpageinformation;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import ch.ksfx.web.pages.Login;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

@Secured({"ROLE_ADMIN"})
public class ManageLoginPageInformation
{
    @Inject
    private GenericDataStoreDAO genericDataStoreDAO;

    @InjectComponent
    private Form loginPageInformationForm;

    private GenericDataStore genericDataStore;

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(Login.LOGIN_PAGE_INFORMATION_KEY);

        if (genericDataStore == null) {
            genericDataStore = new GenericDataStore(Login.LOGIN_PAGE_INFORMATION_KEY, "");
        }
    }

    public String getLoginPageInformation()
    {
        return genericDataStore.getDataValue();
    }

    public void setLoginPageInformation(String loginPageInformation)
    {
        genericDataStore.setDataValue(loginPageInformation);
    }

    public void onSuccess()
    {
        genericDataStoreDAO.saveOrUpdateGenericDataStore(genericDataStore);
    }
}
