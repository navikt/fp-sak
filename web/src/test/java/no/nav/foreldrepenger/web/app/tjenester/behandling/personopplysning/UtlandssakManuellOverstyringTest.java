package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus.UTFØRT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktTjeneste;

@CdiDbAwareTest
class UtlandssakManuellOverstyringTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private AksjonspunktTjeneste applikasjonstjeneste;

    @Test
    void spesial_tilfelle_utland_markering() {
        // Arrange trinn 1: Behandle søknad om fødsel hvor barn ikke er registrert i TPS
        var fødselsdato = LocalDate.now().minusDays(15); // > 14 dager for å unngå ApDef.VENT_PÅ_FØDSEL
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AUTOMATISK_MARKERING_AV_UTENLANDSSAK, BehandlingStegType.INNHENT_SØKNADOPP);
        scenario.leggTilAksjonspunkt(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.medBehandlingStegStart(BehandlingStegType.BEREGN_YTELSE);

        var behandling = scenario.lagre(repositoryProvider);

        behandling.getAksjonspunktMedDefinisjonOptional(AUTOMATISK_MARKERING_AV_UTENLANDSSAK)
                .ifPresent(a -> AksjonspunktTestSupport.setTilUtført(a, "test"));
        behandling.getAksjonspunktMedDefinisjonOptional(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET)
                .ifPresent(a -> AksjonspunktTestSupport.setTilUtført(a, "test"));
        behandling.getAksjonspunktMedDefinisjonOptional(SJEKK_MANGLENDE_FØDSEL).ifPresent(a -> AksjonspunktTestSupport.setTilUtført(a, "test"));
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var behandlingId = behandling.getId();

        var fødselDto = new OverstyringUtenlandssakMarkeringDto("NASJONAL", "BOSATT_UTLAND");

        // Act
        applikasjonstjeneste.overstyrAksjonspunkter(singletonList(fødselDto), behandlingId);

        // Assert
        behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.getAksjonspunktMedDefinisjonOptional(SJEKK_MANGLENDE_FØDSEL).orElseThrow().getStatus()).isEqualTo(UTFØRT);
        assertThat(behandling.getAksjonspunktMedDefinisjonOptional(AUTOMATISK_MARKERING_AV_UTENLANDSSAK).orElseThrow().getStatus()).isEqualTo(UTFØRT);
        assertThat(behandling.getAksjonspunktMedDefinisjonOptional(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET).orElseThrow().getStatus())
                .isEqualTo(UTFØRT);
    }
}
