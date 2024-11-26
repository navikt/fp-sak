package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftEktefelleAksjonspunktDto;

@CdiDbAwareTest
class BekreftEktefelleOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_ektefelle() {
        var oppdaterer = new BekreftEktefelleOppdaterer(repositoryProvider.getHistorikkinnslag2Repository(), familieHendelseTjeneste);

        // Arrange
        var oppdatertEktefellesBarn = true;

        // Behandling
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()));

        scenario.medSøknad()
            .medFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        scenario.medBekreftetHendelse().medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(LocalDate.now())
            .medAdoptererAlene(true));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        var dto = new BekreftEktefelleAksjonspunktDto("begrunnelse", oppdatertEktefellesBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var oppdateringResultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        assertThat(oppdateringResultat.kreverTotrinnsKontroll()).isTrue();
        var historikkinnslag = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();

        // Assert
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(2);
        assertThat(historikkinnslag.getTekstlinjer().getFirst().getTekst()).doesNotContain("ikke å være");
        assertThat(historikkinnslag.getTekstlinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }

}
