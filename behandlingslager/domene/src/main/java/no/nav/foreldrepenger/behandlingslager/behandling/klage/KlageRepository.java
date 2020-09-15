package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

@ApplicationScoped
public class KlageRepository {

    private EntityManager entityManager;

    protected KlageRepository() {
        // for CDI proxy
    }

    @Inject
    public KlageRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public KlageResultatEntitet hentEvtOpprettKlageResultat(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageResultatEntitet> query = entityManager.createQuery(
            " FROM KlageResultat WHERE klageBehandlingId = :behandlingId", //$NON-NLS-1$
            KlageResultatEntitet.class);// NOSONAR

        query.setParameter("behandlingId", behandlingId);
        return hentUniktResultat(query).orElseGet(() -> leggTilKlageResultat(behandlingId));
    }

    private List<KlageVurderingResultat> hentVurderingsResultaterForKlageBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageVurderingResultat> query = entityManager.createQuery(
            " FROM KlageVurderingResultat WHERE klageResultat.klageBehandlingId = :behandlingId", //$NON-NLS-1$
            KlageVurderingResultat.class);// NOSONAR

        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    private List<KlageFormkravEntitet> hentKlageFormkravForKlageBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageFormkravEntitet> query = entityManager.createQuery(
            " FROM KlageFormkrav WHERE klageResultat.klageBehandlingId = :behandlingId", //$NON-NLS-1$
            KlageFormkravEntitet.class);// NOSONAR
        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    private KlageResultatEntitet leggTilKlageResultat(Long klageBehandlingId) {
        KlageResultatEntitet resultatEntitet = KlageResultatEntitet.builder().medKlageBehandlingId(klageBehandlingId).build();
        entityManager.persist(resultatEntitet);
        entityManager.flush();
        return resultatEntitet;
    }

    public void settPåklagdBehandlingId(Long klageBehandlingId, Long påKlagdBehandlingId) {
        KlageResultatEntitet klageResultat = hentEvtOpprettKlageResultat(klageBehandlingId);
        klageResultat.settPåKlagdBehandlingId(påKlagdBehandlingId);
        klageResultat.settPåKlagdEksternBehandlingUuid(null);

        entityManager.persist(klageResultat);
        entityManager.flush();
    }

    public void settPåklagdEksternBehandlingUuid(Long klageBehandlingId, UUID påKlagdEksternBehandlingUuid) {
        KlageResultatEntitet klageResultat = hentEvtOpprettKlageResultat(klageBehandlingId);
        klageResultat.settPåKlagdEksternBehandlingUuid(påKlagdEksternBehandlingUuid);
        klageResultat.settPåKlagdBehandlingId(null);

        entityManager.persist(klageResultat);
        entityManager.flush();
    }

    public Optional<KlageFormkravEntitet> hentGjeldendeKlageFormkrav(Long behandlingId) {
        List<KlageFormkravEntitet> klageFormkravListe = hentKlageFormkravForKlageBehandling(behandlingId);

        var gjeldende = klageFormkravListe.stream()
            .filter(kf -> KlageVurdertAv.NK.equals(kf.getKlageVurdertAv()))
            .findFirst().orElseGet(() -> klageFormkravListe.stream().findFirst().orElse(null));

        return Optional.ofNullable(gjeldende);
    }

    public Optional<KlageFormkravEntitet> hentKlageFormkrav(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
        List<KlageFormkravEntitet> klageFormkravList = hentKlageFormkravForKlageBehandling(klageBehandlingId);
        return klageFormkravList.stream()
            .filter(kf -> klageVurdertAv.equals(kf.getKlageVurdertAv()))
            .findFirst();
    }

    public Optional<KlageVurderingResultat> hentGjeldendeKlageVurderingResultat(Behandling behandling) {
        List<KlageVurderingResultat> klageVurderingResultat = hentVurderingsResultaterForKlageBehandling(behandling.getId());

        var resultat = klageVurderingResultat.stream()
            .filter(kvr -> KlageVurdertAv.NK.equals(kvr.getKlageVurdertAv()))
            .findFirst()
            .orElseGet(() -> klageVurderingResultat.stream().findFirst().orElse(null));

        return Optional.ofNullable(resultat);
    }

    public void lagreFormkrav(Behandling klageBehandling, KlageFormkravEntitet.Builder klageFormkravBuilder) {
        klageFormkravBuilder.medKlageResultat(hentEvtOpprettKlageResultat(klageBehandling.getId()));
        KlageFormkravEntitet nyKlageFormkravEntitet = klageFormkravBuilder.build();
        hentKlageFormkrav(klageBehandling.getId(), nyKlageFormkravEntitet.getKlageVurdertAv()).ifPresent(entityManager::remove);
        entityManager.persist(nyKlageFormkravEntitet);
        entityManager.flush();
    }

    public Long lagreVurderingsResultat(Behandling klageBehandling, KlageVurderingResultat.Builder klageVurderingResultatBuilder) {
        klageVurderingResultatBuilder.medKlageResultat(hentEvtOpprettKlageResultat(klageBehandling.getId()));
        KlageVurderingResultat nyKlageVurderingResultat = klageVurderingResultatBuilder.build();
        hentKlageVurderingResultat(klageBehandling.getId(), nyKlageVurderingResultat.getKlageVurdertAv()).ifPresent(entityManager::remove);
        entityManager.persist(nyKlageVurderingResultat);
        entityManager.flush();
        return nyKlageVurderingResultat.getId();
    }

    public Long lagreVurderingsResultat(Long klageBehandlingId, KlageVurderingResultat klageVurderingResultat) {
                hentKlageVurderingResultat(klageBehandlingId, klageVurderingResultat.getKlageVurdertAv()).ifPresent(entityManager::remove);
        entityManager.persist(klageVurderingResultat);
        entityManager.flush();
        return klageVurderingResultat.getId();
    }

    public Optional<KlageVurderingResultat> hentKlageVurderingResultat(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
        List<KlageVurderingResultat> klageVurderingResultatList = hentVurderingsResultaterForKlageBehandling(klageBehandlingId);
        return klageVurderingResultatList.stream()
            .filter(krv -> klageVurdertAv.equals(krv.getKlageVurdertAv()))
            .findFirst();
    }

    public void settKlageGodkjentHosMedunderskriver(Long klageBehandlingId, KlageVurdertAv vurdertAv, boolean vurdering) {
        hentKlageVurderingResultat(klageBehandlingId, vurdertAv).ifPresent(kvr -> {
            kvr.setGodkjentAvMedunderskriver(vurdering);
            entityManager.persist(kvr);
            entityManager.flush();
        });
    }

}
