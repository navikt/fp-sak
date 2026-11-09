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
        Fagsak fagsak = entityManager.find(Fagsak.class, saksnummer, KeyType.NATURAL, LockModeType.PESSIMISTIC_WRITE);
        if (fagsak == null) {
            throw new TekniskException("FP-131238", String.format("Fant ikke sak for låsing, saksnummer=%s.", saksnummer.getVerdi()));
        }
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
