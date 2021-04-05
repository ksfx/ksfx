INSERT INTO `url_fragment_finder` (`id`,`name`) VALUES (1,'RegEx');
INSERT INTO `url_fragment_finder` (`id`,`name`) VALUES (2,'XPath');
INSERT INTO `url_fragment_finder` (`id`,`name`) VALUES (3,'String');

INSERT INTO `time_series_type` (`id`,`name`,`observation_view`) VALUES (1,'Standard Double Series','viewTimeSeriesDouble');
INSERT INTO `time_series_type` (`id`,`name`,`observation_view`) VALUES (2,'Standard News Series','viewTimeSeriesNews');
INSERT INTO `time_series_type` (`id`,`name`,`observation_view`) VALUES (3,'Generic View with Detail Modal','viewTimeSeriesGenericWithDetail');

INSERT INTO `result_unit_type` (`id`,`name`) VALUES (1,'Title');
INSERT INTO `result_unit_type` (`id`,`name`) VALUES (2,'Main Text');
INSERT INTO `result_unit_type` (`id`,`name`) VALUES (3,'Author');
INSERT INTO `result_unit_type` (`id`,`name`) VALUES (4,'Timestamp');
INSERT INTO `result_unit_type` (`id`,`name`) VALUES (5,'Site Identifier');
INSERT INTO `result_unit_type` (`id`,`name`) VALUES (6,'Downloaded Data');
INSERT INTO `result_unit_type` (`id`,`name`) VALUES (7,'Additional Metadata');
INSERT INTO `result_unit_type` (`id`,`name`) VALUES (8,'Summary Text');

INSERT INTO `result_unit_finder` (`id`,`name`) VALUES (1,'RegEx');
INSERT INTO `result_unit_finder` (`id`,`name`) VALUES (2,'XPath');

INSERT INTO `importable_field` (`id`,`name`) VALUES (1,'Ask');
INSERT INTO `importable_field` (`id`,`name`) VALUES (2,'Bid');
INSERT INTO `importable_field` (`id`,`name`) VALUES (3,'Observation Time');
INSERT INTO `importable_field` (`id`,`name`) VALUES (4,'Value');

INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (1,'INTRADAY',0);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (2,'5 MIN',5);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (3,'10 MIN',10);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (4,'15 MIN',15);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (5,'30 MIN',30);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (6,'1 HOUR',60);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (7,'2 HOUR',120);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (8,'5 HOUR',300);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (9,'1 DAY',1440);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (10,'2 DAY',2880);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (11,'3 DAY',4320);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (12,'1 WEEK',10080);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (13,'2 WEEK',20160);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (14,'1 MONTH',43200);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (15,'3 MONTH',129600);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (16,'6 MONTH',259200);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (17,'1 YEAR',525600);
INSERT INTO `asset_pricing_time_range` (`id`,`name`,`offset_min`) VALUES (18,'MAX',1000000000);

INSERT INTO `activity_approval_strategy` (`id`,`name`) VALUES (1,'None');
INSERT INTO `activity_approval_strategy` (`id`,`name`) VALUES (2,'Boolean');
INSERT INTO `activity_approval_strategy` (`id`,`name`) VALUES (3,'Tristate');
INSERT INTO `activity_approval_strategy` (`id`,`name`) VALUES (4,'String');
INSERT INTO `activity_approval_strategy` (`id`,`name`) VALUES (5,'Map');
