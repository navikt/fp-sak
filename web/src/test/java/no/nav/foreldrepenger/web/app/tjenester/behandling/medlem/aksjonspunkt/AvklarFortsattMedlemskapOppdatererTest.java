package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(JpaExtension.class)
class AvklarFortsattMedlemskapOppdatererTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private final LocalDate now = LocalDate.now();

    @BeforeEach
    public void beforeEach(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void avklar_fortsatt_medlemskap() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medSøknadsdato(now);
        scenario.medSøknadHendelse()
                .medFødselsDato(now.minusDays(3))
                .medAntallBarn(1);

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);

        var behandling = scenario.lagre(repositoryProvider);

        var bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setVurderingsdato(now.plusDays(10));
        bekreftetPeriode.setAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT.getKode()));

        var dto = new AvklarFortsattMedlemskapDto("test", List.of(bekreftetPeriode));
        var historikkTjenesteAdapter = mock(HistorikkTjenesteAdapter.class);
        var historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder();
        when(historikkTjenesteAdapter.tekstBuilder()).thenReturn(historikkInnslagTekstBuilder);

        var medlemskapTjeneste = new MedlemskapAksjonspunktTjeneste(repositoryProvider, historikkTjenesteAdapter,
            lagMockYtelseSkjæringstidspunktTjeneste(LocalDate.now()));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        new AvklarFortsattMedlemskapOppdaterer(medlemskapTjeneste)
                .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var vurdertMedlemskap = getVurdertLøpendeMedlemskap(behandling.getId(), repositoryProvider);
        assertThat(vurdertMedlemskap).isPresent();
    }

    private Optional<VurdertMedlemskapPeriodeEntitet> getVurdertLøpendeMedlemskap(Long behandlingId,
            BehandlingRepositoryProvider repositoryProvider) {
        var medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        return medlemskapRepository.hentVurdertLøpendeMedlemskap(behandlingId);
    }

    private SkjæringstidspunktTjeneste lagMockYtelseSkjæringstidspunktTjeneste(LocalDate fom) {
        var skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(fom).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        return skjæringstidspunktTjeneste;
    }
}
