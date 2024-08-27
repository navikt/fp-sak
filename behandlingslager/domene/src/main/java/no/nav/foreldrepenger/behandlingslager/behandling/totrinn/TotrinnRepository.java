package no.nav.foreldrepenger.behandlingslager.behandling.totrinn;


import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

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

    public Collection<Totrinnsvurdering> hentTotrinnaksjonspunktvurderinger(Long behandlingId) {
        return getAktiveTotrinnaksjonspunktvurderinger(behandlingId);
    }

    private void lagreTotrinnaksjonspunktvurdering(Totrinnsvurdering totrinnsvurdering) {
        entityManager.persist(totrinnsvurdering);
    }

    private Collection<Totrinnsvurdering> getAktiveTotrinnaksjonspunktvurderinger(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT tav FROM Totrinnsvurdering tav WHERE tav.behandling.id = :behandling_id AND tav.aktiv = true",
            Totrinnsvurdering.class);

        query.setParameter("behandling_id", behandlingId);
        return query.getResultList();
    }
}
