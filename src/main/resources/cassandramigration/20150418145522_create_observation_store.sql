DROP KEYSPACE IF EXISTS observation_store;
CREATE KEYSPACE observation_store WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

use observation_store;

CREATE TABLE observation (
time_series_id int,
observation_time timestamp,
source_id text,
scalar_value text,
complex_value map<text, text>,
meta_data map<text, text>,
PRIMARY KEY (time_series_id,observation_time,source_id)
) WITH CLUSTERING ORDER BY (observation_time DESC);

CREATE INDEX source_id_index ON observation(source_id);