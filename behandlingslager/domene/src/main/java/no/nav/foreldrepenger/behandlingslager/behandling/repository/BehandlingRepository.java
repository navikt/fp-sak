package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class BehandlingRepository {

    private static final String FAGSAK_ID = "fagsakId";
    private static final String BEHANDLING_ID = "behandlingId";
    private static final String BEHANDLING_UUID = "behandlingUuid";

    private EntityManager entityManager;
    private BehandlingLåsRepository låsRepository;

    BehandlingRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.låsRepository = new BehandlingLåsRepository(entityManager);
    }

    private static Optional<Behandling> optionalFirst(List<Behandling> behandlinger) {
        return behandlinger.isEmpty() ? Optional.empty() : Optional.of(behandlinger.getFirst());
    }

    /**
     * Hent Behandling, der det ikke er gitt at behandlingId er korrekt (eks. for
     * validering av innsendte verdier)
     */
    public Optional<Behandling> finnUnikBehandlingForBehandlingId(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        return hentUniktResultat(lagBehandlingQuery(behandlingId));
    }

    /**
     * Hent Behandling med angitt id.
     */
    public Behandling hentBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        return hentEksaktResultat(lagBehandlingQuery(behandlingId));
    }

    /**
     * Hent Behandling med angitt uuid.
     */
    public Behandling hentBehandling(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID);
        return hentEksaktResultat(lagBehandlingQuery(behandlingUuid));
    }

    /**
     * Hent Behandling med angitt uuid hvis den finnes.
     */
    public Optional<Behandling> hentBehandlingHvisFinnes(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID);
        return hentUniktResultat(lagBehandlingQuery(behandlingUuid));
    }


    /**
     * Hent Behandling med angitt id.
     */
    public Behandling hentBehandlingReadOnly(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        var query = lagBehandlingQuery(behandlingId);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return hentEksaktResultat(query);
    }

    public Behandling hentBehandlingReadOnly(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID);
        var query = lagBehandlingQuery(behandlingUuid);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return hentEksaktResultat(query);
    }

    /**
     * NB: Sikker på at du vil hente alle behandlinger, inklusiv de som er lukket?
     * <p>
     * Hent alle behandlinger for angitt saksnummer. Dette er eksternt saksnummer
     * angitt av GSAK.
     */
    public List<Behandling> hentAbsoluttAlleBehandlingerForSaksnummer(Saksnummer saksnummer) {
        Objects.requireNonNull(saksnummer, "saksnummer");
        Objects.requireNonNull(saksnummer.getVerdi());

        var query = entityManager.createQuery(
                "SELECT beh from Behandling AS beh JOIN FETCH beh.fagsak WHERE beh.fagsak.saksnummer=:saksnummer",
                Behandling.class);
        query.setParameter("saksnummer", saksnummer);
        return query.getResultList();
    }

    /**
     * NB: Sikker på at du vil hente alle behandlinger, inklusiv de som er lukket?
     * <p>
     * Hent alle behandlinger for angitt fagsakId.
     */
    public List<Behandling> hentAbsoluttAlleBehandlingerForFagsak(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        var query = entityManager.createQuery(
                "SELECT beh from Behandling AS beh JOIN FETCH beh.fagsak where beh.fagsak.id=:fagsakId",
                Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        return query.getResultList();
    }

    /**
     * Hent siste behandling for angitt {@link Fagsak#id}
     */
    public Optional<Behandling> hentSisteYtelsesBehandlingForFagsakId(Long fagsakId) {
        return finnSisteBehandling(fagsakId, BehandlingType.getYtelseBehandlingTyper(), false);
    }

    public Optional<Behandling> hentSisteYtelsesBehandlingForFagsakIdReadOnly(Long fagsakId) {
        return finnSisteBehandling(fagsakId, BehandlingType.getYtelseBehandlingTyper(), true);
    }

    /**
     * Hent siste behandling for angitt {@link Fagsak#id} og behandling type
     */
    public Optional<Behandling> hentSisteBehandlingAvBehandlingTypeForFagsakId(Long fagsakId, BehandlingType behandlingType) {
        return finnSisteBehandling(fagsakId, Set.of(behandlingType), false);
    }

    /**
     * Hent alle behandlinger for en fagsak som har en av de angitte
     * behandlingsårsaker
     */
    public List<Behandling> hentBehandlingerMedÅrsakerForFagsakId(Long fagsakId, Set<BehandlingÅrsakType> årsaker) {
        var query = entityManager.createQuery("SELECT b FROM Behandling b" +
                " WHERE b.fagsak.id = :fagsakId " +
                " AND EXISTS (SELECT å FROM BehandlingÅrsak å" +
                "   WHERE å.behandling = b AND å.behandlingÅrsakType IN :årsaker)", Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("årsaker", årsaker);

        return query.getResultList();
    }

    /**
     * Hent alle behandlinger som ikke er avsluttet på fagsak.
     */
    public List<Behandling> hentBehandlingerSomIkkeErAvsluttetForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        var query = entityManager.createQuery(
                "SELECT beh from Behandling AS beh WHERE beh.fagsak.id = :fagsakId AND beh.status != :status",
                Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("status", BehandlingStatus.AVSLUTTET);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    /**
     * Hent alle åpne behandlinger på fagsak.
     */
    public List<Behandling> hentÅpneBehandlingerForFagsakId(Long fagsakId) {
        return hentÅpneBehandlingerForFagsakId(fagsakId, true);
    }

    public List<Behandling> hentÅpneBehandlingerForFagsakId(Long fagsakId, boolean readOnly) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        var query = entityManager.createQuery(
            "SELECT beh from Behandling AS beh " +
                "WHERE beh.fagsak.id = :fagsakId " +
                "AND beh.status NOT IN (:status)",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser());
        if (readOnly) {
            query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        }
        return query.getResultList();
    }

    public List<Long> hentÅpneBehandlingerIdForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        var query = entityManager.createQuery(
                "SELECT beh.id from Behandling AS beh " +
                        "WHERE beh.fagsak.id = :fagsakId " +
                        "AND beh.status NOT IN (:status)",
                Long.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser());
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        query.setHint(HibernateHints.HINT_CACHE_MODE, "IGNORE");
        return query.getResultList();
    }

    public boolean harÅpenOrdinærYtelseBehandlingerForFagsakId(Long fagsakId) {
        return hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
                .anyMatch(SpesialBehandling::erIkkeSpesialBehandling);
    }

    public List<Behandling> hentÅpneYtelseBehandlingerForFagsakId(Long fagsakId) {
        return hentÅpneYtelseBehandlingerForFagsakIdInternal(fagsakId, true);
    }

    public List<Behandling> hentÅpneYtelseBehandlingerForFagsakIdForUpdate(Long fagsakId) {
        return hentÅpneYtelseBehandlingerForFagsakIdInternal(fagsakId, false);
    }

    private List<Behandling> hentÅpneYtelseBehandlingerForFagsakIdInternal(Long fagsakId, boolean readonly) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        var query = entityManager.createQuery(
                "SELECT beh from Behandling AS beh " +
                        "WHERE beh.fagsak.id = :fagsakId " +
                        "AND beh.behandlingType IN (:ytelseTyper) " +
                        "AND beh.status NOT IN (:status)",
                Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser());
        query.setParameter("ytelseTyper", BehandlingType.getYtelseBehandlingTyper());
        if (readonly) {
            query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        }
        return query.getResultList();
    }

    /** Kaller lagre Behandling, og renser first-level cache i JPA. */
    public Long lagreOgClear(Behandling behandling, BehandlingLås lås) {
        var id = lagre(behandling, lås);
        entityManager.clear();
        return id;
    }

    /**
     * Lagrer behandling, sikrer at relevante parent-entiteter (Fagsak,
     * FagsakRelasjon) også oppdateres.
     */
    public Long lagre(Behandling behandling, BehandlingLås lås) {
        if (!Objects.equals(behandling.getId(), lås.getBehandlingId())) {
            // hvis satt må begge være like. (Objects.equals håndterer også at begge er
            // null)
            throw new IllegalArgumentException(
                    "Behandling#id [" + behandling.getId() + "] og lås#behandlingId [" + lås.getBehandlingId()
                            + "] må være like, eller begge må være null.");
        }

        long behandlingId = lagre(behandling);
        verifiserBehandlingLås(lås);

        // i tilfelle denne ikke er satt fra før, f.eks. for ny entitet
        lås.setBehandlingId(behandlingId);

        return behandlingId;
    }

    public Optional<Behandling> finnSisteAvsluttedeIkkeHenlagteBehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        return optionalFirst(finnAlleAvsluttedeIkkeHenlagteBehandlingerAvType(fagsakId, BehandlingType.getYtelseBehandlingTyper()));
    }

    public List<Behandling> finnAlleAvsluttedeIkkeHenlagteBehandlinger(Long fagsakId) {
        return finnAlleAvsluttedeIkkeHenlagteBehandlingerAvType(fagsakId, BehandlingType.getYtelseBehandlingTyper());
    }

    private List<Behandling> finnAlleAvsluttedeIkkeHenlagteBehandlingerAvType(Long fagsakId, Set<BehandlingType> inkluder) {
        // BehandlingResultatType = Innvilget, endret, ikke endret, avslått.
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        var query = entityManager.createQuery(
                "SELECT behandling FROM Behandling behandling " +
                        "INNER JOIN Behandlingsresultat behandlingsresultat " +
                        "ON behandling=behandlingsresultat.behandling " +
                        "INNER JOIN BehandlingVedtak behandling_vedtak " +
                        "ON behandlingsresultat=behandling_vedtak.behandlingsresultat " +
                        "WHERE behandling.status IN :avsluttetOgIverkKode " +
                        "AND behandling.behandlingType IN (:inluderteTyper) " +
                        "AND behandling.fagsak.id=:fagsakId " +
                        "ORDER BY behandling_vedtak.vedtakstidspunkt DESC, behandling_vedtak.endretTidspunkt DESC",
                Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("avsluttetOgIverkKode", BehandlingStatus.getFerdigbehandletStatuser());
        query.setParameter("inluderteTyper", inkluder);
        query.setHint(HibernateHints.HINT_READ_ONLY, true);
        return query.getResultList();
    }

    public Optional<Behandling> finnSisteInnvilgetBehandling(Long fagsakId) {
        // BehandlingResultatType = Innvilget, endret.
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        var query = entityManager.createQuery(
                "SELECT behandling FROM Behandling behandling " +
                        "INNER JOIN Behandlingsresultat behandlingsresultat " +
                        "ON behandling=behandlingsresultat.behandling " +
                        "INNER JOIN BehandlingVedtak behandling_vedtak " +
                        "ON behandlingsresultat=behandling_vedtak.behandlingsresultat " +
                        "WHERE behandling.status IN :avsluttetOgIverkKode " +
                        "AND behandling.behandlingType NOT IN (:ekskluderteTyper) " +
                        "AND behandlingsresultat.behandlingResultatType IN (:innvilgetKoder) " +
                        "AND behandling.fagsak.id=:fagsakId " +
                        "ORDER BY behandling_vedtak.vedtakstidspunkt DESC, behandling_vedtak.endretTidspunkt DESC",
                Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("avsluttetOgIverkKode", BehandlingStatus.getFerdigbehandletStatuser());
        query.setParameter("ekskluderteTyper", BehandlingType.getAndreBehandlingTyper());
        query.setParameter("innvilgetKoder", BehandlingResultatType.getAlleInnvilgetKoder());

        return optionalFirst(query.getResultList());
    }

    /**
     * Lagrer vilkårResultat på en Behandling. Sørger for at samtidige oppdateringer
     * på samme Behandling, eller andre Behandlinger på samme Fagsak ikke kan gjøres
     * samtidig.
     *
     * @return id for {@link VilkårResultat} opprettet/endret.
     * @see BehandlingLås
     */
    public Long lagre(VilkårResultat vilkårResultat, BehandlingLås lås) {
        long id = lagre(vilkårResultat);
        verifiserBehandlingLås(lås);
        entityManager.flush();
        return id;
    }

    /**
     * Ta lås for oppdatering av behandling/fagsak. Påkrevd før lagring. Convenience
     * metode som tar hele entiteten.
     *
     * @see #taSkriveLås(Long, Long)
     */
    public BehandlingLås taSkriveLås(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling");
        var behandlingId = behandling.getId();
        return taSkriveLås(behandlingId);
    }

    public BehandlingLås taSkriveLås(Long behandlingId) {
        return låsRepository.taLås(behandlingId);
    }

    public BehandlingLås taSkriveLås(UUID behandlingUuid) {
        return låsRepository.taLås(behandlingUuid);
    }

    /**
     * Slette tidligere beregning på en Behandling. Sørger for at samtidige
     * oppdateringer på samme Behandling, eller andre Behandlinger på samme Fagsak
     * ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     * @deprecated Gjelder kun Engangsstønad. Skal flyttes
     */
    @Deprecated
    public void slettTidligereBeregningerES(Behandling behandling, BehandlingLås lås) {
        var behandlingsresultat = getBehandlingsresultat(behandling);
        if (behandlingsresultat != null
                && behandlingsresultat.getBeregningResultat() != null) {
            behandlingsresultat.getBeregningResultat().getBeregninger()
                    .forEach(beregning -> {
                        var query = entityManager.createQuery(
                                "DELETE FROM LegacyESBeregning b WHERE b.id=:beregningId");
                        query.setParameter("beregningId", beregning.getId());
                        query.executeUpdate();
                    });
            verifiserBehandlingLås(lås);
            entityManager.flush();
        }
    }

    private Optional<Behandling> finnSisteBehandling(Long fagsakId, Set<BehandlingType> behandlingType, boolean readOnly) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        Objects.requireNonNull(behandlingType, "behandlingType");

        var query = entityManager.createQuery(
                "SELECT b from Behandling b where b.fagsak.id=:fagsakId and behandlingType in (:behandlingType) order by b.opprettetTidspunkt desc",
                Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("behandlingType", behandlingType);
        if (readOnly) {
            query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        }
        return optionalFirst(query.getResultList());
    }

    public Optional<Behandling> finnSisteIkkeHenlagteYtelseBehandlingFor(Long fagsakId) {
        return finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(fagsakId, false, BehandlingType.getYtelseBehandlingTyper());
    }

    public Optional<Behandling> finnSisteIkkeHenlagteYtelseBehandlingReadOnlyFor(Long fagsakId) {
        return finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(fagsakId, true, BehandlingType.getYtelseBehandlingTyper());
    }

    public Optional<Behandling> finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(Long fagsakId, BehandlingType behandlingType) {
        Objects.requireNonNull(behandlingType, "behandlingType");
        return finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(fagsakId, false, Set.of(behandlingType));
    }

    private Optional<Behandling> finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(Long fagsakId, boolean readOnly,
                                                                                              Set<BehandlingType> behandlingTyper) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        var query = entityManager.createQuery(
                " FROM Behandling b WHERE b.fagsak.id=:fagsakId " +
                        " AND b.behandlingType IN :behandlingTyper " +
                        " AND NOT EXISTS (SELECT r FROM Behandlingsresultat r" +
                        "    WHERE r.behandling=b " +
                        "    AND r.behandlingResultatType IN :henlagtKoder)" +
                        " ORDER BY b.opprettetTidspunkt DESC",
                Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("behandlingTyper", behandlingTyper);
        query.setParameter("henlagtKoder", BehandlingResultatType.getAlleHenleggelseskoder());
        if (readOnly) {
            query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        }

        return optionalFirst(query.getResultList());
    }

    private IllegalStateException flereAggregatOpprettelserISammeLagringException(Class<?> aggregat) {
        return new IllegalStateException("Glemt å lagre "
                + aggregat.getSimpleName()
                + "? Denne må lagres separat siden den er et selvstendig aggregat delt mellom behandlinger");
    }

    private TypedQuery<Behandling> lagBehandlingQuery(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);

        var query = entityManager.createQuery("from Behandling where id=:" + BEHANDLING_ID, Behandling.class);
        query.setParameter(BEHANDLING_ID, behandlingId);
        return query;
    }

    private TypedQuery<Behandling> lagBehandlingQuery(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID);

        var query = entityManager.createQuery("from Behandling where uuid=:" + BEHANDLING_UUID, Behandling.class);
        query.setParameter(BEHANDLING_UUID, behandlingUuid);
        return query;
    }

    private Long lagre(VilkårResultat vilkårResultat) {
        var originalBehandling = vilkårResultat.getOriginalBehandling();

        if (originalBehandling == null || originalBehandling.getId() == null) {
            throw new IllegalStateException("Glemt å lagre "
                    + Behandling.class.getSimpleName()
                    + "? Denne må lagres separat siden "
                    + VilkårResultat.class.getSimpleName()
                    + " er et separat aggregat delt mellom flere behandlinger");
        }

        entityManager.persist(originalBehandling);
        entityManager.persist(vilkårResultat);
        for (var ivr : vilkårResultat.getVilkårene()) {
            entityManager.persist(ivr);
            entityManager.persist(ivr.getVilkårResultat());
            for (var vr : ivr.getVilkårResultat().getVilkårene()) {
                entityManager.persist(vr);
            }
        }
        return vilkårResultat.getId();
    }

    /** sjekk lås og oppgrader til skriv */
    public void verifiserBehandlingLås(BehandlingLås lås) {
        låsRepository.oppdaterLåsVersjon(lås);
    }

    Long lagre(Behandling behandling) {
        entityManager.persist(behandling);

        var behandlingsresultat = getBehandlingsresultat(behandling);
        if (behandlingsresultat != null) {
            entityManager.persist(behandlingsresultat);

            var vilkårResultat = behandlingsresultat.getVilkårResultat();
            if (vilkårResultat != null && vilkårResultat.getId() == null) {
                throw flereAggregatOpprettelserISammeLagringException(VilkårResultat.class);
            }
        }
        var behandlingÅrsak = behandling.getBehandlingÅrsaker();
        behandlingÅrsak.forEach(entityManager::persist);

        entityManager.flush();

        return behandling.getId();
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    public Boolean erVersjonUendret(Long behandlingId, Long versjon) {
        var query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM dual " +
                        "WHERE exists (SELECT 1 FROM behandling WHERE (behandling.id = ?) AND (behandling.versjon = ?))");
        query.setParameter(1, behandlingId);
        query.setParameter(2, versjon);
        return ((BigDecimal) query.getSingleResult()).intValue() == 1;
    }

    public void oppdaterSistOppdatertTidspunkt(Behandling behandling, LocalDateTime tidspunkt) {
        var query = entityManager.createNativeQuery("UPDATE BEHANDLING BE SET BE.SIST_OPPDATERT_TIDSPUNKT = :tidspunkt WHERE " +
                "BE.ID = :behandling_id");

        query.setParameter("tidspunkt", tidspunkt);
        query.setParameter("behandling_id", behandling.getId());

        query.executeUpdate();
    }

    public Optional<LocalDateTime> hentSistOppdatertTidspunkt(Long behandlingId) {
        var query = entityManager.createNativeQuery("SELECT be.SIST_OPPDATERT_TIDSPUNKT FROM BEHANDLING be WHERE be.ID = :behandling_id");

        query.setParameter("behandling_id", behandlingId);

        var resultat = query.getSingleResult();
        if (resultat == null) {
            return Optional.empty();
        }

        var timestamp = (Timestamp) resultat;
        var value = LocalDateTime.ofInstant(timestamp.toInstant(), TimeZone.getDefault().toZoneId());
        return Optional.of(value);
    }

    public List<BehandlingÅrsak> finnÅrsakerForBehandling(Behandling behandling) {
        var query = entityManager.createQuery(
                "FROM BehandlingÅrsak  årsak " +
                        "WHERE (årsak.behandling = :behandling)",
                BehandlingÅrsak.class);

        query.setParameter("behandling", behandling);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    public List<BehandlingÅrsakType> finnÅrsakTyperForBehandling(Behandling behandling) {
        var query = entityManager.createQuery(
                "select distinct behandlingÅrsakType FROM BehandlingÅrsak årsak " +
                        "WHERE årsak.behandling = :behandling",
                BehandlingÅrsakType.class);

        query.setParameter("behandling", behandling);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    public List<Behandling> hentÅpneBehandlingerOpprettetIntervall(LocalDateTime fom, LocalDateTime tom) {
        var query = entityManager.createQuery(
            "SELECT beh from Behandling AS beh WHERE beh.status != :avslu and beh.opprettetTidspunkt >= :fom and beh.opprettetTidspunkt < :tom",
            Behandling.class)
            .setParameter("avslu", BehandlingStatus.AVSLUTTET)
            .setParameter("fom", fom)
            .setParameter("tom", tom)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

}
