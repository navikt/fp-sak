UPDATE BR_SATS SET TOM = to_date('2020-04-30', 'YYYY-MM-DD')
where SATS_TYPE = 'GRUNNBELØP' and FOM = to_date('2019-05-01', 'YYYY-MM-DD');

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GRUNNBELØP' and a.FOM = to_date('2020-05-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GRUNNBELØP', to_date('2020-05-01', 'YYYY-MM-DD'), to_date('2119-09-04', 'YYYY-MM-DD'), 101351);

MERGE INTO BR_SATS a
USING DUAL b
ON (a.SATS_TYPE = 'GSNITT' and a.FOM = to_date('2020-01-01', 'YYYY-MM-DD'))
WHEN NOT MATCHED THEN
    INSERT (id, SATS_TYPE, fom, tom, verdi)
    VALUES (SEQ_BR_SATS.nextval, 'GSNITT', to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2020-12-31', 'YYYY-MM-DD'), 100853);
