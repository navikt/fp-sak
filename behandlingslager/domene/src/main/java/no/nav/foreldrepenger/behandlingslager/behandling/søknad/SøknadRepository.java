package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class SøknadRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    protected SøknadRepository() {
    }

    @Inject
    public SøknadRepository( EntityManager entityManager, BehandlingRepository behandlingRepository) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
    }

    public SøknadEntitet hentSøknad(Long behandlingId) {
        if (behandlingId == null) {
            return null;
        }
        var query = entityManager.createQuery("FROM SøknadGrunnlag s " + "WHERE s.behandling.id = :behandlingId AND s.aktiv = true",
            SøknadGrunnlagEntitet.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query).map(SøknadGrunnlagEntitet::getSøknad)
                .orElseGet(() -> {
                    var originalId = behandlingRepository.finnUnikBehandlingForBehandlingId(behandlingId)
                        .flatMap(Behandling::getOriginalBehandlingId);
                    return originalId.map(this::hentSøknad).orElse(null);
                });
    }

    public Optional<SøknadEntitet> hentSøknadHvisEksisterer(Long behandlingId) {
        try {
            return Optional.ofNullable(hentSøknad(behandlingId));
        } catch (NoResultException ignore) {
            return Optional.empty();
        }
    }

    public Optional<SøknadEntitet> hentSøknadFraGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId).map(SøknadGrunnlagEntitet::getSøknad);
    }

    public void lagreOgFlush(Behandling behandling, SøknadEntitet søknad) {
        Objects.requireNonNull(behandling, "behandling");
        var søknadGrunnlagEntitet = hentEksisterendeGrunnlag(behandling.getId());
        if (søknadGrunnlagEntitet.isPresent()) {
            // deaktiver eksisterende grunnlag

            var søknadGrunnlagEntitet1 = søknadGrunnlagEntitet.get();
            søknadGrunnlagEntitet1.setAktiv(false);
            entityManager.persist(søknadGrunnlagEntitet1);
            entityManager.flush();
        }

        var grunnlagEntitet = new SøknadGrunnlagEntitet(behandling, søknad);
        entityManager.persist(søknad);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<SøknadGrunnlagEntitet> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery("FROM SøknadGrunnlag s " + "WHERE s.behandling.id = :behandlingId AND s.aktiv = true",
            SøknadGrunnlagEntitet.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling gammelBehandling, Behandling nyBehandling) {
        var søknadEntitet = hentSøknadHvisEksisterer(gammelBehandling.getId());
        søknadEntitet.ifPresent(entitet -> lagreOgFlush(nyBehandling, entitet));
    }

    public List<SøknadEntitet> hentAbsoluttAlleSøknaderForFagsak(Long fagsakId) {
        return behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId).stream()
            .map(Behandling::getId)
            .map(this::hentSøknad)
            .toList();
    }
}
