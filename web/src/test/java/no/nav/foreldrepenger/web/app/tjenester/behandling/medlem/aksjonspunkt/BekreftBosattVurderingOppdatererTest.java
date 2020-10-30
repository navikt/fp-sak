package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

public class BekreftBosattVurderingOppdatererTest extends EntityManagerAwareTest {

    private LocalDate now = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;
    private HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @BeforeEach
    public void beforeEach() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
    }

    @Test
    public void bekreft_bosett_vurdering() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medSøknadsdato(now);
        scenario.medSøknadHendelse()
                .medFødselsDato(now.minusDays(3))
                .medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);

        Behandling behandling = scenario.lagre(repositoryProvider);

        BekreftedePerioderDto bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setBosattVurdering(true);
        BekreftBosattVurderingDto dto = new BekreftBosattVurderingDto("test", List.of(bekreftetPeriode));

        // Act
        final MedlemskapAksjonspunktTjeneste medlemskapTjeneste = new MedlemskapAksjonspunktTjeneste(
                repositoryProvider, mock(HistorikkTjenesteAdapter.class), skjæringstidspunktTjeneste);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        new BekreftBosattVurderingOppdaterer(repositoryProvider, lagMockHistory(), medlemskapTjeneste).oppdater(dto,
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        VurdertMedlemskap vurdertMedlemskap = getVurdertMedlemskap(behandling.getId(), repositoryProvider);
        assertThat(vurdertMedlemskap.getBosattVurdering()).isTrue();
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    private VurdertMedlemskap getVurdertMedlemskap(Long behandlingId, BehandlingRepositoryProvider repositoryProvider) {
        MedlemskapRepository medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskapRepository.hentVurdertMedlemskap(behandlingId);
        return vurdertMedlemskap.orElse(null);
    }
}
