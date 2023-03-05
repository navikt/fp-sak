package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom.SkjermetPersonKlient;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.Arbeidsfordeling;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.ArbeidsfordelingRequest;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.ArbeidsfordelingResponse;

@ExtendWith(MockitoExtension.class)
class EnhetsTjenesteTest {

    private static AktørId MOR_AKTØR_ID = AktørId.dummy();
    private static AktørId FAR_AKTØR_ID = AktørId.dummy();
    private static AktørId BARN_AKTØR_ID = AktørId.dummy();
    private static final Set<AktørId> FAMILIE = Set.of(MOR_AKTØR_ID, FAR_AKTØR_ID, BARN_AKTØR_ID);

    private static PersonIdent MOR_PID = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private static PersonIdent FAR_PID = new PersonIdent(new FiktiveFnr().nesteMannFnr());

    private static OrganisasjonsEnhet enhetNormal = new OrganisasjonsEnhet("4802", "NAV Bærum");
    private static OrganisasjonsEnhet enhetKode6 = new OrganisasjonsEnhet("2103", "NAV Viken");
    private static OrganisasjonsEnhet enhetSkjermet = new OrganisasjonsEnhet("4883", "NAV Skjermet");
    private static ArbeidsfordelingResponse respNormal = new ArbeidsfordelingResponse("4802", "NAV Bærum", "Aktiv", "FPY");
    private static ArbeidsfordelingResponse respKode6 = new ArbeidsfordelingResponse("2103", "NAV Viken", "Aktiv", "KO");

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private Arbeidsfordeling arbeidsfordelingTjeneste;
    @Mock
    private SkjermetPersonKlient skjermetPersonKlient;
    private EnhetsTjeneste enhetsTjeneste;

    @BeforeEach
    public void oppsett() {
        enhetsTjeneste = new EnhetsTjeneste(personinfoAdapter, arbeidsfordelingTjeneste, skjermetPersonKlient);
    }

    @Test
    void finn_enhet_utvidet_normal_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, false);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);

        assertThat(enhet).isNotNull();
        assertThat(enhet).isEqualTo(enhetNormal);
    }

    @Test
    void finn_enhet_utvidet_bruker_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(true, false, false);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);

        assertThat(enhet).isNotNull();
        assertThat(enhet).isEqualTo(enhetKode6);
    }

    @Test
    void finn_enhet_utvidet_bruker_skjermet_fordeling() {
        // Oppsett
        settOppSkjermetStrukturer(true, false);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);

        assertThat(enhet).isNotNull();
        assertThat(enhet).isEqualTo(enhetSkjermet);
    }

    @Test
    void finn_enhet_utvidet_barn_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, true, false);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);
        var enhet1 = enhetsTjeneste
                .oppdaterEnhetSjekkOppgittePersoner(enhet.enhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE).orElse(enhet);

        assertThat(enhet).isNotNull();
        assertThat(enhet1).isEqualTo(enhetKode6);
    }

    @Test
    void finn_enhet_utvidet_ektefelle_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, true);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);
        var enhet1 = enhetsTjeneste
                .oppdaterEnhetSjekkOppgittePersoner(enhet.enhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE).orElse(enhet);

        assertThat(enhet).isNotNull();
        assertThat(enhet1).isEqualTo(enhetKode6);
    }

    @Test
    void oppdater_enhet_annenpart_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, true);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.enhetId(), BehandlingTema.ENGANGSSTØNAD,
            MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    void finn_enhet_utvidet_annenpart_skjermet_fordeling() {
        // Oppsett
        settOppSkjermetStrukturer(false, true);

        var enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);
        var enhet1 = enhetsTjeneste
            .oppdaterEnhetSjekkOppgittePersoner(enhet.enhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isNotNull();
        assertThat(enhet).isEqualTo(enhetNormal);
        assertThat(enhet1).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetSkjermet));
    }


    @Test
    void presendens_enhet() {
        // Oppsett
        settOppTpsStrukturer(false, false, false);

        var enhet = enhetsTjeneste.enhetsPresedens(enhetNormal, enhetKode6);

        assertThat(enhet).isEqualTo(enhetKode6);
    }

    @Test
    void oppdater_enhet_mor_annenpart_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(true, false, true);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetKode6.enhetId(), BehandlingTema.ENGANGSSTØNAD,
                MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isNotPresent();
    }

    @Test
    void oppdater_etter_vent_barn_fått_kode6() {
        // Oppsett
        settOppTpsStrukturer(false, true, false);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.enhetId(), BehandlingTema.ENGANGSSTØNAD,
                MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    void oppdater_etter_vent_far_fått_kode6() {
        // Oppsett
        settOppTpsStrukturer(false, false, true);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.enhetId(), BehandlingTema.ENGANGSSTØNAD,
                MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    void oppdater_etter_vent_far_skjermet() {
        // Oppsett
        settOppSkjermetStrukturer(false, true);

        var enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.enhetId(), BehandlingTema.ENGANGSSTØNAD,
            MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetSkjermet));
    }

    private void settOppTpsStrukturer(boolean morKode6, boolean barnKode6, boolean annenPartKode6) {

        lenient().when(personinfoAdapter.hentGeografiskTilknytning(any())).thenReturn("0219");

        lenient().when(personinfoAdapter.hentDiskresjonskode(MOR_AKTØR_ID)).thenReturn(morKode6 ? Diskresjonskode.KODE6 : Diskresjonskode.UDEFINERT);
        lenient().when(personinfoAdapter.hentDiskresjonskode(FAR_AKTØR_ID)).thenReturn(annenPartKode6 ? Diskresjonskode.KODE6 : Diskresjonskode.UDEFINERT);
        lenient().when(personinfoAdapter.hentDiskresjonskode(BARN_AKTØR_ID)).thenReturn(barnKode6 ? Diskresjonskode.KODE6 : Diskresjonskode.UDEFINERT);

        lenient().when(personinfoAdapter.hentFnr(MOR_AKTØR_ID)).thenReturn(Optional.of(MOR_PID));
        lenient().when(personinfoAdapter.hentFnr(FAR_AKTØR_ID)).thenReturn(Optional.of(FAR_PID));
        lenient().when(skjermetPersonKlient.erSkjermet(any())).thenReturn(false);

        lenient().doAnswer((Answer<List<ArbeidsfordelingResponse>>) invocation -> {
            var data = (ArbeidsfordelingRequest) invocation.getArguments()[0];
            return Objects.equals("SPSF", data.diskresjonskode()) ? List.of(respKode6) : List.of(respNormal);
        }).when(arbeidsfordelingTjeneste).finnEnhet(any(ArbeidsfordelingRequest.class));

    }

    private void settOppSkjermetStrukturer(boolean morSkjermet, boolean annenPartSkjermet) {

        lenient().when(personinfoAdapter.hentGeografiskTilknytning(any())).thenReturn("0219");

        lenient().when(personinfoAdapter.hentDiskresjonskode(any())).thenReturn(Diskresjonskode.UDEFINERT);

        lenient().when(personinfoAdapter.hentFnr(MOR_AKTØR_ID)).thenReturn(Optional.of(MOR_PID));
        lenient().when(personinfoAdapter.hentFnr(FAR_AKTØR_ID)).thenReturn(Optional.of(FAR_PID));

        lenient().when(skjermetPersonKlient.erSkjermet(MOR_PID.getIdent())).thenReturn(morSkjermet);
        lenient().when(skjermetPersonKlient.erSkjermet(FAR_PID.getIdent())).thenReturn(annenPartSkjermet);

        lenient().doAnswer((Answer<List<ArbeidsfordelingResponse>>) invocation -> List.of(respNormal)).when(arbeidsfordelingTjeneste).finnEnhet(any(ArbeidsfordelingRequest.class));
    }
}
