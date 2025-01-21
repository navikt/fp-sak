package no.nav.foreldrepenger.domene.uttak.fakta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fakta.omsorg.AnnenForelderHarRettAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.omsorg.BrukerHarAleneomsorgAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

class OmsorgRettUttakTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    private OmsorgRettUttakTjeneste tjeneste;

    private PersonopplysningerForUttak personopplysninger;

    @BeforeEach
    void setUp() {
        personopplysninger = mock(PersonopplysningerForUttak.class);
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
        var annenForelderHarRettAksjonspunktUtleder = new AnnenForelderHarRettAksjonspunktUtleder(repositoryProvider,
            personopplysninger, uttakTjeneste);
        var brukerHarAleneomsorgAksjonspunktUtleder = new BrukerHarAleneomsorgAksjonspunktUtleder(repositoryProvider,
            personopplysninger);
        var ytelseFordelingTjeneste = new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        var utledere = List.of(annenForelderHarRettAksjonspunktUtleder,
            brukerHarAleneomsorgAksjonspunktUtleder);
        tjeneste = new OmsorgRettUttakTjeneste(utledere, ytelseFordelingTjeneste, personopplysninger);
    }

    @Test
    void automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_oppgitt_rett_og_annenpart_uten_norsk_id() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        when(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).thenReturn(true);

        tjeneste.avklarOmAnnenForelderHarRett(ref);

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getAnnenForelderRettAvklaring();
        assertThat(perioderAnnenforelderHarRett)
            .isNotNull()
            .isFalse();
    }

    @Test
    void automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_ikke_oppgitt_rett_og_annenpart_uten_norsk_id() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett());
        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        when(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).thenReturn(true);

        tjeneste.avklarOmAnnenForelderHarRett(ref);

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getAnnenForelderRettAvklaring();
        assertThat(perioderAnnenforelderHarRett).isNull();
    }

    @Test
    void ikke_automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_er_i_pdl() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        when(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).thenReturn(false);

        tjeneste.avklarOmAnnenForelderHarRett(ref);

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getAnnenForelderRettAvklaring();
        assertThat(perioderAnnenforelderHarRett).isNull();
    }

    @Test
    void ikke_automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_ikke_finnes() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarOmAnnenForelderHarRett(BehandlingReferanse.fra(behandling));

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getAnnenForelderRettAvklaring();
        assertThat(perioderAnnenforelderHarRett).isNull();
    }

}
