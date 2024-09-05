package no.nav.foreldrepenger.domene.entiteter;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.typer.Beløp;


@ApplicationScoped
public class BeregningsgrunnlagKoblingRepository {
    private EntityManager entityManager;

    protected BeregningsgrunnlagKoblingRepository() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagKoblingRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    public Optional<BeregningsgrunnlagKobling> hentKobling(Long behandlingId) {
        var query = entityManager.createQuery(
            "from BeregningsgrunnlagKobling kobling " +
                "where kobling.behandlingId=:behandlingId ", BeregningsgrunnlagKobling.class);
        query.setParameter("behandlingId", behandlingId);
        return hentUniktResultat(query);
    }

    public BeregningsgrunnlagKobling opprettKoblingFraOriginal(BehandlingReferanse behandlingReferanse, BeregningsgrunnlagKobling original) {
        guardMotEksisterende(behandlingReferanse);
        var bgKobling = new BeregningsgrunnlagKobling(behandlingReferanse.behandlingId(), behandlingReferanse.behandlingUuid(),
            original.getSkjæringstidspunkt().orElse(null),
            original.getGrunnbeløp().orElse(null),
            Boolean.TRUE.equals(original.getReguleringsbehov().orElse(null)));
        lagreKobling(bgKobling);
        return bgKobling;
    }

    public BeregningsgrunnlagKobling opprettKobling(BehandlingReferanse behandlingReferanse) {
        guardMotEksisterende(behandlingReferanse);
        var bgKobling = new BeregningsgrunnlagKobling(behandlingReferanse.behandlingId(), behandlingReferanse.behandlingUuid());
        lagreKobling(bgKobling);
        return bgKobling;
    }

    private void guardMotEksisterende(BehandlingReferanse behandlingReferanse) {
        var eksisterende = hentKobling(behandlingReferanse.behandlingId());
        if (eksisterende.isPresent()) {
            throw new IllegalStateException("Finnes allerede kobling på behandlingId " + behandlingReferanse.behandlingId());
        }
    }

    public BeregningsgrunnlagKobling oppdaterKoblingMedStpOgGrunnbeløp(BeregningsgrunnlagKobling kobling, Beløp g, LocalDate skjæringstidspunkt) {
        kobling.oppdaterMedGrunnbeløp(g);
        kobling.oppdaterMedSkjæringstidspunkt(skjæringstidspunkt);
        lagreKobling(kobling);
        return kobling;
    }

    public BeregningsgrunnlagKobling oppdaterKoblingMedReguleringsbehov(BeregningsgrunnlagKobling kobling, boolean erKandidatForGregulering) {
        kobling.oppdaterMedReguleringsbehov(erKandidatForGregulering);
        lagreKobling(kobling);
        return kobling;
    }

    private void lagreKobling(BeregningsgrunnlagKobling bgKobling) {
        entityManager.persist(bgKobling);
        entityManager.flush();
    }
}

