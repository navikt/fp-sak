package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class BehandlingRepository {

    private static final String FAGSAK_ID = "fagsakId"; //$NON-NLS-1$
    private static final String BEHANDLING_ID = "behandlingId"; //$NON-NLS-1$
    private static final String BEHANDLING_UUID = "behandlingUuid"; //$NON-NLS-1$

    private EntityManager entityManager;

    BehandlingRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private static Optional<Behandling> optionalFirst(List<Behandling> behandlinger) {
        return behandlinger.isEmpty() ? Optional.empty() : Optional.of(behandlinger.get(0));
    }

    EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Hent Behandling, der det ikke er gitt at behandlingId er korrekt (eks. for validering av innsendte verdier)
     */
    public Optional<Behandling> finnUnikBehandlingForBehandlingId(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR //$NON-NLS-1$
        return hentUniktResultat(lagBehandlingQuery(behandlingId));
    }

    /**
     * Hent Behandling med angitt id.
     */
    public Behandling hentBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR //$NON-NLS-1$
        return hentEksaktResultat(lagBehandlingQuery(behandlingId));
    }

    /**
     * Hent Behandling med angitt uuid.
     */
    public Behandling hentBehandling(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR //$NON-NLS-1$
        return hentEksaktResultat(lagBehandlingQuery(behandlingUuid));
    }

    /**
     * Hent Behandling med angitt uuid hvis den finnes.
     */
    public Optional<Behandling> hentBehandlingHvisFinnes(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR //$NON-NLS-1$
        return hentUniktResultat(lagBehandlingQuery(behandlingUuid));
    }

    /**
     * NB: Sikker på at du vil hente alle behandlinger, inklusiv de som er lukket?
     * <p>
     * Hent alle behandlinger for angitt saksnummer.
     * Dette er eksternt saksnummer angitt av GSAK.
     */
    public List<Behandling> hentAbsoluttAlleBehandlingerForSaksnummer(Saksnummer saksnummer) {
        Objects.requireNonNull(saksnummer, "saksnummer"); //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh, Fagsak AS fagsak WHERE beh.fagsak.id=fagsak.id AND fagsak.saksnummer=:saksnummer", //$NON-NLS-1$
            Behandling.class);
        query.setParameter("saksnummer", saksnummer); //$NON-NLS-1$
        return query.getResultList();
    }

    /**
     * NB: Sikker på at du vil hente alle behandlinger, inklusiv de som er lukket?
     * <p>
     * Hent alle behandlinger for angitt fagsakId.
     */
    public List<Behandling> hentAbsoluttAlleBehandlingerForFagsak(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID); //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh, Fagsak AS fagsak WHERE beh.fagsak.id=fagsak.id AND fagsak.id=:fagsakId", //$NON-NLS-1$
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId); //$NON-NLS-1$
        return query.getResultList();
    }

    /**
     * Hent siste behandling for angitt {@link Fagsak#id}
     */
    public Optional<Behandling> hentSisteYtelsesBehandlingForFagsakId(Long fagsakId) {
        return finnSisteBehandling(fagsakId, BehandlingType.getYtelseBehandlingTyper(), false);
    }

    /**
     * Hent siste behandling for angitt {@link Fagsak#id} og behandling type
     */
    public Optional<Behandling> hentSisteBehandlingAvBehandlingTypeForFagsakId(Long fagsakId, BehandlingType behandlingType) {
        return finnSisteBehandling(fagsakId, Set.of(behandlingType), false);
    }

    /**
     * Hent alle behandlinger for en fagsak som har en av de angitte behandlingsårsaker
     */
    public List<Behandling> hentBehandlingerMedÅrsakerForFagsakId(Long fagsakId, Set<BehandlingÅrsakType> årsaker) {
        TypedQuery<Behandling> query = getEntityManager().createQuery("SELECT b FROM Behandling b" +
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
        Objects.requireNonNull(fagsakId, FAGSAK_ID); //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh WHERE beh.fagsak.id = :fagsakId AND beh.status != :status", //$NON-NLS-1$
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId); //$NON-NLS-1$
        query.setParameter("status", BehandlingStatus.AVSLUTTET); // NOSONAR //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    /**
     * Hent alle åpne behandlinger på fagsak.
     */
    public List<Behandling> hentÅpneBehandlingerForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID); //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh " +
                "WHERE beh.fagsak.id = :fagsakId " +
                "AND beh.status NOT IN (:status)", //$NON-NLS-1$
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId); //$NON-NLS-1$
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser()); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    public List<Long> hentÅpneBehandlingerIdForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID); //$NON-NLS-1$

        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT beh.id from Behandling AS beh " +
                "WHERE beh.fagsak.id = :fagsakId " +
                "AND beh.status NOT IN (:status)", //$NON-NLS-1$
            Long.class);
        query.setParameter(FAGSAK_ID, fagsakId); //$NON-NLS-1$
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser()); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_CACHE_MODE, "IGNORE");
        return query.getResultList();
    }

    public boolean harÅpenOrdinærYtelseBehandlingerForFagsakId(Long fagsakId) {
        return hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
            .anyMatch(b -> !b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING));
    }

    public List<Behandling> hentÅpneYtelseBehandlingerForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID); //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh " +
                "WHERE beh.fagsak.id = :fagsakId " +
                "AND beh.behandlingType IN (:ytelseTyper) " +
                "AND beh.status NOT IN (:status)", //$NON-NLS-1$
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId); //$NON-NLS-1$
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser()); //$NON-NLS-1$
        query.setParameter("ytelseTyper", BehandlingType.getYtelseBehandlingTyper()); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    /** Kaller lagre Behandling, og renser first-level cache i JPA. */
    public Long lagreOgClear(Behandling behandling, BehandlingLås lås) {
        Long id = lagre(behandling, lås);
        getEntityManager().clear();
        return id;
    }

    /**
     * Lagrer behandling, sikrer at relevante parent-entiteter (Fagsak, FagsakRelasjon) også oppdateres.
     */
    public Long lagre(Behandling behandling, BehandlingLås lås) {
        if (!Objects.equals(behandling.getId(), lås.getBehandlingId())) {
            // hvis satt må begge være like. (Objects.equals håndterer også at begge er null)
            throw new IllegalArgumentException(
                "Behandling#id [" + behandling.getId() + "] og lås#behandlingId [" + lås.getBehandlingId() + "] må være like, eller begge må være null."); //$NON-NLS-1$
        }

        long behandlingId = lagre(behandling);
        verifiserBehandlingLås(lås);

        // i tilfelle denne ikke er satt fra før, f.eks. for ny entitet
        lås.setBehandlingId(behandlingId);

        return behandlingId;
    }

    @SuppressWarnings("unchecked")
    public Optional<Behandling> finnSisteAvsluttedeIkkeHenlagteBehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        return optionalFirst(finnAlleAvsluttedeIkkeHenlagteBehandlingerAvType(fagsakId, BehandlingType.getYtelseBehandlingTyper()));
    }

    public List<Behandling> finnAlleAvsluttedeIkkeHenlagteBehandlinger(Long fagsakId) {
        return finnAlleAvsluttedeIkkeHenlagteBehandlingerAvType(fagsakId, BehandlingType.getYtelseBehandlingTyper());
    }

    private List<Behandling> finnAlleAvsluttedeIkkeHenlagteBehandlingerAvType(Long fagsakId, Set<BehandlingType> inkluder) {
        // BehandlingResultatType = Innvilget, endret, ikke endret, avslått.
        Objects.requireNonNull(fagsakId, FAGSAK_ID); // NOSONAR //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
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
        query.setHint(QueryHints.HINT_READONLY, true);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public Optional<Behandling> finnSisteInnvilgetBehandling(Long fagsakId) {
        // BehandlingResultatType = Innvilget, endret.
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
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
     * Lagrer vilkårResultat på en Behandling. Sørger for at samtidige oppdateringer på samme Behandling, eller
     * andre Behandlinger
     * på samme Fagsak ikke kan gjøres samtidig.
     *
     * @return id for {@link VilkårResultat} opprettet/endret.
     * @see BehandlingLås
     */
    public Long lagre(VilkårResultat vilkårResultat, BehandlingLås lås) {
        long id = lagre(vilkårResultat);
        verifiserBehandlingLås(lås);
        getEntityManager().flush();
        return id;
    }

    /**
     * Ta lås for oppdatering av behandling/fagsak. Påkrevd før lagring.
     * Convenience metode som tar hele entiteten.
     *
     * @see #taSkriveLås(Long, Long)
     */
    public BehandlingLås taSkriveLås(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling"); //$NON-NLS-1$
        Long behandlingId = behandling.getId();
        return taSkriveLås(behandlingId);
    }

    public BehandlingLås taSkriveLås(Long behandlingId) {
        BehandlingLåsRepository låsRepo = new BehandlingLåsRepository(getEntityManager());
        return låsRepo.taLås(behandlingId);
    }

    /**
     * Slette tidligere beregning på en Behandling. Sørger for at samtidige oppdateringer på samme Behandling,
     * eller andre Behandlinger
     * på samme Fagsak ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     * @deprecated Gjelder kun Engangsstønad. Skal flyttes
     */
    @Deprecated
    public void slettTidligereBeregningerES(Behandling behandling, BehandlingLås lås) {
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        if (behandlingsresultat != null
            && behandlingsresultat.getBeregningResultat() != null) {
            behandlingsresultat.getBeregningResultat().getBeregninger()
                .forEach(beregning -> {
                    Query query = getEntityManager().createQuery(
                        "DELETE FROM LegacyESBeregning b WHERE b.id=:beregningId");
                    query.setParameter("beregningId", beregning.getId()); //$NON-NLS-1$
                    query.executeUpdate();
                });
            verifiserBehandlingLås(lås);
            getEntityManager().flush();
        }
    }

    private Optional<Behandling> finnSisteBehandling(Long fagsakId, Set<BehandlingType> behandlingType, boolean readOnly) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        Objects.requireNonNull(behandlingType, "behandlingType");

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "from Behandling where fagsak.id=:fagsakId and behandlingType in (:behandlingType) order by opprettetTidspunkt desc",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("behandlingType", behandlingType);
        if (readOnly) {
            query.setHint(QueryHints.HINT_READONLY, "true");
        }
        return optionalFirst(query.getResultList());
    }

    public Optional<Behandling> finnSisteIkkeHenlagteYtelseBehandlingFor(Long fagsakId) {
        return finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(fagsakId, BehandlingType.getYtelseBehandlingTyper());
    }

    public Optional<Behandling> finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(Long fagsakId, BehandlingType behandlingType) {
        Objects.requireNonNull(behandlingType, "behandlingType");
        return finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(fagsakId, Set.of(behandlingType));
    }

    private Optional<Behandling> finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(Long fagsakId, Set<BehandlingType> behandlingTyper) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
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

        return optionalFirst(query.getResultList());
    }

    private IllegalStateException flereAggregatOpprettelserISammeLagringException(Class<?> aggregat) {
        return new IllegalStateException("Glemt å lagre "
            + aggregat.getSimpleName()
            + "? Denne må lagres separat siden den er et selvstendig aggregat delt mellom behandlinger"); //$NON-NLS-1$
    }

    private TypedQuery<Behandling> lagBehandlingQuery(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery("from Behandling where id=:" + BEHANDLING_ID, Behandling.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_ID, behandlingId); //$NON-NLS-1$
        return query;
    }

    private TypedQuery<Behandling> lagBehandlingQuery(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery("from Behandling where uuid=:" + BEHANDLING_UUID, Behandling.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_UUID, behandlingUuid); //$NON-NLS-1$
        return query;
    }

    private Long lagre(VilkårResultat vilkårResultat) {
        Behandling originalBehandling = vilkårResultat.getOriginalBehandling();

        if (originalBehandling == null || originalBehandling.getId() == null) {
            throw new IllegalStateException("Glemt å lagre " // NOSONAR //$NON-NLS-1$
                + Behandling.class.getSimpleName()
                + "? Denne må lagres separat siden "// NOSONAR //$NON-NLS-1$
                + VilkårResultat.class.getSimpleName()
                + " er et separat aggregat delt mellom flere behandlinger"); //$NON-NLS-1$ // NOSONAR
        }

        getEntityManager().persist(originalBehandling);
        getEntityManager().persist(vilkårResultat);
        for (Vilkår ivr : vilkårResultat.getVilkårene()) {
            getEntityManager().persist(ivr);
            getEntityManager().persist(ivr.getVilkårResultat());
            for (Vilkår vr : ivr.getVilkårResultat().getVilkårene()) {
                getEntityManager().persist(vr);
            }
        }
        return vilkårResultat.getId();
    }

    /** sjekk lås og oppgrader til skriv */
    public void verifiserBehandlingLås(BehandlingLås lås) {
        BehandlingLåsRepository låsHåndterer = new BehandlingLåsRepository(getEntityManager());
        låsHåndterer.oppdaterLåsVersjon(lås);
    }

    Long lagre(Behandling behandling) {
        getEntityManager().persist(behandling);

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        if (behandlingsresultat != null) {
            getEntityManager().persist(behandlingsresultat);

            VilkårResultat vilkårResultat = behandlingsresultat.getVilkårResultat();
            if (vilkårResultat != null && vilkårResultat.getId() == null) {
                throw flereAggregatOpprettelserISammeLagringException(VilkårResultat.class);
            }
        }
        List<BehandlingÅrsak> behandlingÅrsak = behandling.getBehandlingÅrsaker();
        behandlingÅrsak.forEach(getEntityManager()::persist);

        getEntityManager().flush();

        return behandling.getId();
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    public Boolean erVersjonUendret(Long behandlingId, Long versjon) {
        Query query = getEntityManager().createNativeQuery(
            "SELECT COUNT(*) FROM dual " +
                "WHERE exists (SELECT 1 FROM behandling WHERE (behandling.id = ?) AND (behandling.versjon = ?))");
        query.setParameter(1, behandlingId);
        query.setParameter(2, versjon);
        return ((BigDecimal) query.getSingleResult()).intValue() == 1;
    }

    public void oppdaterSistOppdatertTidspunkt(Behandling behandling, LocalDateTime tidspunkt) {
        Query query = getEntityManager().createNativeQuery("UPDATE BEHANDLING BE SET BE.SIST_OPPDATERT_TIDSPUNKT = :tidspunkt WHERE " +
            "BE.ID = :behandling_id");

        query.setParameter("tidspunkt", tidspunkt); // NOSONAR $NON-NLS-1$
        query.setParameter("behandling_id", behandling.getId()); // NOSONAR $NON-NLS-1$

        query.executeUpdate();
    }

    public Optional<LocalDateTime> hentSistOppdatertTidspunkt(Long behandlingId) {
        Query query = getEntityManager().createNativeQuery("SELECT be.SIST_OPPDATERT_TIDSPUNKT FROM BEHANDLING be WHERE be.ID = :behandling_id");

        query.setParameter("behandling_id", behandlingId); // NOSONAR $NON-NLS-1$

        Object resultat = query.getSingleResult();
        if (resultat == null) {
            return Optional.empty();
        }

        Timestamp timestamp = (Timestamp) resultat;
        LocalDateTime value = LocalDateTime.ofInstant(timestamp.toInstant(), TimeZone.getDefault().toZoneId());
        return Optional.of(value);
    }

    public List<BehandlingÅrsak> finnÅrsakerForBehandling(Behandling behandling) {
        TypedQuery<BehandlingÅrsak> query = entityManager.createQuery(
            "FROM BehandlingÅrsak  årsak " +
                "WHERE (årsak.behandling = :behandling)",
            BehandlingÅrsak.class);

        query.setParameter("behandling", behandling);
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    public List<BehandlingÅrsakType> finnÅrsakTyperForBehandling(Behandling behandling) {
        TypedQuery<BehandlingÅrsakType> query = entityManager.createQuery(
            "select distinct behandlingÅrsakType FROM BehandlingÅrsak årsak " +
                "WHERE årsak.behandling = :behandling",
            BehandlingÅrsakType.class);

        query.setParameter("behandling", behandling);
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    public List<Behandling> unntakOmsorg() {
        TypedQuery<Behandling> query = entityManager.createQuery(
            "SELECT behandling FROM Behandling behandling " +
                "INNER JOIN Aksjonspunkt ap on ap.behandling.id=behandling.id " +
                " WHERE ap.status IN :aapneAksjonspunktKoder " +
                "   AND ap.opprettetTidspunkt > :fom " +
                "   AND ap.opprettetTidspunkt < :tom " +
                "   AND ap.aksjonspunktDefinisjon = :aksjonspunkt ",
            Behandling.class);
        query.setHint(QueryHints.HINT_READONLY, "true");
        query.setParameter("aapneAksjonspunktKoder", AksjonspunktStatus.getÅpneAksjonspunktStatuser());
        query.setParameter("aksjonspunkt", AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
        query.setParameter("fom", LocalDate.of(2020,10,27).atStartOfDay());
        query.setParameter("tom", LocalDate.of(2020,11,1).atStartOfDay());

        return query.getResultList();
    }
}
