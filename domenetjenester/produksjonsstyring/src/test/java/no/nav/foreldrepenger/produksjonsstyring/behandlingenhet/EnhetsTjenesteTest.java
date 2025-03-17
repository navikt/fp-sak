package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.ArbeidsfordelingResponse;

@ExtendWith(MockitoExtension.class)
class EnhetsTjenesteTest {

    private static AktørId MOR_AKTØR_ID = AktørId.dummy();
    private static AktørId FAR_AKTØR_ID = AktørId.dummy();
    private static AktørId BARN_AKTØR_ID = AktørId.dummy();
    private static final Set<AktørId> FAMILIE = Set.of(MOR_AKTØR_ID, FAR_AKTØR_ID, BARN_AKTØR_ID);

    private static OrganisasjonsEnhet enhetNormal = new OrganisasjonsEnhet("4867", "Nav foreldrepenger");
    private static OrganisasjonsEnhet enhetKode6 = new OrganisasjonsEnhet("2103", "Nav Vikafossen");
    private static OrganisasjonsEnhet enhetSkjermet = new OrganisasjonsEnhet("4883", "Nav skjermet");
    private static ArbeidsfordelingResponse respNormal = new ArbeidsfordelingResponse("4867", "Nav foreldrepenger", "Aktiv", "FPY");
    private static ArbeidsfordelingResponse respKode6 = new ArbeidsfordelingResponse("2103", "Nav Vikafossen", "Aktiv", "KO");

    @Mock
    private RutingKlient rutingKlient;

    private EnhetsTjeneste enhetsTjeneste;

    @BeforeEach
    public void oppsett() {
        enhetsTjeneste = new EnhetsTjeneste(rutingKlient);
    }

    @Test
    void finn_enhet_utvidet_normal_fordeling() {
        // Oppsett
        settOppPDLStrukturer(false, false, false);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID);

        assertThat(enhet)
            .isNotNull()
            .isEqualTo(enhetNormal);
    }

    @Test
    void finn_enhet_utvidet_bruker_kode_fordeling() {
        // Oppsett
        settOppPDLStrukturer(true, false, false);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID);

        assertThat(enhet)
            .isNotNull()
            .isEqualTo(enhetKode6);
    }

    @Test
    void finn_enhet_utvidet_bruker_skjermet_fordeling() {
        // Oppsett
        settOppSkjermetStrukturer(true, false);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID);

        assertThat(enhet)
            .isNotNull()
            .isEqualTo(enhetSkjermet);
    }

    @Test
    void finn_enhet_utvidet_barn_kode_fordeling() {
        // Oppsett
        settOppPDLStrukturer(false, true, false);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID);
        var enhet1 = enhetsTjeneste
                .oppdaterEnhetSjekkOppgittePersoner(enhet.enhetId(), MOR_AKTØR_ID, FAMILIE, Set.of()).orElse(enhet);

        assertThat(enhet).isNotNull();
        assertThat(enhet1).isEqualTo(enhetKode6);
    }

    @Test
    void finn_enhet_utvidet_ektefelle_kode_fordeling() {
        // Oppsett
        settOppPDLStrukturer(false, false, true);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID);
        var enhet1 = enhetsTjeneste
                .oppdaterEnhetSjekkOppgittePersoner(enhet.enhetId(), MOR_AKTØR_ID, FAMILIE, Set.of()).orElse(enhet);

        assertThat(enhet).isNotNull();
        assertThat(enhet1).isEqualTo(enhetKode6);
    }

    @Test
    void oppdater_enhet_annenpart_kode_fordeling() {
        // Oppsett
        settOppPDLStrukturer(false, false, true);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.enhetId(), MOR_AKTØR_ID, FAMILIE, Set.of());

        assertThat(enhet)
            .isPresent()
            .hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    void finn_enhet_utvidet_annenpart_skjermet_fordeling() {
        // Oppsett
        settOppSkjermetStrukturer(false, true);
        when(rutingKlient.finnRutingEgenskaper(Set.of(MOR_AKTØR_ID.getId()))).thenReturn(Set.of());
        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID);
        var enhet1 = enhetsTjeneste
            .oppdaterEnhetSjekkOppgittePersoner(enhet.enhetId(), MOR_AKTØR_ID, FAMILIE, Set.of());

        assertThat(enhet)
            .isNotNull()
            .isEqualTo(enhetNormal);
        assertThat(enhet1).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetSkjermet));
    }


    @Test
    void presendens_enhet() {
        // Oppsett
        settOppPDLStrukturer(false, false, false);

        var enhet = EnhetsTjeneste.enhetsPresedens(enhetNormal, enhetKode6);

        assertThat(enhet).isEqualTo(enhetKode6);
    }

    @Test
    void oppdater_enhet_mor_annenpart_kode_fordeling() {
        // Oppsett
        settOppPDLStrukturer(true, false, true);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetKode6.enhetId(), MOR_AKTØR_ID, FAMILIE, Set.of());

        assertThat(enhet).isNotPresent();
    }

    @Test
    void oppdater_etter_vent_barn_fått_kode6() {
        // Oppsett
        settOppPDLStrukturer(false, true, false);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.enhetId(), MOR_AKTØR_ID, FAMILIE, Set.of());

        assertThat(enhet)
            .isPresent()
            .hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    void oppdater_etter_vent_far_fått_kode6() {
        // Oppsett
        settOppPDLStrukturer(false, false, true);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.enhetId(), MOR_AKTØR_ID, FAMILIE, Set.of());

        assertThat(enhet)
            .isPresent()
            .hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    void oppdater_etter_vent_far_skjermet() {
        // Oppsett
        settOppSkjermetStrukturer(false, true);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.enhetId(), MOR_AKTØR_ID, FAMILIE, Set.of());

        assertThat(enhet)
            .isPresent()
            .hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetSkjermet));
    }

    private void settOppPDLStrukturer(boolean morKode6, boolean barnKode6, boolean annenPartKode6) {

        lenient().when(rutingKlient.finnRutingEgenskaper(any())).thenReturn(morKode6 || annenPartKode6 || barnKode6? Set.of(RutingResultat.STRENGTFORTROLIG) : Set.of());
    }

    private void settOppSkjermetStrukturer(boolean morSkjermet, boolean annenPartSkjermet) {

        lenient().when(rutingKlient.finnRutingEgenskaper(any())).thenReturn(morSkjermet || annenPartSkjermet ? Set.of(RutingResultat.SKJERMING) : Set.of());
    }
}
