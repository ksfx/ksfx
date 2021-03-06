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

package ch.ksfx.dao.ebean.spidering;

import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.Resource;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.util.EbeanGridDataSource;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import io.ebean.SqlUpdate;
import org.apache.tapestry5.grid.GridDataSource;

import java.util.Date;
import java.util.List;


public class EbeanSpideringDAO implements SpideringDAO
{
    @Override
    public Spidering getSpideringForId(Long spideringId)
    {
        return Ebean.find(Spidering.class, spideringId);
    }

    @Override
    public void saveOrUpdate(Spidering spidering)
    {
        if (spidering.getId() != null) {
            Ebean.update(spidering);
        } else {
            Ebean.save(spidering);
        }
    }

    @Override
    public void delete(Spidering spidering)
    {
        //TODO on delete cascade result unit deletion.
        SqlUpdate update = Ebean.createSqlUpdate("DELETE FROM resource WHERE spidering = :spidering");
        update.setParameter("spidering", spidering.getId());
        update.execute();

        SqlUpdate update2 = Ebean.createSqlUpdate("UPDATE result SET spidering = NULL WHERE spidering = :spidering");
        update2.setParameter("spidering", spidering.getId());
        update2.execute();

        Ebean.delete(spidering);
    }

    @Override
    public List<Spidering> getAllSpiderings()
    {
        return Ebean.find(Spidering.class).findList();
    }

    @Override
    public List<Spidering> getSpideringsOlderThan(Date date)
    {
        return Ebean.find(Spidering.class).where().le("finished",date).findList();
    }
    
    @Override
    public List<Spidering> getSpideringsOlderThanForSpideringConfiguration(Date date, SpideringConfiguration spideringConfiguration)
    {
    	return Ebean.find(Spidering.class).where().le("finished",date).eq("spideringConfiguration.id", spideringConfiguration.getId()).findList();	
    }

    @Override
    public GridDataSource getSpideringGridDataSourceForSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        ExpressionList expressionList = Ebean.find(Spidering.class).where().eq("spideringConfiguration.id", spideringConfiguration.getId());

        return new EbeanGridDataSource(expressionList, Spidering.class);
    }

    @Override
    public List<Spidering> getSpideringsForSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        return Ebean.find(Spidering.class).where().eq("spideringConfiguration.id", spideringConfiguration.getId()).findList();
    }

    @Override
    public Integer calculateResourcesCount(Spidering spidering)
    {
        return Ebean.find(Resource.class).where().eq("spidering.id", spidering.getId()).findCount();
    }
}
