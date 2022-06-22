package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class FagsakRepository {

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private EntityManager entityManager;

    public FagsakRepository() {
        // for CDI proxy
    }

    @Inject
    public FagsakRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Fagsak finnEksaktFagsak(long fagsakId) {
        var query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class);
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        var fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Fagsak finnEksaktFagsakReadOnly(long fagsakId) {
        var query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class)
            .setParameter("fagsakId", fagsakId)
            .setHint(QueryHints.HINT_READONLY, "true"); // NOSONAR
        var fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Optional<Fagsak> finnUnikFagsak(long fagsakId) {
        var query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class);
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        var opt = HibernateVerktøy.hentUniktResultat(query);
        if (opt.isPresent()) {
            entityManager.refresh(opt.get());
        }
        return opt;
    }

    public List<Fagsak> hentForBruker(AktørId aktørId) {
        var query = entityManager.createQuery("from Fagsak where navBruker.aktørId=:aktørId and stengt=:ikkestengt",
                Fagsak.class);
        query.setParameter("aktørId", aktørId); // NOSONAR
        query.setParameter("ikkestengt", false); // NOSONAR
        return query.getResultList();
    }

    public List<Fagsak> hentForBrukerMulti(Set<AktørId> aktørId) {
        var query = entityManager.createQuery("from Fagsak where navBruker.aktørId in (:aktørId) and stengt=:ikkestengt",
                Fagsak.class);
        query.setParameter("aktørId", aktørId); // NOSONAR
        query.setParameter("ikkestengt", false); // NOSONAR
        return query.getResultList();
    }

    public Optional<Journalpost> hentJournalpost(JournalpostId journalpostId) {
        var query = entityManager.createQuery("from Journalpost where journalpostId=:journalpost",
                Journalpost.class);
        query.setParameter("journalpost", journalpostId); // NOSONAR
        var journalposter = query.getResultList();
        return journalposter.isEmpty() ? Optional.empty() : Optional.ofNullable(journalposter.get(0));
    }

    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from Fagsak where saksnummer=:saksnummer", Fagsak.class);
        query.setParameter("saksnummer", saksnummer); // NOSONAR

        var fagsaker = query.getResultList();
        if (fagsaker.size() > 1) {
            throw flereEnnEnFagsakFeil(saksnummer);
        }

        return fagsaker.isEmpty() ? Optional.empty() : Optional.of(fagsaker.get(0));
    }

    private TekniskException flereEnnEnFagsakFeil(Saksnummer saksnummer) {
        return new TekniskException("FP-429883", "Det var flere enn en Fagsak for saksnummer: " + saksnummer);
    }

    public Long opprettNy(Fagsak fagsak) {
        if (fagsak.getId() != null) {
            throw new IllegalStateException("Fagsak [" + fagsak.getId() + "] eksisterer. Kan ikke opprette på ny");
        }
        entityManager.persist(fagsak.getNavBruker());
        entityManager.persist(fagsak);
        entityManager.flush();
        return fagsak.getId();
    }

    public void oppdaterRelasjonsRolle(Long fagsakId, RelasjonsRolleType relasjonsRolleType) {
        var fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setRelasjonsRolleType(relasjonsRolleType);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void oppdaterBruker(Long fagsakId, NavBruker bruker) {
        var fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setNavBruker(bruker);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void oppdaterBrukerMedAktørId(Long fagsakId, AktørId aktørId) {
        var fagsak = finnEksaktFagsak(fagsakId);
        var query = entityManager.createNativeQuery("UPDATE BRUKER SET AKTOER_ID = :aktoer WHERE ID=:id"); //$NON-NLS-1$
        query.setParameter("aktoer", aktørId.getId()); //$NON-NLS-1$
        query.setParameter("id", fagsak.getNavBruker().getId()); //$NON-NLS-1$
        query.executeUpdate();
        entityManager.flush();
    }

    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer, boolean taSkriveLås) {
        var query = entityManager.createQuery("from Fagsak where saksnummer=:saksnummer", Fagsak.class);
        query.setParameter("saksnummer", saksnummer); // NOSONAR
        if (taSkriveLås) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }

        var fagsaker = query.getResultList();
        if (fagsaker.size() > 1) {
            throw flereEnnEnFagsakFeil(saksnummer);
        }

        return fagsaker.isEmpty() ? Optional.empty() : Optional.of(fagsaker.get(0));
    }

    public Long lagre(Journalpost journalpost) {
        entityManager.persist(journalpost);
        return journalpost.getId();
    }

    /**
     * Oppderer status på fagsak.
     *
     * @param fagsakId - id på fagsak
     * @param status   - ny status
     */
    public void oppdaterFagsakStatus(Long fagsakId, FagsakStatus status) {
        var fagsak = finnEksaktFagsak(fagsakId);
        fagsak.oppdaterStatus(status);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public List<Fagsak> hentForStatus(FagsakStatus fagsakStatus) {
        var query = entityManager.createQuery("select fagsak from Fagsak fagsak where fagsak.fagsakStatus=:fagsakStatus",
                Fagsak.class);
        query.setParameter("fagsakStatus", fagsakStatus); // NOSONAR

        return query.getResultList();
    }

    public List<Saksnummer> hentÅpneFagsakerUtenBehandling() {
        var query = entityManager.createNativeQuery(
                "select f.saksnummer from FAGSAK f where f.FAGSAK_STATUS<>'AVSLU' and not exists (select * from BEHANDLING b where b.FAGSAK_ID = f.ID)");
        @SuppressWarnings("unchecked")
        List<String> saksnumre = query.getResultList();
        return saksnumre.stream().filter(Objects::nonNull).map(Saksnummer::new).collect(Collectors.toList());
    }

    public Saksnummer genererNyttSaksnummer() {
        var query = entityManager.createNativeQuery("select SEQ_SAKSNUMMER.nextval as num from dual"); //NOSONAR Her har vi full kontroll på sql
        var singleResult = (BigDecimal) query.getSingleResult();
        return new Saksnummer(String.valueOf(singleResult.longValue()));
    }

    public void fagsakSkalStengesForBruk(Long fagsakId) {
        var fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setStengt(true);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void fagsakSkalGjenåpnesForBruk(Long fagsakId) {
        var fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setStengt(false);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    @SuppressWarnings("unchecked")
    public List<Fagsak> hentFagsakerRelevanteForAvslutning() {
        var query = entityManager.createNativeQuery("""
        select f.*
        from fpsak.fagsak f
        join fpsak.fagsak_relasjon fr  on (aktiv = :aktiv and f.id in (fagsak_en_id, fagsak_to_id))
        where fagsak_to_id is not null
        and fagsak_status = :lopende
        and exists (select * from fpsak.fagsak f2 join fpsak.behandling b on b.fagsak_id = f2.id join fpsak.behandling_resultat br on br.behandling_id = b.id where f2.id = f.id and br.behandling_resultat_type = :opphor)
        and not exists (select * from fpsak.behandling b2 where b2.fagsak_id = f.id and behandling_status <> :avsluttet)
        and not exists (select * from fpsak.gr_nestesak ns join fpsak.behandling b3 on b3.id = ns.behandling_id join fpsak.fagsak f3 on f3.id = b3.fagsak_id where f3.id = f.ID and ns.aktiv = :aktiv )
        """, Fagsak.class);
        query.setParameter("aktiv", true);
        query.setParameter("lopende", FagsakStatus.LØPENDE);
        query.setParameter("opphor", BehandlingResultatType.OPPHØR);
        query.setParameter("avsluttet", BehandlingStatus.AVSLUTTET);

        return query.getResultList();
    }
}
