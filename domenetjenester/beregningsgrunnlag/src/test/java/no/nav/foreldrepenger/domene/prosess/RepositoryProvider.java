package no.nav.foreldrepenger.domene.prosess;

import java.util.Objects;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange injection points.
 * Kun for test, ikke for injection
 */
public class RepositoryProvider {

    private final EntityManager entityManager;

    public RepositoryProvider(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public BehandlingRepository getBehandlingRepository() {
        return new BehandlingRepository(entityManager);
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return new FagsakRepository(entityManager);
    }

    public BeregningsgrunnlagRepository getBeregningsgrunnlagRepository() {
        return new BeregningsgrunnlagRepository(entityManager);
    }

}
