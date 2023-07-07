package no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class ArbeidsforholdValgRepository {

    private EntityManager entityManager;

    ArbeidsforholdValgRepository() {
        // CDI
    }

    @Inject
    public ArbeidsforholdValgRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public List<ArbeidsforholdValg> hentArbeidsforholdValgForBehandling(Long behandlingId) {
        var query = entityManager.createQuery("FROM ArbeidsforholdValg arb " + "WHERE arb.behandlingId = :behandlingId AND arb.aktiv = :aktiv",
            ArbeidsforholdValg.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("aktiv", true);
        return query.getResultList();
    }

    public void lagre(ArbeidsforholdValg nyttNotat, Long behandlingId) {
        var eksisterendeNotatOpt = hentEksaktValg(behandlingId, nyttNotat.getArbeidsgiver().getIdentifikator(), nyttNotat.getArbeidsforholdRef());
        if (eksisterendeNotatOpt.isPresent()) {
            var eksisterendeNotat = eksisterendeNotatOpt.get();
            eksisterendeNotat.setBegrunnelse(nyttNotat.getBegrunnelse());
            eksisterendeNotat.setVurdering(nyttNotat.getVurdering());
            lagre(eksisterendeNotat);
        } else {
            nyttNotat.setBehandlingId(behandlingId);
            lagre(nyttNotat);
        }
    }

    public void fjernValg(ArbeidsforholdValg arbeidsforholdValg) {
        if (arbeidsforholdValg.getId() == null || !arbeidsforholdValg.erAktiv()) {
            throw new IllegalStateException("FEIL: Valg som skal deaktiveres må ha "
                    + "id og være aktive. Valg som ble forsøkt deaktivert: " + arbeidsforholdValg);
        }
        arbeidsforholdValg.setAktiv(false);
        lagre(arbeidsforholdValg);
    }

    private Optional<ArbeidsforholdValg> hentEksaktValg(Long behandlingId, String arbeidsgiverIdent, InternArbeidsforholdRef internRef) {
        var arbeidsforholdValgs = hentAlleValgForArbeidsgiver(behandlingId, arbeidsgiverIdent);
        return arbeidsforholdValgs.stream().filter(valg -> valg.getArbeidsforholdRef().gjelderFor(internRef)).findFirst();
    }

    private List<ArbeidsforholdValg> hentAlleValgForArbeidsgiver(Long behandlingId, String arbeidsgiverIdent) {
        var query = entityManager.createQuery("FROM ArbeidsforholdValg arb " + "WHERE arb.behandlingId = :behandlingId " + "AND arb.aktiv = :aktiv "
            + "AND arb.arbeidsgiverIdent = :arbeidsgiverIdent", ArbeidsforholdValg.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("aktiv", true);
        query.setParameter("arbeidsgiverIdent", arbeidsgiverIdent);
        return query.getResultList();
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Lager helt ny entiteter.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long originalBehandlingId, Long nyBehandlingId) {
        var valgPåOriginalBehandling = hentArbeidsforholdValgForBehandling(originalBehandlingId);
        var kopierteValg = valgPåOriginalBehandling.stream().map(valg -> ArbeidsforholdValg.kopier(valg).build()).toList();
        kopierteValg.forEach(valg -> lagre(valg, nyBehandlingId));
    }

    private void lagre(ArbeidsforholdValg arbeidsforholdValg) {
        entityManager.persist(arbeidsforholdValg);
        entityManager.flush();
    }
}
