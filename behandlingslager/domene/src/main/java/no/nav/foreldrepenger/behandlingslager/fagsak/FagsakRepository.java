package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import jakarta.persistence.TypedQuery;

import org.hibernate.jpa.HibernateHints;
import org.hibernate.query.NativeQuery;

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
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Fagsak finnEksaktFagsak(long fagsakId) {
        var query = getFagsakQuery(fagsakId);
        var fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Fagsak finnEksaktFagsak(Saksnummer saksnummer) {
        var query = getFagsakWhereSaksnummerQuery(saksnummer);
        var fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Fagsak finnEksaktFagsakReadOnly(long fagsakId) {
        var query = getFagsakQuery(fagsakId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        var fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Fagsak finnEksaktFagsakReadOnly(Saksnummer saksnummer) {
        var query = getFagsakWhereSaksnummerQuery(saksnummer)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        var fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Optional<Fagsak> finnUnikFagsak(long fagsakId) {
        var query = getFagsakQuery(fagsakId);
        var opt = HibernateVerktøy.hentUniktResultat(query);
        if (opt.isPresent()) {
            entityManager.refresh(opt.get());
        }
        return opt;
    }

    public List<Fagsak> hentForBruker(AktørId aktørId) {
        var query = entityManager.createQuery("from Fagsak where navBruker.aktørId=:aktørId and stengt=:ikkestengt",
                Fagsak.class);
        query.setParameter("aktørId", aktørId);
        query.setParameter("ikkestengt", false);
        return query.getResultList();
    }

    public List<Fagsak> hentForBrukerMulti(Set<AktørId> aktørId) {
        var query = entityManager.createQuery("from Fagsak where navBruker.aktørId in (:aktørId) and stengt=:ikkestengt",
                Fagsak.class);
        query.setParameter("aktørId", aktørId);
        query.setParameter("ikkestengt", false);
        return query.getResultList();
    }

    public Optional<Journalpost> hentJournalpost(JournalpostId journalpostId) {
        var query = entityManager.createQuery("from Journalpost where journalpostId=:journalpost",
                Journalpost.class);
        query.setParameter("journalpost", journalpostId);
        var journalposter = query.getResultList();
        return journalposter.isEmpty() ? Optional.empty() : Optional.ofNullable(journalposter.getFirst());
    }

    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer) {
        var query = getFagsakWhereSaksnummerQuery(saksnummer);

        var fagsaker = query.getResultList();
        if (fagsaker.size() > 1) {
            throw flereEnnEnFagsakFeil(saksnummer);
        }

        return fagsaker.isEmpty() ? Optional.empty() : Optional.of(fagsaker.getFirst());
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
        var query = entityManager.createNativeQuery("UPDATE BRUKER SET AKTOER_ID = :aktoer WHERE ID=:id");
        query.setParameter("aktoer", aktørId.getId());
        query.setParameter("id", fagsak.getNavBruker().getId());
        query.executeUpdate();
        entityManager.flush();
    }

    public void oppdaterBrukerMedAktørId(AktørId utgåttAktørId, AktørId nyAktørId) {
        var query = entityManager.createNativeQuery("UPDATE BRUKER SET AKTOER_ID = :ny WHERE AKTOER_ID=:gammel");
        query.setParameter("gammel", utgåttAktørId.getId());
        query.setParameter("ny", nyAktørId.getId());
        query.executeUpdate();
        entityManager.flush();
    }

    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer, boolean taSkriveLås) {
        var query = getFagsakWhereSaksnummerQuery(saksnummer);
        if (taSkriveLås) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }

        var fagsaker = query.getResultList();
        if (fagsaker.size() > 1) {
            throw flereEnnEnFagsakFeil(saksnummer);
        }

        return fagsaker.isEmpty() ? Optional.empty() : Optional.of(fagsaker.getFirst());
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
        query.setParameter("fagsakStatus", fagsakStatus);

        return query.getResultList();
    }

    public List<Saksnummer> hentÅpneFagsakerUtenBehandling() {
        var query = entityManager.createNativeQuery(
                "select f.saksnummer from FAGSAK f where f.FAGSAK_STATUS<>'AVSLU' and not exists (select * from BEHANDLING b where b.FAGSAK_ID = f.ID)");
        @SuppressWarnings("unchecked")
        List<String> saksnumre = query.getResultList();
        return saksnumre.stream().filter(Objects::nonNull).map(Saksnummer::new).toList();
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
         var query = (NativeQuery<Fagsak>) entityManager
            .createNativeQuery(
            "SELECT f.* FROM FAGSAK f JOIN FAGSAK_RELASJON fr ON (fr.aktiv = :aktivRelasjon AND f.id IN (fr.fagsak_en_id, fr.fagsak_to_id)) "
                + " WHERE fagsak_to_id IS NOT NULL "
                + " AND fagsak_status = :lopende "
                + " AND EXISTS (select * FROM FAGSAK f2 join BEHANDLING b on b.fagsak_id = f2.id join BEHANDLING_RESULTAT br ON br.behandling_id = b.id WHERE f2.id = f.id AND br.behandling_resultat_type = :opphor ) "
                + " AND NOT EXISTS (select * FROM BEHANDLING b2 WHERE b2.fagsak_id = f.id AND behandling_status <> :avsluttet ) "
                + " AND NOT EXISTS (select * FROM GR_NESTESAK ns join BEHANDLING b3 ON b3.id = ns.behandling_id JOIN FAGSAK f3 ON f3.id = b3.fagsak_id WHERE f3.id = f.ID AND ns.aktiv = :aktivNesteSak ) ",
                Fagsak.class);

        query.setParameter("aktivRelasjon", "J")
            .setParameter("lopende", FagsakStatus.LØPENDE.getKode())
            .setParameter("opphor", BehandlingResultatType.OPPHØR.getKode())
            .setParameter("avsluttet", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("aktivNesteSak", "J");

       return query.getResultList();
    }

    public void lagreFagsakNotat(Long fagsakId, String notat) {
        if (notat == null || notat.isEmpty()) {
            return;
        }
        var nyttnotat = new FagsakNotat(fagsakId, notat);
        entityManager.persist(nyttnotat);
        entityManager.flush();
    }

    public List<FagsakNotat> hentFagsakNotater(Long fagsakId) {
        var query = entityManager.createQuery("from FagsakNotat where fagsakId=:fagsakId AND aktiv = true order by opprettetTidspunkt asc", FagsakNotat.class)
            .setParameter("fagsakId", fagsakId);
        return query.getResultList();
    }


    public List<Fagsak> finnLøpendeFagsakerFPForEnPeriode(LocalDateTime fraDatoTid, LocalDateTime tilDatoTid) {
        var query = entityManager.createQuery("select f from Fagsak f " +
                    "where f.fagsakStatus = :lopende and f.opprettetTidspunkt > :fomTid and f.opprettetTidspunkt < :tomTid and f.ytelseType in (:fp, :svp)",
                Fagsak.class)
            .setParameter("lopende", FagsakStatus.LØPENDE)
            .setParameter("fomTid", fraDatoTid)
            .setParameter("tomTid", tilDatoTid)
            .setParameter("fp", FagsakYtelseType.FORELDREPENGER)
            .setParameter("svp", FagsakYtelseType.SVANGERSKAPSPENGER);

        return query.getResultList();
    }

    private TypedQuery<Fagsak> getFagsakQuery(long fagsakId) {
        var query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class);
        query.setParameter("fagsakId", fagsakId);
        return query;
    }

    private TypedQuery<Fagsak> getFagsakWhereSaksnummerQuery(Saksnummer saksnummer) {
        return entityManager.createQuery("from Fagsak where saksnummer=:saksnummer", Fagsak.class).setParameter( "saksnummer", saksnummer);
    }
}
