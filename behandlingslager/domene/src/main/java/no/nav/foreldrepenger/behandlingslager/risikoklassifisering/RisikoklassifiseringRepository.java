package no.nav.foreldrepenger.behandlingslager.risikoklassifisering;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

@ApplicationScoped
public class RisikoklassifiseringRepository {

    private EntityManager entityManager;

    RisikoklassifiseringRepository() {
        // for CDI proxy
    }

    @Inject
    public RisikoklassifiseringRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }


    public void lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering faresignalVurdering, long behandlingId) {

        var gammelEntitet = hentRisikoklassifiseringForBehandling(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Finner ikke risikoklassifisering for behandling med id " + behandlingId));

        deaktiverGrunnlag(gammelEntitet);

        var nyEntitet = RisikoklassifiseringEntitet.builder()
            .medKontrollresultat(gammelEntitet.getKontrollresultat())
            .medFaresignalVurdering(faresignalVurdering)
            .buildFor(gammelEntitet.getBehandlingId());

        lagre(nyEntitet);
    }


    public void lagreRisikoklassifisering(RisikoklassifiseringEntitet risikoklassifisering, Long behandlingId) {
        Objects.requireNonNull(risikoklassifisering, "risikoklassifisering");
        Objects.requireNonNull(risikoklassifisering.getBehandlingId(), "behandlingId");

        deaktiverGammeltGrunnlagOmNødvendig(behandlingId);

        lagre(risikoklassifisering);
    }


    public Optional<RisikoklassifiseringEntitet> hentRisikoklassifiseringForBehandling(long behandlingId) {
        TypedQuery<RisikoklassifiseringEntitet> query = entityManager.createQuery("from RisikoklassifiseringEntitet where behandlingId = :behandlingId and erAktiv = :erAktiv", RisikoklassifiseringEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("erAktiv", true);
        return hentUniktResultat(query);
    }

    private void lagre(RisikoklassifiseringEntitet risikoklassifisering) {
        risikoklassifisering.setErAktiv(true);

        entityManager.persist(risikoklassifisering);
        entityManager.flush();
    }

    private void deaktiverGammeltGrunnlagOmNødvendig(Long behandlingId) {
        hentRisikoklassifiseringForBehandling(behandlingId).ifPresent(this::deaktiverGrunnlag);
    }

    private void deaktiverGrunnlag(RisikoklassifiseringEntitet risikoklassifiseringEntitet) {
        risikoklassifiseringEntitet.setErAktiv(false);
        entityManager.persist(risikoklassifiseringEntitet);
        entityManager.flush();
    }

}
