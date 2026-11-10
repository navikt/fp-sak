package no.nav.foreldrepenger.behandlingslager.fagsak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.hibernate.KeyType;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class FagsakLåsRepository {

    private EntityManager entityManager;

    protected FagsakLåsRepository() {
    }

    @Inject
    public FagsakLåsRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Tar lås på underliggende rader
     *
     * @param saksnummer fagsaken
     * @return låsen
     */
    public FagsakLås taLås(final Saksnummer saksnummer) {
        // Se om det er mulig å håndtere fagsakRelasjon-henting bedre (uten å laste fagsak) - da kan man se på find med NATURAL og PW med nullsjekk
        entityManager.createQuery("select fs.id, fs.versjon from Fagsak fs where fs.saksnummer=:saksnummer")
            .setParameter("saksnummer", saksnummer)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();
        return new FagsakLås(saksnummer);
    }

    /**
     * Verifisering av lås
     *
     * @param lås låsen
     */
    public void oppdaterLåsVersjon(FagsakLås lås) {
        var fagsak = entityManager.find(Fagsak.class, lås.saksnummer(), KeyType.NATURAL, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        if (fagsak == null) {
            throw new TekniskException("FP-131238", String.format("Fant ikke sak for låsing, saksnummer=%s.", lås.saksnummer().getVerdi()));
        }
    }
}
