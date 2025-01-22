package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

class OppdragskontrollEntityTest {
    private Oppdragskontroll.Builder oppdragskontrollBuilder;
    private Oppdragskontroll oppdragskontroll;
    private Oppdragskontroll oppdragskontroll_2;

    private static final Saksnummer SAKSID = new Saksnummer("700");
    private static final Boolean VENTERKVITTERING = true;
    private static final Long TASKID = 52L;

    private static final String FORVENTET_EXCEPTION = "forventet exception";
    private static final Long BEHANDLINGID = 321L;

    @BeforeEach
    public void setup() {
        oppdragskontrollBuilder = Oppdragskontroll.builder();
        oppdragskontroll = null;
    }

    @Test
    void skal_bygge_instans_med_påkrevde_felter() {
        oppdragskontroll = lagBuilderMedPaakrevdeFelter().build();

        assertThat(oppdragskontroll.getSaksnummer()).isEqualTo(SAKSID);
        assertThat(oppdragskontroll.getVenterKvittering()).isEqualTo(VENTERKVITTERING);
    }

    @Test
    void skal_ikke_bygge_instans_hvis_mangler_påkrevde_felter() {

        // mangler behandlingId
        try {
            oppdragskontrollBuilder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("behandlingId");
        }

        // mangler saksnummer
        oppdragskontrollBuilder.medBehandlingId(BEHANDLINGID);
        try {
            oppdragskontrollBuilder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("saksnummer");
        }

        // mangler venterKvittering
        oppdragskontrollBuilder.medSaksnummer(SAKSID);
        try {
            oppdragskontrollBuilder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("venterKvittering");
        }

        // mangler taskId
        oppdragskontrollBuilder.medVenterKvittering(Boolean.TRUE);
        try {
            oppdragskontrollBuilder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("prosessTaskId");
        }

    }

    @Test
    void skal_håndtere_null_this_feilKlasse_i_equals() {
        oppdragskontroll = lagBuilderMedPaakrevdeFelter().build();

        assertThat(oppdragskontroll)
            .isNotNull()
            .isNotEqualTo("blabla");
    }

    @Test
    void skal_ha_refleksiv_equalsOgHashCode() {
        oppdragskontrollBuilder = lagBuilderMedPaakrevdeFelter();
        oppdragskontroll = oppdragskontrollBuilder.build();
        oppdragskontroll_2 = oppdragskontrollBuilder.build();

        assertThat(oppdragskontroll).isEqualTo(oppdragskontroll_2);
        assertThat(oppdragskontroll_2).isEqualTo(oppdragskontroll);

        oppdragskontroll_2 = oppdragskontrollBuilder.medVenterKvittering(Boolean.FALSE).build();
        assertThat(oppdragskontroll).isNotEqualTo(oppdragskontroll_2);
        assertThat(oppdragskontroll_2).isNotEqualTo(oppdragskontroll);
    }

    @Test
    void skal_bruke_SaksId_i_equalsOgHashCode() {
        oppdragskontrollBuilder = lagBuilderMedPaakrevdeFelter();
        oppdragskontroll = oppdragskontrollBuilder.build();
        oppdragskontrollBuilder.medSaksnummer(new Saksnummer("701"));
        oppdragskontroll_2 = oppdragskontrollBuilder.build();

        assertThat(oppdragskontroll).isNotEqualTo(oppdragskontroll_2);
        assertThat(oppdragskontroll.hashCode()).isNotEqualTo(oppdragskontroll_2.hashCode());

    }

    @Test
    void skal_bruke_VenterKvittering_i_equalsOgHashCode() {
        oppdragskontrollBuilder = lagBuilderMedPaakrevdeFelter();
        oppdragskontroll = oppdragskontrollBuilder.build();
        oppdragskontrollBuilder.medVenterKvittering(Boolean.FALSE);
        oppdragskontroll_2 = oppdragskontrollBuilder.build();

        assertThat(oppdragskontroll).isNotEqualTo(oppdragskontroll_2);
        assertThat(oppdragskontroll.hashCode()).isNotEqualTo(oppdragskontroll_2.hashCode());

    }

    private Oppdragskontroll.Builder lagBuilderMedPaakrevdeFelter() {
        return Oppdragskontroll.builder()
                .medBehandlingId(BEHANDLINGID)
                .medSaksnummer(SAKSID)
                .medVenterKvittering(VENTERKVITTERING)
                .medProsessTaskId(TASKID);
    }
}
