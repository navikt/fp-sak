-- ##################################################
-- ### Grant for fpinfo###
--
-- ### Legger du til/fjerner views i fpinfo, bør du gjøre tilsvarende endringer her slik
-- ### at bare nødvendige grants er aktivert.
--
-- ##################################################

DECLARE

    FUNCTION f_schema_exists(schema_navn VARCHAR2)
        RETURN BOOLEAN IS
        userexists INTEGER := 0;
    BEGIN
        SELECT count(*)
        INTO userexists
        FROM SYS.ALL_USERS
        WHERE USERNAME = upper(schema_navn);
        IF userexists > 0
        THEN
            RETURN TRUE;
        ELSE
            RETURN FALSE;
        END IF;
    END;

BEGIN

    BEGIN
        IF (f_schema_exists('fpinfo_schema'))
        THEN
            -- Grant for fpinfo_schema (fpinfo)
            EXECUTE IMMEDIATE ('GRANT SELECT ON FAGSAK                              TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON FH_FAMILIE_HENDELSE                 TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON GR_PERSONOPPLYSNING                 TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON PO_INFORMASJON                      TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON PO_RELASJON                         TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON SO_ANNEN_PART                       TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON GR_FAMILIE_HENDELSE                 TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON BRUKER                              TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON BEHANDLING                          TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON MOTTATT_DOKUMENT                    TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON AKSJONSPUNKT                        TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON BEHANDLING_RESULTAT                 TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON BEHANDLING_ARSAK                    TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON BEHANDLING_VEDTAK                   TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON UTTAK_RESULTAT_PERIODE              TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON UTTAK_RESULTAT_PERIODE_AKT          TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON UTTAK_RESULTAT                      TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON UTTAK_RESULTAT_PERIODER             TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON FAGSAK_RELASJON                     TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON FH_ADOPSJON                         TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON FH_TERMINBEKREFTELSE                TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON FH_UIDENTIFISERT_BARN               TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON SO_SOEKNAD                          TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON SO_DEKNINGSGRAD                     TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON SO_RETTIGHET                        TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON YF_DOKUMENTASJON_PERIODE            TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON YF_FORDELING_PERIODE                TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON GR_YTELSES_FORDELING                TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON UTTAK_RESULTAT_PERIODE_SOKNAD       TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON UTTAK_AKTIVITET                     TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON YF_FORDELING                        TO fpinfo_schema WITH GRANT OPTION');
            EXECUTE IMMEDIATE ('GRANT SELECT ON GR_UFORETRYGD                       TO fpinfo_schema WITH GRANT OPTION');
        END IF;
    END;
END;
