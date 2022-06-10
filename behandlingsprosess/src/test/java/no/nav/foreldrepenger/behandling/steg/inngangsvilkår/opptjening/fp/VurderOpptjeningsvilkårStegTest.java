package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import java.time.LocalDate;

import javax.inject.Inject;

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
import no.nav.foreldrepenger.domene.abakus.AbakusInntektArbeidYtelseTjeneste;

@CdiDbAwareTest
public class VurderOpptjeningsvilkårStegTest {
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    public InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;
    @Inject
    public AbakusInntektArbeidYtelseTjeneste abakusInntektArbeidYtelseTjeneste;

    private final LocalDate idag = LocalDate.now();

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_lagre_resultat_av_opptjeningsvilkår() {

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medDefaultSøknadTerminbekreftelse();

        scenario.leggTilVilkår(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårUtfallType.IKKE_VURDERT);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_VURDERT);

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(idag)
                .build();

        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);

        var behandling = lagre(scenario);
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act
        // opprett opptjening
        new FastsettOpptjeningsperiodeSteg(repositoryProvider, inngangsvilkårFellesTjeneste)
                .utførSteg(kontekst);

        // vurder vilkåret
        new VurderOpptjeningsvilkårSteg(repositoryProvider, inngangsvilkårFellesTjeneste, abakusInntektArbeidYtelseTjeneste)
                .utførSteg(kontekst);
    }
}
