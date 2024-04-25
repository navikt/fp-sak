package no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;


@ApplicationScoped
public class AktivitetskravArbeidRepository {
    private EntityManager entityManager;

    @Inject
    public AktivitetskravArbeidRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public void lagreAktivitetskravArbeidPerioder(Long behandlingId,
                                                  AktivitetskravArbeidPerioderEntitet.Builder builder,
                                                  LocalDate fraDato,
                                                  LocalDate tilDato) {

        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        var nyttGrunnlag = AktivitetskravGrunnlagEntitet.Builder.oppdatere(aktivtGrunnlag)
            .medBehandlingId(behandlingId)
            .medPerioderMedAktivitetskravArbeid(builder)
            .medPeriode(fraDato, tilDato);

        lagreGrunnlag(aktivtGrunnlag, nyttGrunnlag.build());
    }

    private void lagreGrunnlag(Optional<AktivitetskravGrunnlagEntitet> aktivtGrunnlag, AktivitetskravGrunnlagEntitet nyttGrunnlag) {
        var differ = new RegisterdataDiffsjekker(true).getDiffEntity();
        if (aktivtGrunnlag.isEmpty() || differ.areDifferent(aktivtGrunnlag.orElse(null), nyttGrunnlag)) {
            aktivtGrunnlag.ifPresent(eksisterendeGrunnlag -> {
                eksisterendeGrunnlag.deaktiver();
                entityManager.persist(eksisterendeGrunnlag);
            });
            nyttGrunnlag.getAktivitetskravPerioderMedArbeidEnitet().ifPresent(perioder -> {
                entityManager.persist(perioder);
                for (var entitet : perioder.getAktivitetskravArbeidPeriodeListe()) {
                    entityManager.persist(entitet);
                }
            });
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        }
    }

    public Optional<AktivitetskravGrunnlagEntitet> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "FROM AktivitetskravGrunnlag grunnlag WHERE grunnlag.behandlingId = :behandlingId AND grunnlag.aktiv = true",
            AktivitetskravGrunnlagEntitet.class).setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }
}
