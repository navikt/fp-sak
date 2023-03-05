package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(JpaExtension.class)
class OpptjeningIUtlandDokStatusTjenesteTest {

    private OpptjeningIUtlandDokStatusTjeneste tjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        tjeneste = new OpptjeningIUtlandDokStatusTjeneste(new OpptjeningIUtlandDokStatusRepository(entityManager));
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skalLagreOgHente() {
        var behandling = opprettBehandling();
        var status = OpptjeningIUtlandDokStatus.DOKUMENTASJON_VIL_BLI_INNHENTET;
        tjeneste.lagreStatus(behandling.getId(), status);

        var lagret = tjeneste.hentStatus(behandling.getId());
        assertThat(lagret).isPresent();
        assertThat(lagret.get()).isEqualTo(status);
    }

    @Test
    void skalDeaktivereStatus() {
        var behandling = opprettBehandling();
        var status = OpptjeningIUtlandDokStatus.DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET;
        tjeneste.lagreStatus(behandling.getId(), status);
        tjeneste.deaktiverStatus(behandling.getId());

        var lagret = tjeneste.hentStatus(behandling.getId());
        assertThat(lagret).isNotPresent();
    }

    @Test
    void skalDeaktivereStatusPåBehandlingUtenStatus() {
        var behandling = opprettBehandling();
        tjeneste.deaktiverStatus(behandling.getId());

        var lagret = tjeneste.hentStatus(behandling.getId());
        assertThat(lagret).isNotPresent();
    }

    private Behandling opprettBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));

        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
        return behandling;
    }
}
