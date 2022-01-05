package no.nav.foreldrepenger.behandlingslager.risikoklassifisering;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@ApplicationScoped
public class RisikoklassifiseringRepository {
    private static final LocalDateTime FØRSTE_VURDERING_LAGRET_I_FPRISK_TIDSPUNKT = LocalDateTime.of(2021, 12, 14, 20,28);

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

    public List<RisikoklassifiseringEntitet> finnKlassifiseringerForMigrering() {
        var query = entityManager.createQuery("from RisikoklassifiseringEntitet " +
            "where erAktiv = :erAktiv " +
            "and faresignalVurdering is not null " +
            "and faresignalVurdering != :udefinert " +
            "and opprettetTidspunkt < :førsteMigrerteSak", RisikoklassifiseringEntitet.class);
        query.setParameter("førsteMigrerteSak", FØRSTE_VURDERING_LAGRET_I_FPRISK_TIDSPUNKT);
        query.setParameter("udefinert", FaresignalVurdering.UDEFINERT);
        query.setParameter("erAktiv", true);
        return query.getResultList();
    }


    public void lagreRisikoklassifisering(RisikoklassifiseringEntitet risikoklassifisering, Long behandlingId) {
        Objects.requireNonNull(risikoklassifisering, "risikoklassifisering");
        Objects.requireNonNull(risikoklassifisering.getBehandlingId(), "behandlingId");

        deaktiverGammeltGrunnlagOmNødvendig(behandlingId);

        lagre(risikoklassifisering);
    }


    public Optional<RisikoklassifiseringEntitet> hentRisikoklassifiseringForBehandling(long behandlingId) {
        var query = entityManager.createQuery("from RisikoklassifiseringEntitet where behandlingId = :behandlingId and erAktiv = :erAktiv", RisikoklassifiseringEntitet.class);
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
