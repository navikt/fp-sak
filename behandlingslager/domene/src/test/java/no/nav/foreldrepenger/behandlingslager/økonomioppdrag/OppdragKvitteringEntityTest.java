package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class OppdragKvitteringEntityTest {

    private OppdragKvittering.Builder oppdragKvitteringBuilder;
    private OppdragKvittering oppdragKvittering;
    private OppdragKvittering oppdragKvittering_2;

    private static final KodeFagområde KODEFAGOMRADE = KodeFagområde.FP;
    private static final Alvorlighetsgrad ALVORLIGHETSGRAD = Alvorlighetsgrad.OK;
    private static final String BESKR_MELDING = "Beskr melding";
    private static final String MELDING_KODE = "Melding kode";
    private static final Long FAGSYSTEMID = 250L;
    private static final String OPPDRAGGJELDERID = "1";
    private static final String SAKSBEHID = "Z1236525";
    private static final Long BEHANDLINGID = 321L;
    private static final Saksnummer SAKSID = new Saksnummer("700");
    private static final Boolean VENTERKVITTERING = true;
    private static final Long TASKID = 52L;
    private static final String FORVENTET_EXCEPTION = "forventet exception";

    @BeforeEach
    public void setup() {
        oppdragKvitteringBuilder = OppdragKvittering.builder();
        oppdragKvittering = null;
    }

    @Test
    void skal_bygge_instans_med_påkrevde_felter() {
        oppdragKvittering = lagOppdragKvitteringMedPaakrevdeFelter().build();

        assertThat(oppdragKvittering.getAlvorlighetsgrad()).isEqualTo(ALVORLIGHETSGRAD);
        assertThat(oppdragKvittering.getBeskrMelding()).isEqualTo(BESKR_MELDING);
        assertThat(oppdragKvittering.getMeldingKode()).isEqualTo(MELDING_KODE);
    }

    @Test
    void skal_ikke_bygge_instans_hvis_mangler_påkrevde_felter() {
        // mangler oppdrag110
        try {
            oppdragKvitteringBuilder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("oppdrag110");
        }
    }

    @Test
    void skal_håndtere_null_this_feilKlasse_i_equals() {
        oppdragKvittering = lagOppdragKvitteringMedPaakrevdeFelter().build();

        assertThat(oppdragKvittering)
            .isNotNull()
            .isNotEqualTo("blabla");
    }

    @Test
    void skal_ha_refleksiv_equalsOgHashCode() {
        oppdragKvitteringBuilder = lagOppdragKvitteringMedPaakrevdeFelter();
        oppdragKvittering = oppdragKvitteringBuilder.build();

        var oppdragKvitteringBuilder2 = lagOppdragKvitteringMedPaakrevdeFelter();
        oppdragKvittering_2 = oppdragKvitteringBuilder2.build();

        assertThat(oppdragKvittering).isEqualTo(oppdragKvittering_2);
        assertThat(oppdragKvittering_2).isEqualTo(oppdragKvittering);

        var oppdragKvitteringBuilder3 = lagOppdragKvitteringMedPaakrevdeFelter();
        var oppdragKvittering_3 = oppdragKvitteringBuilder3.medAlvorlighetsgrad(Alvorlighetsgrad.OK_MED_MERKNAD).build();
        assertThat(oppdragKvittering).isNotEqualTo(oppdragKvittering_3);
        assertThat(oppdragKvittering_3).isNotEqualTo(oppdragKvittering);
    }

    @Test
    void skal_bruke_MeldingKode_i_equalsOgHashCode() {
        oppdragKvitteringBuilder = lagOppdragKvitteringMedPaakrevdeFelter();
        oppdragKvittering = oppdragKvitteringBuilder.build();

        var oppdragKvitteringBuilder2 = lagOppdragKvitteringMedPaakrevdeFelter();
        oppdragKvitteringBuilder2.medMeldingKode("Melding kode 2");
        oppdragKvittering_2 = oppdragKvitteringBuilder2.build();

        assertThat(oppdragKvittering).isNotEqualTo(oppdragKvittering_2);
        assertThat(oppdragKvittering.hashCode()).isNotEqualTo(oppdragKvittering_2.hashCode());

    }

    private OppdragKvittering.Builder lagOppdragKvitteringMedPaakrevdeFelter() {
        return OppdragKvittering.builder()
                .medAlvorlighetsgrad(ALVORLIGHETSGRAD)
                .medBeskrMelding(BESKR_MELDING)
                .medMeldingKode(MELDING_KODE)
                .medOppdrag110(lagOppdrag110MedPaakrevdeFelter().build());
    }

    private Oppdrag110.Builder lagOppdrag110MedPaakrevdeFelter() {
        return Oppdrag110.builder()
                .medKodeEndring(KodeEndring.NY)
                .medKodeFagomrade(KODEFAGOMRADE)
                .medFagSystemId(FAGSYSTEMID)
                .medOppdragGjelderId(OPPDRAGGJELDERID)
                .medSaksbehId(SAKSBEHID)
                .medAvstemming(Avstemming.ny())
                .medOppdragskontroll(lagOppdragskontrollMedPaakrevdeFelter().build());
    }

    private Oppdragskontroll.Builder lagOppdragskontrollMedPaakrevdeFelter() {
        return Oppdragskontroll.builder()
                .medBehandlingId(BEHANDLINGID)
                .medSaksnummer(SAKSID)
                .medVenterKvittering(VENTERKVITTERING)
                .medProsessTaskId(TASKID);
    }
}
