package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@CdiDbAwareTest
public class KontrollerFaktaStegImplTest {

    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);

    @Inject
    @FagsakYtelseTypeRef("ES")
    private KontrollerFaktaTjeneste kontrollerFaktaTjeneste;

    private Behandling behandling;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;

    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

    private KontrollerFaktaSteg steg;

    private ScenarioFarSøkerEngangsstønad byggBehandlingMedFarSøkerType(FarSøkerType farSøkerType) {
        AktørId aktørId = AktørId.dummy();
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad
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
        ScenarioFarSøkerEngangsstønad scenario = byggBehandlingMedFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        scenario.medBruker(AktørId.dummy(), NavBrukerKjønn.MANN);
        behandling = scenario.lagre(repositoryProvider);

        steg = new KontrollerFaktaStegImpl(repositoryProvider, skjæringstidspunktTjeneste, kontrollerFaktaTjeneste);
    }

    @Test
    public void skal_ved_overhopp_bakover_rydde_avklarte_fakta() {
        Fagsak fagsak = behandling.getFagsak();
        // Arrange
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        BehandlingStegModell stegModellMock = mock(BehandlingStegModell.class);
        BehandlingModell modellmock = mock(BehandlingModell.class);
        when(stegModellMock.getBehandlingModell()).thenReturn(modellmock);

        // Act
        steg.vedTransisjon(kontekst, stegModellMock, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        Long behandlingId = behandling.getId();
        final Optional<MedlemskapAggregat> medlemskapAggregat = repositoryProvider.getMedlemskapRepository().hentMedlemskap(behandlingId);
        assertThat(medlemskapAggregat).isPresent();
        assertThat(medlemskapAggregat.flatMap(MedlemskapAggregat::getVurdertMedlemskap)).isNotPresent();
        behandling = behandlingRepository.hentBehandling(behandlingId);

        assertThat(behandling.getBehandlingsresultat().getBeregningResultat()).isNull();
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, NavBrukerKjønn kjønn) {
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon søker = builderForRegisteropplysninger
                .medPersonas()
                .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn, Region.UDEFINERT)
                .build();
        scenario.medRegisterOpplysninger(søker);
    }
}
