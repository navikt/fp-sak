package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class VurderOpptjeningsvilkårStegTest {
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    public InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_lagre_resultat_av_opptjeningsvilkår() {

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medDefaultSøknadTerminbekreftelse();

        scenario.leggTilVilkår(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårUtfallType.IKKE_VURDERT);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_VURDERT);

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(LocalDate.now())
                .build();

        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);

        var behandling = lagre(scenario);
        var kontekst = new BehandlingskontrollKontekst(behandling,
                repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act
        // opprett opptjening
        new FastsettOpptjeningsperiodeSteg(repositoryProvider, inngangsvilkårFellesTjeneste)
                .utførSteg(kontekst);

        // vurder vilkåret
        new VurderOpptjeningsvilkårSteg(repositoryProvider, inngangsvilkårFellesTjeneste)
                .utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.OPPFYLT);

    }
}
