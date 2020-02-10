package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringSokersOpplysingspliktDto;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class SøkersopplysningspliktOverstyringhåndtererTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_søkers_opplysningsplikt_overstyrt() {
        // Arrange
        // Behandling
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        Behandling behandling = scenario.getBehandling();
        // Dto
        OverstyringSokersOpplysingspliktDto overstyringspunktDto = new OverstyringSokersOpplysingspliktDto(false,
            "test av overstyring");
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(overstyringspunktDto), behandling.getId());

        // Assert
        List<Historikkinnslag> historikkinnslagene = repositoryProvider.getHistorikkRepository().hentHistorikk(behandling.getId());
        assertThat(historikkinnslagene.get(0).getHistorikkinnslagDeler()).hasSize(1);
        List<HistorikkinnslagFelt> feltList = historikkinnslagene.get(0).getHistorikkinnslagDeler().get(0).getEndredeFelt();
        assertThat(feltList).hasSize(1);
        HistorikkinnslagFelt felt = feltList.get(0);
        assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT.getKode());
        assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
        assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo(HistorikkEndretFeltVerdiType.IKKE_OPPFYLT.getKode());

        Set<Aksjonspunkt> aksjonspunktSet = behandling.getAksjonspunkter();

        assertThat(aksjonspunktSet).extracting("aksjonspunktDefinisjon").contains(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST);

        assertThat(aksjonspunktSet.stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST)))
            .anySatisfy(ap -> assertThat(ap.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT));

        assertThat(aksjonspunktSet.stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL)))
            .anySatisfy(ap -> assertThat(ap.getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET));

        assertThat(aksjonspunktSet).hasSize(3);
    }

}
