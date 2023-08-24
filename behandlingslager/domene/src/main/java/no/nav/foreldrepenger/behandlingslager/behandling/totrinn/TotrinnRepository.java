package no.nav.foreldrepenger.behandlingslager.behandling.totrinn;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class TotrinnRepository {

    private EntityManager entityManager;

    TotrinnRepository() {
        // CDI
    }

    @Inject
    public TotrinnRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public void lagreOgFlush(Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        var behandlingId = totrinnresultatgrunnlag.getBehandlingId();
        var aktivtTotrinnresultatgrunnlag = getAktivtTotrinnresultatgrunnlag(behandlingId);
        if (aktivtTotrinnresultatgrunnlag.isPresent()) {
            var grunnlag = aktivtTotrinnresultatgrunnlag.get();
            grunnlag.setAktiv(false);
            entityManager.persist(grunnlag);
        }
        lagreTotrinnsresultatgrunnlag(totrinnresultatgrunnlag);
        entityManager.flush();
    }

    public void lagreOgFlush(Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        var behandlingIds = totrinnaksjonspunktvurderinger.stream().map(Totrinnsvurdering::getBehandlingId).collect(Collectors.toSet());
        if (behandlingIds.size() > 1) {
            throw new IllegalArgumentException("Alle totrinnsvurderinger må ha samme behandling. Fant " + behandlingIds);
        }

        var behandlingId = behandlingIds.stream().findFirst().orElseThrow();
        var aktiveVurderinger = getAktiveTotrinnaksjonspunktvurderinger(behandlingId);
        if (!aktiveVurderinger.isEmpty()) {
            aktiveVurderinger.forEach(vurdering -> {
                vurdering.setAktiv(false);
                entityManager.persist(vurdering);
            });
        }
        totrinnaksjonspunktvurderinger.forEach(this::lagreTotrinnaksjonspunktvurdering);
        entityManager.flush();
    }


    public Optional<Totrinnresultatgrunnlag> hentTotrinngrunnlag(Long behandlingId) {
        return getAktivtTotrinnresultatgrunnlag(behandlingId);
    }

    public Collection<Totrinnsvurdering> hentTotrinnaksjonspunktvurderinger(Long behandlingId) {
        return getAktiveTotrinnaksjonspunktvurderinger(behandlingId);
    }

    private void lagreTotrinnsresultatgrunnlag(Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        entityManager.persist(totrinnresultatgrunnlag);
    }

    private void lagreTotrinnaksjonspunktvurdering(Totrinnsvurdering totrinnsvurdering) {
        entityManager.persist(totrinnsvurdering);
    }

    private Optional<Totrinnresultatgrunnlag> getAktivtTotrinnresultatgrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT trg FROM Totrinnresultatgrunnlag trg WHERE trg.behandling.id = :behandling_id AND trg.aktiv = 'J'",
            Totrinnresultatgrunnlag.class);

        query.setParameter("behandling_id", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Collection<Totrinnsvurdering> getAktiveTotrinnaksjonspunktvurderinger(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT tav FROM Totrinnsvurdering tav WHERE tav.behandling.id = :behandling_id AND tav.aktiv = 'J'",
            Totrinnsvurdering.class);

        query.setParameter("behandling_id", behandlingId);
        return query.getResultList();
    }
}
