package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class FagsakRepository {

    private EntityManager entityManager;

    FagsakRepository() {
        // for CDI proxy
    }

    @Inject
    public FagsakRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Fagsak finnEksaktFagsak(long fagsakId) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class);
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        Fagsak fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Optional<Fagsak> finnUnikFagsak(long fagsakId) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class);
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        Optional<Fagsak> opt = HibernateVerktøy.hentUniktResultat(query);
        if (opt.isPresent()) {
            entityManager.refresh(opt.get());
        }
        return opt;
    }

    public List<Fagsak> hentForBruker(AktørId aktørId) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where navBruker.aktørId=:aktørId and skalTilInfotrygd=:ikkestengt", Fagsak.class);
        query.setParameter("aktørId", aktørId); // NOSONAR
        query.setParameter("ikkestengt", false); // NOSONAR
        return query.getResultList();
    }

    public List<Fagsak> hentForBrukerMulti(Set<AktørId> aktørId) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where navBruker.aktørId in (:aktørId) and skalTilInfotrygd=:ikkestengt", Fagsak.class);
        query.setParameter("aktørId", aktørId); // NOSONAR
        query.setParameter("ikkestengt", false); // NOSONAR
        return query.getResultList();
    }

    public Optional<Journalpost> hentJournalpost(JournalpostId journalpostId) {
        TypedQuery<Journalpost> query = entityManager.createQuery("from Journalpost where journalpostId=:journalpost",
            Journalpost.class);
        query.setParameter("journalpost", journalpostId); // NOSONAR
        List<Journalpost> journalposter = query.getResultList();
        return journalposter.isEmpty() ? Optional.empty() : Optional.ofNullable(journalposter.get(0));
    }

    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where saksnummer=:saksnummer", Fagsak.class);
        query.setParameter("saksnummer", saksnummer); // NOSONAR

        List<Fagsak> fagsaker = query.getResultList();
        if (fagsaker.size() > 1) {
            throw FagsakFeil.FACTORY.flereEnnEnFagsakForSaksnummer(saksnummer).toException();
        }

        return fagsaker.isEmpty() ? Optional.empty() : Optional.of(fagsaker.get(0));
    }

    public List<Fagsak> hentIkkeAvsluttedeFagsakerIPeriode(LocalDateTime fom, LocalDateTime tom) {
        TypedQuery<Fagsak> query = entityManager.createQuery(
            "select f from Fagsak f join FagsakRelasjon fr on f.id = fr.fagsakNrEn " +
            "where fagsak_status<>'AVSLU' and aktiv='J' "+
            "and fr.avsluttningsdato >= '01.09.2020' "+
            "and nvl(fr.endretTidspunkt, fr.opprettetTidspunkt) > :fom "+
            "and nvl(fr.endretTidspunkt, fr.opprettetTidspunkt) < :tom "+
            "and f.id not in (select fagsak.id from Behandling where behandlingType in ('BT-002', 'BT-004') and status not in ('IVED', 'AVSLU'))"
            , Fagsak.class);
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);
        return query.getResultList();

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
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setRelasjonsRolleType(relasjonsRolleType);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void oppdaterSaksnummer(Long fagsakId, Saksnummer saksnummer) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setSaksnummer(saksnummer);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void oppdaterBruker(Long fagsakId, NavBruker bruker) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setNavBruker(bruker);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void oppdaterBrukerMedAktørId(Long fagsakId, AktørId aktørId) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        Query query = entityManager.createNativeQuery("UPDATE BRUKER SET AKTOER_ID = :aktoer WHERE ID=:id"); //$NON-NLS-1$
        query.setParameter("aktoer", aktørId.getId()); //$NON-NLS-1$
        query.setParameter("id", fagsak.getNavBruker().getId()); //$NON-NLS-1$
        query.executeUpdate();
        entityManager.flush();
    }

    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer, boolean taSkriveLås) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where saksnummer=:saksnummer", Fagsak.class);
        query.setParameter("saksnummer", saksnummer); // NOSONAR
        if (taSkriveLås) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }

        List<Fagsak> fagsaker = query.getResultList();
        if (fagsaker.size() > 1) {
            throw FagsakFeil.FACTORY.flereEnnEnFagsakForSaksnummer(saksnummer).toException();
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
     * @param status - ny status
     */
    public void oppdaterFagsakStatus(Long fagsakId, FagsakStatus status) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.oppdaterStatus(status);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public List<Fagsak> hentForStatus(FagsakStatus fagsakStatus) {
        TypedQuery<Fagsak> query = entityManager.createQuery("select fagsak from Fagsak fagsak where fagsak.fagsakStatus=:fagsakStatus", Fagsak.class);
        query.setParameter("fagsakStatus", fagsakStatus); // NOSONAR

        return query.getResultList();
    }

    public List<Saksnummer> hentÅpneFagsakerUtenBehandling() {
        Query query = entityManager.createNativeQuery(
            "select f.saksnummer from FAGSAK f where f.FAGSAK_STATUS<>'AVSLU' and not exists (select * from BEHANDLING b where b.FAGSAK_ID = f.ID)");
        @SuppressWarnings("unchecked")
        List<String> saksnumre = query.getResultList();
        return saksnumre.stream().filter(Objects::nonNull).map(Saksnummer::new).collect(Collectors.toList());
    }

    /**
     * @deprecated : Er kun til migrering av vedtak fra fpsak til fp-abakus
     * @param fagsakId min fagsakid
     * @param maxResult antall rader
     * @return liste over fagsaker
     */
    @Deprecated(forRemoval = true)
    public List<Fagsak> hentFagsakerMedIdStørreEnn(long fagsakId, int maxResult) {
        final TypedQuery<Fagsak> query = entityManager.createQuery("SELECT f FROM Fagsak f WHERE f.id > :fagsakId order by f.id asc",
            Fagsak.class);
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        query.setMaxResults(maxResult);
        return query.getResultList();
    }

    public void fagsakSkalBehandlesAvInfotrygd(Long fagsakId) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setSkalTilInfotrygd(true);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void fagsakSkalGjenåpnesForBruk(Long fagsakId) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setSkalTilInfotrygd(false);
        entityManager.persist(fagsak);
        entityManager.flush();
    }
}
