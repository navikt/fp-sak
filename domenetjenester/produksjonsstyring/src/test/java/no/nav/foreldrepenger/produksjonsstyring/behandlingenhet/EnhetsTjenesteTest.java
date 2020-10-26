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

import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingResponse;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRestKlient;

@ExtendWith(MockitoExtension.class)
public class EnhetsTjenesteTest {

    private static AktørId MOR_AKTØR_ID = AktørId.dummy();

    private static AktørId FAR_AKTØR_ID = AktørId.dummy();

    private static AktørId BARN_AKTØR_ID = AktørId.dummy();

    private static final Set<AktørId> FAMILIE = Set.of(MOR_AKTØR_ID, FAR_AKTØR_ID, BARN_AKTØR_ID);

    private static OrganisasjonsEnhet enhetNormal = new OrganisasjonsEnhet("4802", "NAV Bærum");
    private static OrganisasjonsEnhet enhetKode6 = new OrganisasjonsEnhet("2103", "NAV Viken");
    private static ArbeidsfordelingResponse respNormal = new ArbeidsfordelingResponse("4802", "NAV Bærum", "Aktiv", "FPY");
    private static ArbeidsfordelingResponse respKode6 = new ArbeidsfordelingResponse("2103", "NAV Viken", "Aktiv", "KO");

    private static GeografiskTilknytning tilknytningNormal = new GeografiskTilknytning("0219", Diskresjonskode.UDEFINERT);
    private static GeografiskTilknytning tilknytningKode6 = new GeografiskTilknytning("0219", Diskresjonskode.KODE6);

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private ArbeidsfordelingRestKlient arbeidsfordelingTjeneste;
    private EnhetsTjeneste enhetsTjeneste;

    @BeforeEach
    public void oppsett() {
        enhetsTjeneste = new EnhetsTjeneste(personinfoAdapter, arbeidsfordelingTjeneste);
    }

    @Test
    public void finn_enhet_utvidet_normal_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, false, true);

        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);

        assertThat(enhet).isNotNull();
        assertThat(enhet).isEqualTo(enhetNormal);
    }

    @Test
    public void finn_enhet_utvidet_bruker_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(true, false, false, true);

        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);

        assertThat(enhet).isNotNull();
        assertThat(enhet).isEqualTo(enhetKode6);
    }

    @Test
    public void finn_enhet_utvidet_barn_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, true, false, true);

        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);
        OrganisasjonsEnhet enhet1 = enhetsTjeneste
                .oppdaterEnhetSjekkOppgittePersoner(enhet.getEnhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE).orElse(enhet);

        assertThat(enhet).isNotNull();
        assertThat(enhet1).isEqualTo(enhetKode6);
    }

    @Test
    public void finn_enhet_utvidet_ektefelle_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, true, true);

        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);
        OrganisasjonsEnhet enhet1 = enhetsTjeneste
                .oppdaterEnhetSjekkOppgittePersoner(enhet.getEnhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE).orElse(enhet);

        assertThat(enhet).isNotNull();
        assertThat(enhet1).isEqualTo(enhetKode6);
    }

    @Test
    public void oppdater_enhet_annenpart_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, true, false);

        Optional<OrganisasjonsEnhet> enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.getEnhetId(), BehandlingTema.ENGANGSSTØNAD,
                MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    public void presendens_enhet() {
        // Oppsett
        settOppTpsStrukturer(false, false, false, false);

        OrganisasjonsEnhet enhet = enhetsTjeneste.enhetsPresedens(enhetNormal, enhetKode6);

        assertThat(enhet).isEqualTo(enhetKode6);
    }

    @Test
    public void oppdater_enhet_mor_annenpart_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(true, false, true, false);

        Optional<OrganisasjonsEnhet> enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetKode6.getEnhetId(), BehandlingTema.ENGANGSSTØNAD,
                MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isNotPresent();
    }

    @Test
    public void oppdater_etter_vent_barn_fått_kode6() {
        // Oppsett
        settOppTpsStrukturer(false, true, false, true);

        Optional<OrganisasjonsEnhet> enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.getEnhetId(), BehandlingTema.ENGANGSSTØNAD,
                MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    public void oppdater_etter_vent_far_fått_kode6() {
        // Oppsett
        settOppTpsStrukturer(false, false, true, false);

        Optional<OrganisasjonsEnhet> enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.getEnhetId(), BehandlingTema.ENGANGSSTØNAD,
                MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    private void settOppTpsStrukturer(boolean morKode6, boolean barnKode6, boolean annenPartKode6, boolean foreldreRelatertTps) {

        lenient().when(personinfoAdapter.hentGeografiskTilknytning(MOR_AKTØR_ID)).thenReturn(morKode6 ? tilknytningKode6 : tilknytningNormal);
        lenient().when(personinfoAdapter.hentGeografiskTilknytning(FAR_AKTØR_ID)).thenReturn(annenPartKode6 ? tilknytningKode6 : tilknytningNormal);
        lenient().when(personinfoAdapter.hentGeografiskTilknytning(BARN_AKTØR_ID)).thenReturn(barnKode6 ? tilknytningKode6 : tilknytningNormal);

        lenient().doAnswer((Answer<List<ArbeidsfordelingResponse>>) invocation -> {
            ArbeidsfordelingRequest data = (ArbeidsfordelingRequest) invocation.getArguments()[0];
            return Objects.equals("SPSF", data.getDiskresjonskode()) ? List.of(respKode6) : List.of(respNormal);
        }).when(arbeidsfordelingTjeneste).finnEnhet(any(ArbeidsfordelingRequest.class));

    }
}
