
UPDATE BR_SATS SET TOM = to_date('2024-04-30', 'YYYY-MM-DD')
where SATS_TYPE = 'GRUNNBELØP' and FOM = to_date('2023-05-01', 'YYYY-MM-DD');

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GRUNNBELØP' and a.FOM = to_date('2024-05-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GRUNNBELØP', to_date('2024-05-01', 'YYYY-MM-DD'), to_date('2025-04-30', 'YYYY-MM-DD'), 124028);

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GSNITT' and a.FOM = to_date('2024-01-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GSNITT', to_date('2024-01-01', 'YYYY-MM-DD'), to_date('2024-12-31', 'YYYY-MM-DD'), 122225);


UPDATE BR_SATS SET TOM = to_date('2025-04-30', 'YYYY-MM-DD')
where SATS_TYPE = 'GRUNNBELØP' and FOM = to_date('2024-05-01', 'YYYY-MM-DD');

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GRUNNBELØP' and a.FOM = to_date('2025-05-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GRUNNBELØP', to_date('2025-05-01', 'YYYY-MM-DD'), to_date('2126-04-30', 'YYYY-MM-DD'), 130160);

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GSNITT' and a.FOM = to_date('2025-01-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GSNITT', to_date('2025-01-01', 'YYYY-MM-DD'), to_date('2025-12-31', 'YYYY-MM-DD'), 128116);

