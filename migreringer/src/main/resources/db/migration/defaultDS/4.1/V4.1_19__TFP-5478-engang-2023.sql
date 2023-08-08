UPDATE BR_SATS set TOM = to_date('2022-12-31', 'YYYY-MM-DD')
where verdi = 90300 AND sats_type = 'ENGANG';

MERGE INTO br_sats s
USING dual b
ON (s.fom = TO_DATE('2023-01-01', 'YYYY-MM-DD') AND s.sats_type = 'ENGANG' )
when not matched then
    INSERT (ID, SATS_TYPE, FOM, TOM, VERDI)
    VALUES (seq_br_sats.nextval, 'ENGANG', TO_DATE('2023-01-01', 'YYYY-MM-DD'), TO_DATE('9999-12-31', 'YYYY-MM-DD'), 92648);
