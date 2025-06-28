package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class EngangsstønadBeregningRepository {

    private EntityManager entityManager;
    private SatsRepository satsRepository;

    EngangsstønadBeregningRepository() {
        // for CDI proxy
    }

    @Inject
    public EngangsstønadBeregningRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
        this.satsRepository = new SatsRepository(entityManager);
    }

    public Optional<EngangsstønadBeregning> hentEngangsstønadBeregning(Long behandlingId) {
        return getAktivEngangsstønadBeregning(behandlingId);
    }

    public void lagre(Long behandlingId, EngangsstønadBeregning engangsstønadBeregning) {
        var eksisterende = getAktivEngangsstønadBeregning(behandlingId);
        if (eksisterende.isPresent()) {
            if (eksisterende.get().likBeregning(engangsstønadBeregning)) {
                // Hvis den eksisterende beregningen er lik den nye, gjør ingenting
                return;
            }
            deaktiverTidligereEngangsstønadBeregning(eksisterende.get());
        }
        entityManager.persist(engangsstønadBeregning);
        entityManager.flush();
    }

    public void deaktiverTidligereEngangsstønadBeregning(Long behandlingId) {
        hentEngangsstønadBeregning(behandlingId).ifPresent(this::deaktiverTidligereEngangsstønadBeregning);
        entityManager.flush();
    }

    private void deaktiverTidligereEngangsstønadBeregning(EngangsstønadBeregning engangsstønadBeregning) {
        engangsstønadBeregning.setAktiv(false);
        entityManager.persist(engangsstønadBeregning);
    }

    private Optional<EngangsstønadBeregning> getAktivEngangsstønadBeregning(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT esb FROM LegacyESBeregning esb WHERE esb.behandlingId = :behandling_id AND esb.aktiv = true",
            EngangsstønadBeregning.class).setParameter("behandling_id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public boolean skalReberegne(Long behandlingId, LocalDate fødselsdato) {
        var vedtakSats = hentEngangsstønadBeregning(behandlingId).map(EngangsstønadBeregning::getSatsVerdi).orElse(0L);
        var satsVedFødsel = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, fødselsdato).getVerdi();
        return vedtakSats != satsVedFødsel;
    }

}
