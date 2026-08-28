-- =============================================================
-- V8 : reference data  (roles, districts, roads, bridges)
--
-- SAMPLE DATA. Centroids are real district headquarters coordinates; the
-- boundary polygons are approximate squares drawn around them, and the road
-- geometries are simplified corridors. Real GSI landslide zonation and OSM
-- road exports can replace these rows later without any schema change.
-- =============================================================

INSERT INTO role (name, description) VALUES
  ('ADMIN',              'Full system administration and user management'),
  ('AUTHORITY_OFFICIAL', 'Government official: road status, incident verification, alerts'),
  ('FIELD_OFFICER',      'Reports incidents from the field, works offline'),
  ('LOGISTICS_MANAGER',  'Manages vehicles, deliveries and route planning'),
  ('DRIVER',             'Drives a vehicle, sends GPS position, updates own delivery');

-- ---------- Districts ----------
-- boundary = a square around the centroid, ST_Envelope over a 0.25 degree buffer.
INSERT INTO district (code, name, state, centroid, boundary, population, area_sq_km) VALUES
  ('AS-KAM','Kamrup Metropolitan','Assam',
   ST_SetSRID(ST_MakePoint(91.7362,26.1445),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(91.7362,26.1445),4326),0.25))),1253938,1528),
  ('AS-CAC','Cachar','Assam',
   ST_SetSRID(ST_MakePoint(92.7789,24.8333),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(92.7789,24.8333),4326),0.30))),1736319,3786),
  ('AS-DIB','Dibrugarh','Assam',
   ST_SetSRID(ST_MakePoint(94.9120,27.4728),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(94.9120,27.4728),4326),0.30))),1326335,3381),
  ('AS-DHA','Dima Hasao','Assam',
   ST_SetSRID(ST_MakePoint(93.0200,25.1800),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(93.0200,25.1800),4326),0.35))),214102,4890),
  ('ML-EKH','East Khasi Hills','Meghalaya',
   ST_SetSRID(ST_MakePoint(91.8933,25.5788),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(91.8933,25.5788),4326),0.30))),825922,2748),
  ('ML-WGH','West Garo Hills','Meghalaya',
   ST_SetSRID(ST_MakePoint(90.2026,25.5140),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(90.2026,25.5140),4326),0.30))),642923,3714),
  ('NL-DIM','Dimapur','Nagaland',
   ST_SetSRID(ST_MakePoint(93.7276,25.9063),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(93.7276,25.9063),4326),0.25))),378811,927),
  ('NL-KOH','Kohima','Nagaland',
   ST_SetSRID(ST_MakePoint(94.1086,25.6751),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(94.1086,25.6751),4326),0.25))),267988,1463),
  ('MN-IMW','Imphal West','Manipur',
   ST_SetSRID(ST_MakePoint(93.9368,24.8170),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(93.9368,24.8170),4326),0.25))),517992,519),
  ('MZ-AIZ','Aizawl','Mizoram',
   ST_SetSRID(ST_MakePoint(92.7176,23.7271),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(92.7176,23.7271),4326),0.30))),400309,3576),
  ('TR-WTR','West Tripura','Tripura',
   ST_SetSRID(ST_MakePoint(91.2868,23.8315),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(91.2868,23.8315),4326),0.25))),917534,942),
  ('AR-PAP','Papum Pare','Arunachal Pradesh',
   ST_SetSRID(ST_MakePoint(93.6053,27.0844),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(93.6053,27.0844),4326),0.30))),176385,2875),
  ('SK-EAS','Gangtok','Sikkim',
   ST_SetSRID(ST_MakePoint(88.6138,27.3314),4326),
   ST_Multi(ST_Envelope(ST_Buffer(ST_SetSRID(ST_MakePoint(88.6138,27.3314),4326),0.25))),283583,954);

-- ---------- Roads ----------
-- length_km is filled in below by PostGIS itself, so it can never drift from
-- the geometry. slope_degrees / landslide_susceptibility / flood_prone /
-- historical_block_days_per_year are the static inputs of the AI risk engine.
INSERT INTO road (code, name, road_type, district_id, geom, length_km, status, condition,
                  current_risk_level, slope_degrees, landslide_susceptibility, flood_prone,
                  historical_block_days_per_year) VALUES
  ('NH06-GS-01','NH-6 Guwahati to Nongpoh','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='AS-KAM'),
   ST_GeomFromText('LINESTRING(91.7362 26.1445, 91.8000 26.0000, 91.8700 25.9000)',4326),
   1,'OPEN','GOOD','LOW_RISK',8,0.35,false,2),

  ('NH06-GS-02','NH-6 Nongpoh to Shillong','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='ML-EKH'),
   ST_GeomFromText('LINESTRING(91.8700 25.9000, 91.8850 25.7200, 91.8933 25.5788)',4326),
   1,'OPEN','GOOD','LOW_RISK',14,0.55,false,5),

  ('NH06-SS-01','NH-6 Shillong to Jowai to Ratacherra','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='ML-EKH'),
   ST_GeomFromText('LINESTRING(91.8933 25.5788, 92.2000 25.4500, 92.5000 25.0000)',4326),
   1,'PARTIALLY_ACCESSIBLE','FAIR','MEDIUM_RISK',22,0.78,false,18),

  ('NH06-SC-01','NH-6 Ratacherra to Silchar','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='AS-CAC'),
   ST_GeomFromText('LINESTRING(92.5000 25.0000, 92.6500 24.9000, 92.7789 24.8333)',4326),
   1,'OPEN','FAIR','LOW_RISK',10,0.40,true,8),

  ('NH27-GN-01','NH-27 Guwahati to Nagaon','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='AS-KAM'),
   ST_GeomFromText('LINESTRING(91.7362 26.1445, 92.3000 26.2000, 92.6800 26.3500)',4326),
   1,'OPEN','GOOD','LOW_RISK',2,0.05,true,4),

  ('NH27-JD-01','NH-27 Jorhat to Dibrugarh','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='AS-DIB'),
   ST_GeomFromText('LINESTRING(94.2000 26.7500, 94.5500 27.1000, 94.9120 27.4728)',4326),
   1,'OPEN','GOOD','LOW_RISK',1,0.03,true,6),

  ('NH02-DK-01','NH-2 Dimapur to Kohima','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='NL-DIM'),
   ST_GeomFromText('LINESTRING(93.7276 25.9063, 93.9000 25.8000, 94.1086 25.6751)',4326),
   1,'HIGH_RISK','POOR','HIGH_RISK',19,0.72,false,22),

  ('NH02-KI-01','NH-2 Kohima to Imphal','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='NL-KOH'),
   ST_GeomFromText('LINESTRING(94.1086 25.6751, 94.0500 25.2000, 93.9368 24.8170)',4326),
   1,'OPEN','FAIR','MEDIUM_RISK',17,0.65,false,14),

  ('NH37-IJ-01','NH-37 Imphal to Jiribam','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='MN-IMW'),
   ST_GeomFromText('LINESTRING(93.9368 24.8170, 93.5000 24.8200, 93.1200 24.8000)',4326),
   1,'PARTIALLY_ACCESSIBLE','POOR','HIGH_RISK',21,0.70,false,25),

  ('NH306-AL-01','NH-306 Aizawl to Lunglei','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='MZ-AIZ'),
   ST_GeomFromText('LINESTRING(92.7176 23.7271, 92.7500 23.2000, 92.7300 22.8800)',4326),
   1,'OPEN','FAIR','MEDIUM_RISK',20,0.60,false,12),

  ('NH10-RG-01','NH-10 Rangpo to Gangtok','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='SK-EAS'),
   ST_GeomFromText('LINESTRING(88.5300 27.1800, 88.5800 27.2500, 88.6138 27.3314)',4326),
   1,'HIGH_RISK','POOR','HIGH_RISK',26,0.88,false,30),

  ('NH415-BI-01','NH-415 Banderdewa to Itanagar','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='AR-PAP'),
   ST_GeomFromText('LINESTRING(93.7800 27.0500, 93.6900 27.0700, 93.6053 27.0844)',4326),
   1,'OPEN','GOOD','LOW_RISK',12,0.45,false,6),

  ('NH08-AU-01','NH-8 Agartala to Udaipur','NATIONAL_HIGHWAY',
   (SELECT id FROM district WHERE code='TR-WTR'),
   ST_GeomFromText('LINESTRING(91.2868 23.8315, 91.3900 23.6800, 91.4800 23.5300)',4326),
   1,'OPEN','GOOD','LOW_RISK',3,0.08,true,5),

  ('SH-DHA-01','Haflong to Maibang State Highway','STATE_HIGHWAY',
   (SELECT id FROM district WHERE code='AS-DHA'),
   ST_GeomFromText('LINESTRING(93.0200 25.1800, 93.0800 25.2400, 93.1300 25.3000)',4326),
   1,'BLOCKED','DAMAGED','HIGH_RISK',24,0.85,false,35),

  ('DR-WGH-01','Tura to Baghmara District Road','DISTRICT_ROAD',
   (SELECT id FROM district WHERE code='ML-WGH'),
   ST_GeomFromText('LINESTRING(90.2026 25.5140, 90.4200 25.3500, 90.6300 25.1900)',4326),
   1,'OPEN','FAIR','LOW_RISK',9,0.30,true,9),

  ('MP-SK-01','Nathu La Approach Pass Road','MOUNTAIN_PASS',
   (SELECT id FROM district WHERE code='SK-EAS'),
   ST_GeomFromText('LINESTRING(88.6138 27.3314, 88.7300 27.3600, 88.8300 27.3900)',4326),
   1,'PARTIALLY_ACCESSIBLE','FAIR','MEDIUM_RISK',28,0.80,false,45);

-- Let PostGIS compute the real length on the curved earth, in kilometres.
UPDATE road SET length_km = ROUND((ST_Length(geom::geography) / 1000)::numeric, 2);

-- ---------- Bridges ----------
-- A single weight-restricted or closed bridge can shut a whole corridor even when
-- every road around it is open, which is why bridges are modelled separately.
INSERT INTO bridge (code, name, road_id, location, load_capacity_tons, condition, status,
                    last_inspection_date) VALUES
  ('BR-UMI-01','Umiam Lake Bridge',
   (SELECT id FROM road WHERE code='NH06-GS-02'),
   ST_SetSRID(ST_MakePoint(91.8850,25.7200),4326), 40, 'GOOD','OPEN','2026-03-12'),

  ('BR-BAR-01','Barapani Approach Bridge',
   (SELECT id FROM road WHERE code='NH06-GS-02'),
   ST_SetSRID(ST_MakePoint(91.8800,25.6800),4326), 25, 'FAIR','WEIGHT_RESTRICTED','2026-01-20'),

  ('BR-JIR-01','Jiri River Bridge',
   (SELECT id FROM road WHERE code='NH37-IJ-01'),
   ST_SetSRID(ST_MakePoint(93.1200,24.8000),4326), 30, 'POOR','WEIGHT_RESTRICTED','2025-11-05'),

  ('BR-DHA-01','Mahur River Bridge',
   (SELECT id FROM road WHERE code='SH-DHA-01'),
   ST_SetSRID(ST_MakePoint(93.0800,25.2400),4326), 20, 'DAMAGED','CLOSED','2026-07-30'),

  ('BR-TEE-01','Teesta River Bridge',
   (SELECT id FROM road WHERE code='NH10-RG-01'),
   ST_SetSRID(ST_MakePoint(88.5300,27.1800),4326), 45, 'FAIR','OPEN','2026-05-18'),

  ('BR-DIK-01','Dikhow River Bridge',
   (SELECT id FROM road WHERE code='NH27-JD-01'),
   ST_SetSRID(ST_MakePoint(94.5500,27.1000),4326), 55, 'GOOD','OPEN','2026-04-02');
