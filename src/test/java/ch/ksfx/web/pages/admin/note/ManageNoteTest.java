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

package ch.ksfx.web.pages.admin.note;

import ch.ksfx.web.PageTesterFactory;
import org.apache.tapestry5.dom.Document;
import org.testng.Assert;
import org.testng.annotations.Test;


public class ManageNoteTest
{
    @Test
    public void test1() throws Exception
    {
        Document doc = PageTesterFactory.getInstance().renderPage("admin/note/managenote");

        //Just a test if the page could be rendered...
        Assert.assertTrue(doc.toString().contains("<h1>Manage Note</h1>"));
    }
}
