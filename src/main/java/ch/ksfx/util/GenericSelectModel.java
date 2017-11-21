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

import org.apache.tapestry5.OptionGroupModel;
import org.apache.tapestry5.OptionModel;
import org.apache.tapestry5.ValueEncoder;
import org.apache.tapestry5.internal.OptionModelImpl;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.apache.tapestry5.ioc.services.PropertyAdapter;
import org.apache.tapestry5.util.AbstractSelectModel;

import java.util.ArrayList;
import java.util.List;


public class GenericSelectModel<T> extends AbstractSelectModel implements ValueEncoder<T>
{
    private PropertyAdapter labelFieldAdapter;
    private PropertyAdapter idFieldAdapter;
    private List<T> list;

    public GenericSelectModel(List<T> list, Class<T> clazz, String labelField, String idField, PropertyAccess access)
    {
        this.list = list;
        if (idField != null) {
            this.idFieldAdapter = access.getAdapter(clazz).getPropertyAdapter(idField);
        }
        if (labelField != null) {
            this.labelFieldAdapter = access.getAdapter(clazz).getPropertyAdapter(labelField);
        }
    }

    @Override
    public List<OptionGroupModel> getOptionGroups()
    {
        return null;
    }

    @Override
    public List<OptionModel> getOptions()
    {
        List<OptionModel> optionModelList = new ArrayList<OptionModel>();
        if (labelFieldAdapter == null) {
            for (T obj : list) {
                optionModelList.add(new OptionModelImpl(nvl(obj), obj));
            }
        } else {
            for (T obj : list) {
                optionModelList.add(new OptionModelImpl(nvl(labelFieldAdapter.get(obj)), obj));
            }
        }
        return optionModelList;
    }

    public List<T> getList()
    {
        return list;
    }

    @Override
    public String toClient(T obj)
    {
        if (idFieldAdapter == null) {
            return obj + "";
        }
        return idFieldAdapter.get(obj) + "";
    }

    @Override
    public T toValue(String string)
    {
        if (idFieldAdapter == null) {
            for (T obj : list) {
                if (nvl(obj).equals(string)) {
                    return obj;
                }
            }
        } else {
            for (T obj : list) {
                if (nvl(idFieldAdapter.get(obj)).equals(string)) {
                    return obj;
                }
            }
        }
        return null;
    }

    private String nvl(Object o)
    {
        if (o == null) {
            return "";
        }
        return o.toString();
    }
}
