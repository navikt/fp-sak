package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class FaktaUttakDokumentasjonStegTest {

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    private FaktaUttakDokumentasjonSteg steg;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void skal_utlede_ap() {
        var fom = LocalDate.of(2022, 11, 16);
        var utsettelse = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, fom.plusWeeks(2).minusDays(1))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelse), true))
            .medFødselAdopsjonsdato(fom);

        var behandling = scenario.lagre(repositoryProvider);

        var stegResultat = steg.utførSteg(
            new BehandlingskontrollKontekst(behandling.getSaksnummer(), behandling.getFagsakId(), new BehandlingLås(behandling.getId())));

        assertThat(stegResultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON);

    }

}
