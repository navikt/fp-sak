package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@CdiDbAwareTest
class KontrollerFaktaStegImplTest {

    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
    private KontrollerFaktaTjeneste kontrollerFaktaTjeneste;

    private Behandling behandling;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;

    private KontrollerFaktaSteg steg;

    private ScenarioFarSøkerEngangsstønad byggBehandlingMedFarSøkerType(FarSøkerType farSøkerType) {
        var aktørId = AktørId.dummy();
        var scenario = ScenarioFarSøkerEngangsstønad
                .forAdopsjon();
        scenario.medBruker(aktørId, NavBrukerKjønn.MANN);
        scenario.medSøknad()
                .medFarSøkerType(farSøkerType);
        scenario.medSøknadHendelse()
                .medFødselsDato(FØDSELSDATO_BARN);

        // Søker må være lagret i BekreftetForeldre
        leggTilSøker(scenario, NavBrukerKjønn.MANN);

        return scenario;
    }

    @BeforeEach
    public void oppsett() {
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        var scenario = byggBehandlingMedFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        scenario.medBruker(AktørId.dummy(), NavBrukerKjønn.MANN);
        behandling = scenario.lagre(repositoryProvider);

        steg = new KontrollerFaktaStegImpl(repositoryProvider, skjæringstidspunktTjeneste, kontrollerFaktaTjeneste);
    }

    @Test
    void skal_ved_overhopp_bakover_rydde_avklarte_fakta() {
        var fagsak = behandling.getFagsak();
        // Arrange
        var lås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        var stegModellMock = mock(BehandlingStegModell.class);

        // Act
        steg.vedTransisjon(kontekst, stegModellMock, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        var behandlingId = behandling.getId();
        var medlemskapAggregat = repositoryProvider.getMedlemskapRepository().hentMedlemskap(behandlingId);
        assertThat(medlemskapAggregat).isPresent();
        assertThat(medlemskapAggregat.flatMap(MedlemskapAggregat::getVurdertMedlemskap)).isNotPresent();
        behandling = behandlingRepository.hentBehandling(behandlingId);

        assertThat(behandling.getBehandlingsresultat().getBeregningResultat()).isNull();
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, NavBrukerKjønn kjønn) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
                .medPersonas()
                .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn)
                .build();
        scenario.medRegisterOpplysninger(søker);
    }
}
