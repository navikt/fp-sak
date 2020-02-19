package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;


@ApplicationScoped
public class OpptjeningIUtlandDokStatusTjeneste {

    private OpptjeningIUtlandDokStatusRepository repository;

    @Inject
    public OpptjeningIUtlandDokStatusTjeneste(OpptjeningIUtlandDokStatusRepository repository) {
        this.repository = repository;
    }

    OpptjeningIUtlandDokStatusTjeneste() {
        //CDI
    }

    public void lagreStatus(Long behandlingId, OpptjeningIUtlandDokStatus status) {
        repository.lagre(new OpptjeningIUtlandDokStatusEntitet(behandlingId, status));
    }

    public Optional<OpptjeningIUtlandDokStatus> hentStatus(Long behandlingId) {
        var entitet = repository.hent(behandlingId);
        return entitet.map(OpptjeningIUtlandDokStatusEntitet::getDokStatus);
    }

    public void deaktiverStatus(Long behandlingId) {
        repository.deaktiverStatus(behandlingId);
    }
}
