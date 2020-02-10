package no.nav.foreldrepenger.domene.risikoklassifisering.modell;

import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Objects;
import java.util.Optional;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

@ApplicationScoped
public class RisikoklassifiseringRepository {

    private EntityManager entityManager;

    RisikoklassifiseringRepository() {
        // for CDI proxy
    }

    @Inject
    public RisikoklassifiseringRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    
    public void lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering faresignalVurdering, long behandlingId) {

        Optional<RisikoklassifiseringEntitet> gammelEntitetOpt = hentRisikoklassifiseringForBehandling(behandlingId);

        if (!gammelEntitetOpt.isPresent()) {
            throw new IllegalStateException("Finner ikke risikoklassifisering for behandling med id " + behandlingId);
        }

        RisikoklassifiseringEntitet gammelEntitet = gammelEntitetOpt.get();

        deaktiverGrunnlag(gammelEntitet);

        RisikoklassifiseringEntitet nyEntitet = RisikoklassifiseringEntitet.builder()
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
        Optional<RisikoklassifiseringEntitet> risikoklassifiseringEntitet = hentRisikoklassifiseringForBehandling(behandlingId);
        risikoklassifiseringEntitet.ifPresent(this::deaktiverGrunnlag);
    }

    private void deaktiverGrunnlag(RisikoklassifiseringEntitet risikoklassifiseringEntitet) {
        risikoklassifiseringEntitet.setErAktiv(false);
        entityManager.persist(risikoklassifiseringEntitet);
        entityManager.flush();
    }

}
