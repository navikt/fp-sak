package no.nav.foreldrepenger.db.validering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import no.nav.foreldrepenger.dbstoette.TestDatabaseInit;
import no.nav.foreldrepenger.konfig.Environment;

/**
 * Tester at alle migreringer følger standarder for navn og god praksis.
 */
class SjekkDbStrukturTest {

    private static final String HJELP = """

            Du har nylig lagt til en ny tabell eller kolonne som ikke er dokumentert ihht. gjeldende regler for dokumentasjon.
            Vennligst gå over sql scriptene og dokumenter tabellene på korrekt måte.

            """;

    private static DataSource ds;
    private static String schema;

    @BeforeAll
    public static void setup() {
        var testDbContainer = Environment.current().getProperty("testcontainer.test.db", String.class, "gvenzl/oracle-free:23-slim-faststart");

        var initPath = "../.oracle/oracle-init/fpsak.sql";
        while (!(new File(initPath)).exists()) {
            initPath = "../" + initPath;
        }
        var testDatabase = new OracleContainer(DockerImageName.parse(testDbContainer))
            .withCopyFileToContainer(MountableFile.forHostPath(initPath), "/docker-entrypoint-initdb.d/init.sql")
            .withReuse(true);
        testDatabase.start();
        ds = TestDatabaseInit.settOppDatasourceOgMigrer(testDatabase.getJdbcUrl(), "fpsak", "fpsak", TestDatabaseInit.DEFAULT_DS_SCHEMA);
        TestDatabaseInit.settJdniOppslag(ds);
        schema = TestDatabaseInit.DEFAULT_DS_SCHEMA;
    }

    @Test
    void sjekk_at_alle_tabeller_er_dokumentert() throws Exception {
        var sql = """
            SELECT table_name FROM all_tab_comments
            WHERE (comments IS NULL OR comments in ('', 'MISSING COLUMN COMMENT'))
            AND owner=sys_context('userenv', 'current_schema')
            AND table_name NOT LIKE 'schema_%'
            AND table_name not like '%_MOCK'
            AND table_name NOT LIKE 'HTE_%'
            """;
        List<String> avvik = new ArrayList<>();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery()) {

            while (rs.next()) {
                if (!"flyway_schema_history".equals(rs.getString(1)))
                    avvik.add(rs.getString(1));
            }

        }

        assertThat(avvik).isEmpty();
    }

    @Test
    void sjekk_at_alle_relevant_kolonner_er_dokumentert() throws Exception {
        List<String> avvik = new ArrayList<>();

        var sql = """
                SELECT t.table_name||'.'||t.column_name
                  FROM all_col_comments t
                 WHERE (t.comments IS NULL OR t.comments = '')
                   AND t.owner = sys_context('userenv','current_schema')
                   AND ( upper(t.table_name) NOT LIKE 'SCHEMA_%' AND upper(t.table_name) NOT LIKE '%_MOCK' AND upper(t.table_name) NOT LIKE 'HTE_%')
                   AND NOT EXISTS (SELECT 1 FROM all_constraints a, all_cons_columns b
                                    WHERE a.table_name = b.table_name
                                      AND b.table_name = t.table_name
                                      AND a.constraint_name = b.constraint_name
                                      AND b.column_name = t.column_name
                                      AND constraint_type IN ('P','R')
                                      AND a.owner = t.owner
                                      AND b.owner = a.owner)
                   AND upper(t.column_name) NOT IN ('OPPRETTET_TID','ENDRET_TID','OPPRETTET_AV','ENDRET_AV','VERSJON','BESKRIVELSE','NAVN','FOM', 'TOM', 'LANDKODE', 'AKTIV')
                 ORDER BY t.table_name, t.column_name
                """;

        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery()) {

            while (rs.next()) {
                if (!rs.getString(1).startsWith("flyway_schema_history"))
                    avvik.add("\n" + rs.getString(1));
            }

        }

        assertThat(avvik).withFailMessage("Mangler dokumentasjon for %s kolonner. %s\n %s", avvik.size(), avvik, HJELP).isEmpty();
    }

    @Test
    void sjekk_at_alle_FK_kolonner_har_fornuftig_indekser() throws Exception {
        var sql = """
                SELECT
                  uc.table_name, uc.constraint_name, LISTAGG(dcc.column_name, ',') WITHIN GROUP (ORDER BY dcc.position) as columns
                FROM all_Constraints Uc
                  INNER JOIN all_cons_columns dcc ON dcc.constraint_name  =uc.constraint_name AND dcc.owner=uc.owner
                WHERE Uc.Constraint_Type='R'
                  AND Uc.Owner            = upper(?)
                  AND EXISTS
                      (SELECT ucc.position, ucc.column_name
                        FROM all_cons_columns ucc
                        WHERE Ucc.Constraint_Name=Uc.Constraint_Name
                          AND Uc.Owner             =Ucc.Owner
                      MINUS
                       SELECT uic.column_position AS position, uic.column_name
                       FROM all_ind_columns uic
                       WHERE uic.table_name=uc.table_name
                         AND uic.table_owner =uc.owner
                      )
                GROUP BY Uc.Table_Name, Uc.Constraint_Name
                ORDER BY uc.table_name
                """;

        List<String> avvik = new ArrayList<>();
        var tekst = new StringBuilder();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schema);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    var t = rs.getString(1) + ", " + rs.getString(2) + ", " + rs.getString(3);
                    avvik.add(t);
                    tekst.append(t).append("\n");
                }
            }

        }
        var sz = avvik.size();
        var manglerIndeks = "Kolonner som inngår i Foreign Keys skal ha indekser .\nMangler indekser for ";

        assertThat(avvik).withFailMessage(manglerIndeks + sz + " foreign keys\n" + tekst).isEmpty();

    }

    @Test
    void skal_ha_primary_key_i_hver_tabell_som_begynner_med_PK() throws Exception {
        var sql = """
                SELECT table_name FROM all_tables at
                WHERE table_name
                 NOT IN ( SELECT ac.table_name FROM all_constraints ac
                         WHERE ac.constraint_type ='P' and at.owner=ac.owner and ac.constraint_name like 'PK_%')
                AND at.owner=upper(?)
                and at.table_name not like 'schema_%'
                and at.table_name not like 'HTE_%'
                """;

        List<String> avvik = new ArrayList<>();
        var tekst = new StringBuilder();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schema);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    var t = rs.getString(1);
                    avvik.add(t);
                    tekst.append(t).append("\n");
                }
            }

        }

        var sz = avvik.size();
        var feilTekst = "Feil eller mangelende definisjon av primary key (skal hete 'PK_<tabell navn>'). Antall feil=";

        assertThat(avvik).withFailMessage(feilTekst + +sz + "\n\nTabell\n" + tekst).isEmpty();

    }

    @Test
    void skal_ha_alle_foreign_keys_begynne_med_FK() throws Exception {
        var sql = "SELECT ac.table_name, ac.constraint_name FROM all_constraints ac"
            + " WHERE ac.constraint_type ='R' and ac.owner=upper(?) and constraint_name NOT LIKE 'FK_%'";

        List<String> avvik = new ArrayList<>();
        var tekst = new StringBuilder();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schema);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    var t = rs.getString(1) + ", " + rs.getString(2);
                    avvik.add(t);
                    tekst.append(t).append("\n");
                }
            }

        }

        var sz = avvik.size();
        var feilTekst = "Feil eller mangelende definisjon av foreign key (skal hete 'FK_<tabell navn>_<løpenummer>'). Antall feil=";

        assertThat(avvik).withFailMessage(feilTekst + sz + "\n\nTabell, Foreign Key\n" + tekst).isEmpty();

    }

    @Test
    void skal_ha_korrekt_index_navn() throws Exception {
        var sql = """
                select table_name, index_name, column_name
                 from all_ind_columns
                 where table_owner=upper(?)
                 and index_name not like 'PK_%' and index_name not like 'IDX_%' and index_name not like 'UIDX_%'
                 and table_name not like 'schema_%'
                 and table_name not like 'HTE_%'
                 and table_name not like 'BIN$%'
                """;

        List<String> avvik = new ArrayList<>();
        var tekst = new StringBuilder();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schema);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    var t = rs.getString(1) + ", " + rs.getString(2) + ", " + rs.getString(3);
                    avvik.add(t);
                    tekst.append(t).append("\n");
                }
            }

        }

        var sz = avvik.size();
        var feilTekst = "Feil navngiving av index.  Primary Keys skal ha prefiks PK_, andre unike indekser prefiks UIDX_, vanlige indekser prefiks IDX_. Antall feil=";

        assertThat(avvik).withFailMessage(feilTekst + +sz + "\n\nTabell, Index, Kolonne\n" + tekst).isEmpty();

    }

    @Test
    void skal_ha_samme_data_type_for_begge_sider_av_en_FK() throws Exception {
        var sql = """
                SELECT T.TABLE_NAME
                , TCC.COLUMN_NAME AS KOL_A
                , ATT.DATA_TYPE AS KOL_A_DATA_TYPE
                , ATT.CHAR_LENGTH AS KOL_A_CHAR_LENGTH
                , ATT.CHAR_USED AS KOL_A_CHAR_USED
                , RCC.COLUMN_NAME AS KOL_B
                , ATR.DATA_TYPE AS KOL_B_DATA_TYPE
                , ATR.CHAR_LENGTH AS KOL_B_CHAR_LENGTH
                , atr.CHAR_USED as KOL_B_CHAR_USED
                FROM ALL_CONSTRAINTS T
                INNER JOIN ALL_CONSTRAINTS R ON R.OWNER=T.OWNER AND R.CONSTRAINT_NAME = T.R_CONSTRAINT_NAME
                INNER JOIN ALL_CONS_COLUMNS TCC ON TCC.TABLE_NAME=T.TABLE_NAME AND TCC.OWNER=T.OWNER AND TCC.CONSTRAINT_NAME=T.CONSTRAINT_NAME
                INNER JOIN ALL_CONS_COLUMNS RCC ON RCC.TABLE_NAME = R.TABLE_NAME AND RCC.OWNER=R.OWNER AND RCC.CONSTRAINT_NAME=R.CONSTRAINT_NAME
                INNER JOIN ALL_TAB_COLS ATT ON ATT.COLUMN_NAME=TCC.COLUMN_NAME AND ATT.OWNER=TCC.OWNER AND Att.TABLE_NAME=TCC.TABLE_NAME
                inner join all_tab_cols atr on atr.column_name=rcc.column_name and atr.owner=rcc.owner and atr.table_name=rcc.table_name
                WHERE T.OWNER=upper(?) AND T.CONSTRAINT_TYPE='R'
                AND TCC.POSITION = RCC.POSITION
                AND TCC.POSITION IS NOT NULL AND RCC.POSITION IS NOT NULL
                AND ((ATT.DATA_TYPE!=ATR.DATA_TYPE) OR (ATT.CHAR_LENGTH!=ATR.CHAR_LENGTH OR ATT.CHAR_USED!=ATR.CHAR_USED) OR (ATT.DATA_TYPE NOT LIKE '%CHAR%' AND ATT.DATA_LENGTH!=ATR.DATA_LENGTH))
                ORDER BY T.TABLE_NAME, TCC.COLUMN_NAME
                """;
        List<String> avvik = new ArrayList<>();
        var tekst = new StringBuilder();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schema);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    var t = rs.getString(1) + ", " + rs.getString(2) + ", " + rs.getString(3) + ", " + rs.getString(4) + ", " + rs.getString(5)
                        + ", " + rs.getString(6) + ", " + rs.getString(7) + ", " + rs.getString(8) + ", " + rs.getString(9);
                    avvik.add(t);
                    tekst.append(t).append("\n");
                }
            }

        }

        var sz = avvik.size();
        var feilTekst = "Forskjellig datatype for kolonne på hver side av en FK. Kan være deklarert feil (husk VARCHAR2(100 CHAR) og ikke VARCHAR2(100)). Antall feil=";
        var cols = ".\n\nTABELL, KOL_A, KOL_A_DATA_TYPE, KOL_A_CHAR_LENGTH, KOL_A_CHAR_USED, KOL_B, KOL_B_DATA_TYPE, KOL_B_CHAR_LENGTH, KOL_B_CHAR_USED\n";

        assertThat(avvik).withFailMessage(feilTekst + +sz + cols + tekst).isEmpty();

    }

    @Test
    void skal_deklarere_VARCHAR2_kolonner_som_CHAR_ikke_BYTE_semantikk() throws Exception {
        var sql = """
                SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHAR_USED, CHAR_LENGTH
                FROM ALL_TAB_COLS
                WHERE DATA_TYPE = 'VARCHAR2'
                AND CHAR_USED !='C' AND TABLE_NAME NOT LIKE '%schema%' AND CHAR_LENGTH>1 AND OWNER=upper(?)
                ORDER BY 1, 2
                """;

        List<String> avvik = new ArrayList<>();
        var tekst = new StringBuilder();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schema);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    var t = rs.getString(1) + ", " + rs.getString(2) + ", " + rs.getString(3) + ", " + rs.getString(4) + ", " + rs.getString(5);
                    avvik.add(t);
                    tekst.append(t).append("\n");
                }
            }

        }

        var sz = avvik.size();
        var feilTekst = "Feil deklarasjon av VARCHAR2 kolonne (husk VARCHAR2(100 CHAR) og ikke VARCHAR2(100)). Antall feil=";
        var cols = ".\n\nTABELL, KOLONNE, DATA_TYPE, CHAR_USED, CHAR_LENGTH\n";

        assertThat(avvik).withFailMessage(feilTekst + +sz + cols + tekst).isEmpty();

    }

    @Test
    void skal_ikke_bruke_FLOAT_eller_DOUBLE() throws Exception {
        var sql = "select table_name, column_name, data_type from all_tab_cols where owner=upper(?) and data_type in ('FLOAT', 'DOUBLE') order by 1, 2";

        List<String> avvik = new ArrayList<>();
        var tekst = new StringBuilder();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schema);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    var t = rs.getString(1) + ", " + rs.getString(2);
                    avvik.add(t);
                    tekst.append(t).append("\n");
                }
            }

        }

        var sz = avvik.size();
        var feilTekst = "Feil bruk av datatype, skal ikke ha FLOAT eller DOUBLE (bruk NUMBER for alle desimaltall, spesielt der penger representeres). Antall feil=";

        assertThat(avvik).withFailMessage(feilTekst + +sz + "\n\nTabell, Kolonne, Datatype\n" + tekst).isEmpty();

    }

    @Test
    void sjekk_at_status_verdiene_i_prosess_task_tabellen_er_også_i_pollingSQL() throws Exception {
        var sql = """
                SELECT SEARCH_CONDITION
                FROM all_constraints
                WHERE table_name = 'PROSESS_TASK'
                AND constraint_name = 'CHK_PROSESS_TASK_STATUS'
                AND owner = sys_context('userenv','current_schema')
                """;

        List<String> statusVerdier = new ArrayList<>();
        try (var conn = ds.getConnection(); var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery()) {

            while (rs.next()) {
                statusVerdier.add(rs.getString(1));
            }

        }
        var feilTekst = "Ved innføring av ny stause må sqlen i TaskManager_pollTask.sql må oppdateres ";
        assertThat(statusVerdier).withFailMessage(feilTekst)
            .containsExactly("status in ('KLAR', 'FEILET', 'VENTER_SVAR', 'SUSPENDERT', 'VETO', 'FERDIG', 'KJOERT')");
    }
}
