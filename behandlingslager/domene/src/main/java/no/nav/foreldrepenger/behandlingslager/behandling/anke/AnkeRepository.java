package no.nav.foreldrepenger.behandlingslager.behandling.anke;


import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class AnkeRepository {

    private EntityManager entityManager;

    protected AnkeRepository() {
        // for CDI proxy
    }

    @Inject
    public AnkeRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public AnkeResultatEntitet hentEllerOpprettAnkeResultat(Long ankeBehandlingId) {
        return hentAnkeResultat(ankeBehandlingId).orElseGet(() -> leggTilAnkeResultat(ankeBehandlingId));
    }

    public Optional<AnkeResultatEntitet> hentAnkeResultat(Long ankeBehandlingId) {
        Objects.requireNonNull(ankeBehandlingId, "behandlingId");

        var query = entityManager.createQuery(" FROM AnkeResultat WHERE ankeBehandlingId = :behandlingId", AnkeResultatEntitet.class);
        query.setParameter("behandlingId", ankeBehandlingId);
        return hentUniktResultat(query);
    }

    private Optional<AnkeVurderingResultatEntitet> hentVurderingsResultaterForAnkeBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        var query = entityManager.createQuery(" FROM AnkeVurderingResultat WHERE ankeResultat.ankeBehandlingId = :behandlingId",
            AnkeVurderingResultatEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private AnkeResultatEntitet leggTilAnkeResultat(Long ankeBehandlingId) {
        var nyttResultat = AnkeResultatEntitet.builder().medAnkeBehandlingId(ankeBehandlingId).build();
        entityManager.persist(nyttResultat);
        entityManager.flush();
        return nyttResultat;
    }

    public void settPåAnketKlageBehandling(Long ankeBehandlingId, Long påAnketKlageBehandlingId) {
        var ankeResultat = hentEllerOpprettAnkeResultat(ankeBehandlingId);
        if (Objects.equals(påAnketKlageBehandlingId, ankeResultat.getPåAnketKlageBehandlingId().orElse(null))) {
            return;
        }
        ankeResultat.settPåAnketKlageBehandling(påAnketKlageBehandlingId);
        entityManager.persist(ankeResultat);
        entityManager.flush();
    }

    public void settKabalReferanse(Long ankeBehandlingId, String kabalReferanse) {
        var ankeResultat = hentEllerOpprettAnkeResultat(ankeBehandlingId);
        if (Objects.equals(kabalReferanse, ankeResultat.getKabalReferanse())) {
            return;
        }
        ankeResultat.setKabalReferanse(kabalReferanse);
        entityManager.persist(ankeResultat);
        entityManager.flush();
    }

    public void lagreVurderingsResultat(Long ankeBehandlingId, AnkeVurderingResultatEntitet ankeVurderingResultat) {
        hentAnkeVurderingResultat(ankeBehandlingId).ifPresent(entityManager::remove);
        entityManager.persist(ankeVurderingResultat);
        entityManager.flush();
    }

    public Optional<AnkeVurderingResultatEntitet> hentAnkeVurderingResultat(Long ankeBehandlingId) {
        return hentVurderingsResultaterForAnkeBehandling(ankeBehandlingId);
    }

}
