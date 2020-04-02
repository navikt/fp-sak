package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class VurderOpptjeningsvilkårStegTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    public InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;
    
    @Inject
    private OpptjeningRepository opptjeningRepository;
    
    @Inject
    private BehandlingRepository behandlingRepository;

    private final LocalDate idag = LocalDate.now();

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_lagre_resultat_av_opptjeningsvilkår() throws Exception {

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medDefaultSøknadTerminbekreftelse();

        scenario.leggTilVilkår(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårUtfallType.IKKE_VURDERT);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_VURDERT);

        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(idag)
            .build();

        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);

        Behandling behandling = lagre(scenario);
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),behandlingRepository.taSkriveLås(behandling));

        // Act
        // opprett opptjening
        new FastsettOpptjeningsperiodeSteg(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste)
                .utførSteg(kontekst);

        // vurder vilkåret
        new VurderOpptjeningsvilkårSteg(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste)
                .utførSteg(kontekst);
    }
}
