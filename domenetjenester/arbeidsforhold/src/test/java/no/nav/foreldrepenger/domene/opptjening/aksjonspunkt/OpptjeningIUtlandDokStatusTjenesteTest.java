package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class OpptjeningIUtlandDokStatusTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private OpptjeningIUtlandDokStatusTjeneste tjeneste = new OpptjeningIUtlandDokStatusTjeneste(new OpptjeningIUtlandDokStatusRepository(repoRule.getEntityManager()));

    @Test
    public void skalLagreOgHente() {
        var behandling = opprettBehandling();
        var status = OpptjeningIUtlandDokStatus.DOKUMENTASJON_VIL_BLI_INNHENTET;
        tjeneste.lagreStatus(behandling.getId(), status);

        var lagret = tjeneste.hentStatus(behandling.getId());
        assertThat(lagret).isPresent();
        assertThat(lagret.get()).isEqualTo(status);
    }

    @Test
    public void skalDeaktivereStatus() {
        var behandling = opprettBehandling();
        var status = OpptjeningIUtlandDokStatus.DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET;
        tjeneste.lagreStatus(behandling.getId(), status);
        tjeneste.deaktiverStatus(behandling.getId());

        var lagret = tjeneste.hentStatus(behandling.getId());
        assertThat(lagret).isNotPresent();
    }

    @Test
    public void skalDeaktivereStatusPåBehandlingUtenStatus() {
        var behandling = opprettBehandling();
        tjeneste.deaktiverStatus(behandling.getId());

        var lagret = tjeneste.hentStatus(behandling.getId());
        assertThat(lagret).isNotPresent();
    }

    private Behandling opprettBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AktørId.dummy(), Språkkode.NB));
        new FagsakRepository(repoRule.getEntityManager()).opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        new BehandlingRepository(repoRule.getEntityManager()).lagre(behandling, new BehandlingLåsRepository(repoRule.getEntityManager()).taLås(behandling.getId()));
        return behandling;
    }
}
