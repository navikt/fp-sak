package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.BekreftSokersOpplysningspliktManuDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.BekreftSøkersOpplysningspliktManuellOppdaterer;

@CdiDbAwareTest
class BekreftSøkersOpplysningspliktManuellOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_søkers_opplysningsplikt_manu() {
        // Arrange
        // Behandling
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU,
                BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        var historikkinnslag2Repository = repositoryProvider.getHistorikkinnslag2Repository();
        var oppdaterer = new BekreftSøkersOpplysningspliktManuellOppdaterer(historikkinnslag2Repository, repositoryProvider.getBehandlingRepository());

        // Dto
        var bekreftSokersOpplysningspliktManuDto = new BekreftSokersOpplysningspliktManuDto(
                "test av manu", true, Collections.emptyList());
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(bekreftSokersOpplysningspliktManuDto.getAksjonspunktDefinisjon());
        var resultat = oppdaterer.oppdater(bekreftSokersOpplysningspliktManuDto,
                new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), bekreftSokersOpplysningspliktManuDto, aksjonspunkt));
        var historikkInnslagForBehandling = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId());

        // Assert
        assertThat(historikkInnslagForBehandling).hasSize(1);
        var historikkInnslag = historikkInnslagForBehandling.getFirst();
        var tekstlinjer = historikkInnslag.getTekstlinjer();
        assertThat(tekstlinjer).hasSize(2);
       assertThat(tekstlinjer.getFirst().getTekst()).contains("Søkers opplysningsplikt");
       assertThat(tekstlinjer.getFirst().getTekst()).contains("Vilkåret er oppfylt");
       assertThat(tekstlinjer.getFirst().getTekst()).doesNotContain("ikke oppfylt");
         assertThat(tekstlinjer.get(1).getTekst()).contains("test av manu");

        var aksjonspunktSet = resultat.getEkstraAksjonspunktResultat().stream()
                .map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toSet());

        assertThat(aksjonspunktSet).isEmpty();
    }
}
