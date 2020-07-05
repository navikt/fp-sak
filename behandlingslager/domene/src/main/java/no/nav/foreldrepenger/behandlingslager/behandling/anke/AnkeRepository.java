package no.nav.foreldrepenger.behandlingslager.behandling.anke;


import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class AnkeRepository {

    private EntityManager entityManager;

    protected AnkeRepository() {
        // for CDI proxy
    }

    @Inject
    public AnkeRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<AnkeResultatEntitet> hentAnkeResultat(Behandling ankeBehandling) {
        Long ankeBehandlingId = ankeBehandling.getId();
        Objects.requireNonNull(ankeBehandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<AnkeResultatEntitet> query = entityManager.createQuery(
            " FROM AnkeResultat " +
                "   WHERE ankeBehandling.id = :behandlingId", AnkeResultatEntitet.class);// NOSONAR //$NON-NLS-1$
        query.setParameter("behandlingId", ankeBehandlingId);
        return Optional.ofNullable(query.getSingleResult());
    }

    private Optional<AnkeVurderingResultatEntitet> hentVurderingsResultaterForAnkeBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        final TypedQuery<AnkeVurderingResultatEntitet> query = entityManager.createQuery(
            " FROM AnkeVurderingResultat" +
                "        WHERE ankeResultat = (FROM AnkeResultat " +
                "        WHERE ankeBehandling.id = :behandlingId)", AnkeVurderingResultatEntitet.class);// NOSONAR //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void leggTilAnkeResultat(Behandling ankeBehandling) {
        entityManager.persist(AnkeResultatEntitet.builder().medAnkeBehandling(ankeBehandling).build());
        entityManager.flush();
    }

    public void settPåAnketBehandling(Behandling ankeBehandling, Behandling påAnketBehandling) {
        Optional<AnkeResultatEntitet> ankeResultatOpt = hentAnkeResultat(ankeBehandling);
        AnkeResultatEntitet ankeResultat = ankeResultatOpt.orElseThrow(() -> new IllegalStateException("fant ikke ankeResultat for behandling."));
        ankeResultat.settPåAnketBehandling(påAnketBehandling);
        entityManager.persist(ankeResultat);
        entityManager.flush();
    }

    public void slettAnkeVurderingResultat(Long ankeBehandlingId) {
        Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat = hentAnkeVurderingResultat(ankeBehandlingId);
        ankeVurderingResultat.ifPresent(avr -> {
            entityManager.remove(avr);
            entityManager.flush();
        });
    }

    public Long lagreVurderingsResultat(Behandling ankeBehandling, AnkeVurderingResultatEntitet.Builder ankeVurderingResultatBuilder) {
        Optional<AnkeResultatEntitet> ankeResultatOpt = hentAnkeResultat(ankeBehandling);
        Optional<AnkeVurderingResultatEntitet> eksisterende = hentAnkeVurderingResultat(ankeBehandling.getId());
        eksisterende.ifPresent(ankeVurderingResultat -> entityManager.remove(ankeVurderingResultat));
        ankeResultatOpt.ifPresent(ankeVurderingResultatBuilder::medAnkeResultat);
        AnkeVurderingResultatEntitet nyAnkeVurderingResultat = ankeVurderingResultatBuilder.build();
        entityManager.persist(nyAnkeVurderingResultat);
        entityManager.flush();
        return nyAnkeVurderingResultat.getId();
    }

    public Optional<AnkeVurderingResultatEntitet> hentAnkeVurderingResultat(Long ankeBehandlingId) {
        Optional<AnkeVurderingResultatEntitet> ankeVurderingResultatEntitet = hentVurderingsResultaterForAnkeBehandling(ankeBehandlingId);
        if (ankeVurderingResultatEntitet.isPresent()){
            return Optional.of(ankeVurderingResultatEntitet.get());
        }
        return Optional.empty();
    }

}
