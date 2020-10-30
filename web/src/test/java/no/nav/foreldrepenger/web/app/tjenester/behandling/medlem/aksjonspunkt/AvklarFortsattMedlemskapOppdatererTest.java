package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.RepositoryAwareTest;

public class AvklarFortsattMedlemskapOppdatererTest extends RepositoryAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private LocalDate now = LocalDate.now();

    @BeforeEach
    public void beforeEach() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
    }

    @Test
    public void avklar_fortsatt_medlemskap() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medSøknadsdato(now);
        scenario.medSøknadHendelse()
                .medFødselsDato(now.minusDays(3))
                .medAntallBarn(1);

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);

        Behandling behandling = scenario.lagre(repositoryProvider);

        BekreftedePerioderDto bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setVurderingsdato(now.plusDays(10));
        bekreftetPeriode.setAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT.getKode()));

        AvklarFortsattMedlemskapDto dto = new AvklarFortsattMedlemskapDto("test", List.of(bekreftetPeriode));
        HistorikkTjenesteAdapter historikkTjenesteAdapter = mock(HistorikkTjenesteAdapter.class);
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder();
        when(historikkTjenesteAdapter.tekstBuilder()).thenReturn(historikkInnslagTekstBuilder);

        final MedlemskapAksjonspunktTjeneste medlemskapTjeneste = new MedlemskapAksjonspunktTjeneste(
                repositoryProvider, historikkTjenesteAdapter, lagMockYtelseSkjæringstidspunktTjeneste(LocalDate.now()));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        // Act
        new AvklarFortsattMedlemskapOppdaterer(medlemskapTjeneste)
                .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskap = getVurdertLøpendeMedlemskap(behandling.getId(), repositoryProvider);
        assertThat(vurdertMedlemskap).isPresent();
    }

    private Optional<VurdertMedlemskapPeriodeEntitet> getVurdertLøpendeMedlemskap(Long behandlingId,
            BehandlingRepositoryProvider repositoryProvider) {
        MedlemskapRepository medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        return medlemskapRepository.hentVurdertLøpendeMedlemskap(behandlingId);
    }

    private SkjæringstidspunktTjeneste lagMockYtelseSkjæringstidspunktTjeneste(LocalDate fom) {
        var skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(fom).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        return skjæringstidspunktTjeneste;
    }
}
