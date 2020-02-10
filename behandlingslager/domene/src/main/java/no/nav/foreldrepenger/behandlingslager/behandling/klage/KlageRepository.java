package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class KlageRepository {

    private EntityManager entityManager;

    protected KlageRepository() {
        // for CDI proxy
    }

    @Inject
    public KlageRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public KlageResultatEntitet hentKlageResultat(Behandling klageBehandling) {
        Long behandlingId = klageBehandling.getId();
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageResultatEntitet> query = entityManager.createQuery(
            " FROM KlageResultat " +
                "   WHERE klageBehandling.id = :behandlingId", //$NON-NLS-1$
            KlageResultatEntitet.class);// NOSONAR

        query.setParameter("behandlingId", behandlingId);
        return query.getSingleResult();
    }

    private List<KlageVurderingResultat> hentVurderingsResultaterForKlageBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageVurderingResultat> query = entityManager.createQuery(
            " FROM KlageVurderingResultat" +
                "        WHERE klageResultat = (FROM KlageResultat " +
                "        WHERE klageBehandling.id = :behandlingId)", //$NON-NLS-1$
            KlageVurderingResultat.class);// NOSONAR

        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    private List<KlageFormkravEntitet> hentKlageFormkravForKlageBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageFormkravEntitet> query = entityManager.createQuery(
            " FROM KlageFormkrav" +
                "        WHERE klageResultat = (FROM KlageResultat" +
                "        WHERE klageBehandling.id = :behandlingId)", //$NON-NLS-1$
            KlageFormkravEntitet.class);// NOSONAR
        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    public void leggTilKlageResultat(Behandling klageBehandling) {
        entityManager.persist(KlageResultatEntitet.builder().medKlageBehandling(klageBehandling).build());
        entityManager.flush();
    }

    public void settPåklagdBehandling(Behandling klageBehandling, Behandling påKlagdBehandling) {
        KlageResultatEntitet klageResultat = hentKlageResultat(klageBehandling);
        klageResultat.settPåKlagdBehandling(påKlagdBehandling);
        entityManager.persist(klageResultat);
        entityManager.flush();
    }

    public Optional<KlageFormkravEntitet> hentGjeldendeKlageFormkrav(Behandling behandling) {
        List<KlageFormkravEntitet> klageFormkravListe = hentKlageFormkravForKlageBehandling(behandling.getId());

        Optional<KlageFormkravEntitet> klageFormkravNK = klageFormkravListe.stream()
            .filter(kf -> KlageVurdertAv.NK.equals(kf.getKlageVurdertAv()))
            .findFirst();

        Optional<KlageFormkravEntitet> klageFormkravNFP = klageFormkravListe.stream()
            .filter(kf -> KlageVurdertAv.NFP.equals(kf.getKlageVurdertAv()))
            .findFirst();

        if (klageFormkravNK.isPresent()) {
            return klageFormkravNK;
        }
        return klageFormkravNFP;
    }

    private List<KlageFormkravEntitet> hentKlageFormkravEntiteterForKlageBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageFormkravEntitet> query = entityManager.createQuery(
            " FROM KlageFormkrav" +
                "        WHERE klageResultat = (FROM KlageResultat" +
                "        WHERE klageBehandling.id = :behandlingId)", //$NON-NLS-1$
            KlageFormkravEntitet.class);// NOSONAR
        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    private Optional<KlageFormkravEntitet> hentKlageFormkravEntitet(Behandling klageBehandling, KlageVurdertAv klageVurdertAv) {
        List<KlageFormkravEntitet> klageFormkravList = hentKlageFormkravEntiteterForKlageBehandling(klageBehandling.getId());
        return klageFormkravList.stream()
            .filter(kf -> klageVurdertAv.equals(kf.getKlageVurdertAv()))
            .findFirst();
    }

    public Optional<KlageVurderingResultat> hentGjeldendeKlageVurderingResultat(Behandling behandling) {

        List<KlageVurderingResultat> klageVurderingResultat = hentVurderingsResultaterForKlageBehandling(behandling.getId());

        Optional<KlageVurderingResultat> klageVurderingResultatNK = klageVurderingResultat.stream()
            .filter(kvr -> KlageVurdertAv.NK.equals(kvr.getKlageVurdertAv()))
            .findFirst();

        Optional<KlageVurderingResultat> klageVurderingResultatNFP = klageVurderingResultat.stream()
            .filter(krv -> KlageVurdertAv.NFP.equals(krv.getKlageVurdertAv()))
            .findFirst();

        if (klageVurderingResultatNK.isPresent()) {
            return klageVurderingResultatNK;
        }
        return klageVurderingResultatNFP;
    }

    public void lagreFormkrav(Behandling klageBehandling, KlageFormkravEntitet.Builder klageFormkravBuilder) {
        klageFormkravBuilder.medKlageResultat(hentKlageResultat(klageBehandling));
        KlageFormkravEntitet nyKlageFormkravEntitet = klageFormkravBuilder.build();
        Optional<KlageFormkravEntitet> optionalGammelFormkrav = hentKlageFormkravEntitet(klageBehandling, nyKlageFormkravEntitet.getKlageVurdertAv());
        if (optionalGammelFormkrav.isPresent()) {
            entityManager.remove(optionalGammelFormkrav.get());
        }
        entityManager.persist(nyKlageFormkravEntitet);
        entityManager.flush();
    }

    public Long lagreVurderingsResultat(Behandling klageBehandling, KlageVurderingResultat.Builder klageVurderingResultatBuilder) {
        klageVurderingResultatBuilder.medKlageResultat(hentKlageResultat(klageBehandling));
        KlageVurderingResultat nyKlageVurderingResultat = klageVurderingResultatBuilder.build();
        Optional<KlageVurderingResultat> optionalGammelVR = hentKlageVurderingResultat(klageBehandling.getId(), nyKlageVurderingResultat.getKlageVurdertAv());
        if (optionalGammelVR.isPresent()) {
            entityManager.remove(optionalGammelVR.get());
        }
        entityManager.persist(nyKlageVurderingResultat);
        entityManager.flush();
        return nyKlageVurderingResultat.getId();
    }

    public Optional<KlageVurderingResultat> hentKlageVurderingResultat(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
        List<KlageVurderingResultat> klageVurderingResultatList = hentVurderingsResultaterForKlageBehandling(klageBehandlingId);
        return klageVurderingResultatList.stream()
            .filter(krv -> klageVurdertAv.equals(krv.getKlageVurdertAv()))
            .findFirst();
    }

    public Optional<KlageFormkravEntitet> hentKlageFormkrav(Behandling klageBehandling, KlageVurdertAv klageVurdertAv) {
        List<KlageFormkravEntitet> klageFormkravList = hentKlageFormkravForKlageBehandling(klageBehandling.getId());
        return klageFormkravList.stream()
            .filter(kf -> klageVurdertAv.equals(kf.getKlageVurdertAv()))
            .findFirst();
    }

    public void slettKlageVurderingResultat(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {

        Optional<KlageVurderingResultat> klageVurderingResultatOptional = hentKlageVurderingResultat(klageBehandlingId, klageVurdertAv);
        if (!klageVurderingResultatOptional.isPresent()) {
            return;
        }
        entityManager.remove(klageVurderingResultatOptional.get());
        entityManager.flush();
    }

    public void slettFormkrav(Behandling behandling, KlageVurdertAv klageVurdertAv) {
        Optional<KlageFormkravEntitet> klageFormkrav = hentKlageFormkrav(behandling, klageVurdertAv);
        if (!klageFormkrav.isPresent()) {
            return;
        }
        entityManager.remove(klageFormkrav.get());
        entityManager.flush();
    }

}
