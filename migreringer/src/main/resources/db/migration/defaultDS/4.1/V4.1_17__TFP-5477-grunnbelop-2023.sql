-- Baseline oppretter som 1000000 mens det gjøres DML som inserter
DROP SEQUENCE SEQ_BR_SATS;
create sequence SEQ_BR_SATS minvalue 1010000 increment by 50 nocache;

UPDATE BR_SATS SET TOM = to_date('2022-04-30', 'YYYY-MM-DD')
where SATS_TYPE = 'GRUNNBELØP' and FOM = to_date('2021-05-01', 'YYYY-MM-DD');

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GRUNNBELØP' and a.FOM = to_date('2022-05-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GRUNNBELØP', to_date('2022-05-01', 'YYYY-MM-DD'), to_date('2023-04-30', 'YYYY-MM-DD'), 111477);

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GSNITT' and a.FOM = to_date('2022-01-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GSNITT', to_date('2022-01-01', 'YYYY-MM-DD'), to_date('2022-12-31', 'YYYY-MM-DD'), 109784);


UPDATE BR_SATS SET TOM = to_date('2023-04-30', 'YYYY-MM-DD')
where SATS_TYPE = 'GRUNNBELØP' and FOM = to_date('2022-05-01', 'YYYY-MM-DD');

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GRUNNBELØP' and a.FOM = to_date('2023-05-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GRUNNBELØP', to_date('2023-05-01', 'YYYY-MM-DD'), to_date('2123-04-30', 'YYYY-MM-DD'), 118620);

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GSNITT' and a.FOM = to_date('2023-01-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GSNITT', to_date('2023-01-01', 'YYYY-MM-DD'), to_date('2023-12-31', 'YYYY-MM-DD'), 116239);

