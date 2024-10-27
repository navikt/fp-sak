package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

@ApplicationScoped
public class KlageRepository {

    private EntityManager entityManager;

    protected KlageRepository() {
        // for CDI proxy
    }

    @Inject
    public KlageRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public KlageResultatEntitet hentEvtOpprettKlageResultat(Long behandlingId) {
        return hentKlageResultatHvisEksisterer(behandlingId).orElseGet(() -> leggTilKlageResultat(behandlingId));
    }

    public Optional<KlageResultatEntitet> hentKlageResultatHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");

        var query = entityManager.createQuery(" FROM KlageResultat WHERE klageBehandlingId = :behandlingId", KlageResultatEntitet.class);

        query.setParameter("behandlingId", behandlingId);
        return hentUniktResultat(query);
    }

    private List<KlageVurderingResultat> hentVurderingsResultaterForKlageBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");

        var query = entityManager.createQuery(" FROM KlageVurderingResultat WHERE klageResultat.klageBehandlingId = :behandlingId",
            KlageVurderingResultat.class);

        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    private List<KlageFormkravEntitet> hentKlageFormkravForKlageBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");

        var query = entityManager.createQuery(" FROM KlageFormkrav WHERE klageResultat.klageBehandlingId = :behandlingId",
            KlageFormkravEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    private KlageResultatEntitet leggTilKlageResultat(Long klageBehandlingId) {
        var resultatEntitet = KlageResultatEntitet.builder().medKlageBehandlingId(klageBehandlingId).build();
        entityManager.persist(resultatEntitet);
        entityManager.flush();
        return resultatEntitet;
    }

    public void settPåklagdBehandlingId(Long klageBehandlingId, Long påKlagdBehandlingId) {
        var klageResultat = hentEvtOpprettKlageResultat(klageBehandlingId);
        klageResultat.settPåKlagdBehandlingId(påKlagdBehandlingId);
        klageResultat.settPåKlagdEksternBehandlingUuid(null);

        entityManager.persist(klageResultat);
        entityManager.flush();
    }

    public void settPåklagdEksternBehandlingUuid(Long klageBehandlingId, UUID påKlagdEksternBehandlingUuid) {
        var klageResultat = hentEvtOpprettKlageResultat(klageBehandlingId);
        klageResultat.settPåKlagdEksternBehandlingUuid(påKlagdEksternBehandlingUuid);
        klageResultat.settPåKlagdBehandlingId(null);

        entityManager.persist(klageResultat);
        entityManager.flush();
    }

    public void settKabalReferanse(Long klageBehandlingId, String kabalReferanse) {
        var klageResultat = hentEvtOpprettKlageResultat(klageBehandlingId);
        if (Objects.equals(kabalReferanse, klageResultat.getKabalReferanse())) {
            return;
        }
        klageResultat.setKabalReferanse(kabalReferanse);
        entityManager.persist(klageResultat);
        entityManager.flush();
    }

    public Optional<KlageFormkravEntitet> hentGjeldendeKlageFormkrav(Long behandlingId) {
        var klageFormkravListe = hentKlageFormkravForKlageBehandling(behandlingId);

        var gjeldende = klageFormkravListe.stream()
            .filter(kf -> KlageVurdertAv.NK.equals(kf.getKlageVurdertAv()))
            .findFirst().orElseGet(() -> klageFormkravListe.stream().findFirst().orElse(null));

        return Optional.ofNullable(gjeldende);
    }

    public Optional<KlageFormkravEntitet> hentKlageFormkrav(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
        var klageFormkravList = hentKlageFormkravForKlageBehandling(klageBehandlingId);
        return klageFormkravList.stream()
            .filter(kf -> klageVurdertAv.equals(kf.getKlageVurdertAv()))
            .findFirst();
    }

    public Optional<KlageVurderingResultat> hentGjeldendeKlageVurderingResultat(Behandling behandling) {
        var klageVurderingResultat = hentVurderingsResultaterForKlageBehandling(behandling.getId());

        return klageVurderingResultat.stream()
            .filter(kvr -> KlageVurdertAv.NK.equals(kvr.getKlageVurdertAv()))
            .findFirst()
            .or(() -> klageVurderingResultat.stream().findFirst());
    }

    public void lagreFormkrav(Behandling klageBehandling, KlageFormkravEntitet.Builder klageFormkravBuilder) {
        klageFormkravBuilder.medKlageResultat(hentEvtOpprettKlageResultat(klageBehandling.getId()));
        var nyKlageFormkravEntitet = klageFormkravBuilder.build();
        hentKlageFormkrav(klageBehandling.getId(), nyKlageFormkravEntitet.getKlageVurdertAv()).ifPresent(entityManager::remove);
        entityManager.persist(nyKlageFormkravEntitet);
        entityManager.flush();
    }

    public void lagreVurderingsResultat(Behandling klageBehandling, KlageVurderingResultat.Builder klageVurderingResultatBuilder) {
        klageVurderingResultatBuilder.medKlageResultat(hentEvtOpprettKlageResultat(klageBehandling.getId()));
        var nyKlageVurderingResultat = klageVurderingResultatBuilder.build();
        hentKlageVurderingResultat(klageBehandling.getId(), nyKlageVurderingResultat.getKlageVurdertAv()).ifPresent(entityManager::remove);
        entityManager.persist(nyKlageVurderingResultat);
        entityManager.flush();
    }

    public void lagreVurderingsResultat(Long klageBehandlingId, KlageVurderingResultat klageVurderingResultat) {
                hentKlageVurderingResultat(klageBehandlingId, klageVurderingResultat.getKlageVurdertAv()).ifPresent(entityManager::remove);
        entityManager.persist(klageVurderingResultat);
        entityManager.flush();
    }

    public Optional<KlageVurderingResultat> hentKlageVurderingResultat(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
        var klageVurderingResultatList = hentVurderingsResultaterForKlageBehandling(klageBehandlingId);
        return klageVurderingResultatList.stream()
            .filter(krv -> klageVurdertAv.equals(krv.getKlageVurdertAv()))
            .findFirst();
    }

}
