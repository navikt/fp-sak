package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.impl.HentMedlemskapFraRegister;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@ExtendWith(MockitoExtension.class)
public class BekreftOppholdVurderingTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    @Mock
    private PersonopplysningTjeneste personopplysningTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private LocalDate now = LocalDate.now();

    @BeforeEach
    public void beforeEach() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
    }

    @Test
    public void bekreft_oppholdsrett_vurdering() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medSøknadsdato(now);
        scenario.medSøknadHendelse()
                .medFødselsDato(now.minusDays(3))
                .medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        var behandling = scenario.lagre(repositoryProvider);
        var bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setOppholdsrettVurdering(true);
        bekreftetPeriode.setLovligOppholdVurdering(true);
        bekreftetPeriode.setErEosBorger(true);

        var dto = new BekreftOppholdsrettVurderingDto("test", List.of(bekreftetPeriode));

        // Act
        final var medlemskapTjeneste = new MedlemTjeneste(repositoryProvider,
                mock(HentMedlemskapFraRegister.class), repositoryProvider.getMedlemskapVilkårPeriodeRepository(), skjæringstidspunktTjeneste,
                personopplysningTjeneste, mock(UtledVurderingsdatoerForMedlemskapTjeneste.class), mock(VurderMedlemskapTjeneste.class));
        final var medlemskapAksjonspunktTjeneste = new MedlemskapAksjonspunktTjeneste(
                repositoryProvider, mock(HistorikkTjenesteAdapter.class), skjæringstidspunktTjeneste);

        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        var bekreftOppholdOppdaterer = new BekreftOppholdOppdaterer(
                lagMockHistory(), medlemskapTjeneste, medlemskapAksjonspunktTjeneste) {
        };
        bekreftOppholdOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        var vurdertMedlemskap = getVurdertMedlemskap(behandling.getId(), repositoryProvider);
        assertThat(vurdertMedlemskap.getOppholdsrettVurdering()).isTrue();
    }

    private VurdertMedlemskap getVurdertMedlemskap(Long behandlingId, BehandlingRepositoryProvider repositoryProvier) {
        var medlemskapRepository = repositoryProvier.getMedlemskapRepository();
        var vurdertMedlemskap = medlemskapRepository.hentVurdertMedlemskap(behandlingId);
        return vurdertMedlemskap.orElse(null);
    }

    @Test
    public void bekreft_lovlig_opphold_vurdering() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medSøknadsdato(now);
        scenario.medSøknadHendelse()
                .medFødselsDato(now.minusDays(3))
                .medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);

        var behandling = scenario.lagre(repositoryProvider);
        var bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setOppholdsrettVurdering(true);
        bekreftetPeriode.setLovligOppholdVurdering(true);
        bekreftetPeriode.setErEosBorger(true);

        var dto = new BekreftLovligOppholdVurderingDto("test", List.of(bekreftetPeriode));

        // Act
        final var medlemskapTjeneste = new MedlemTjeneste(repositoryProvider,
                mock(HentMedlemskapFraRegister.class), repositoryProvider.getMedlemskapVilkårPeriodeRepository(), skjæringstidspunktTjeneste,
                personopplysningTjeneste, mock(UtledVurderingsdatoerForMedlemskapTjeneste.class), mock(VurderMedlemskapTjeneste.class));
        final var medlemskapAksjonspunktTjeneste = new MedlemskapAksjonspunktTjeneste(
                repositoryProvider, mock(HistorikkTjenesteAdapter.class), skjæringstidspunktTjeneste);

        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        new BekreftOppholdOppdaterer(lagMockHistory(), medlemskapTjeneste, medlemskapAksjonspunktTjeneste) {
        }.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        var behandlingId = behandling.getId();
        var vurdertMedlemskap = getVurdertMedlemskap(behandlingId, repositoryProvider);
        assertThat(vurdertMedlemskap.getLovligOppholdVurdering()).isTrue();
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        var mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
