package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;

/**
 * Henter siste {@link BeregningsgrunnlagGrunnlagEntitet} opprettet i et bestemt steg for revurdering. Ignorerer om grunnlaget er aktivt eller ikke.
 * Om revurderingen ikke har grunnlag opprettet i denne tilstanden returneres grunnlaget fra originalbehandlingen for samme tilstand.
 * @param behandlingId en behandlingId
 * @param beregningsgrunnlagTilstand steget {@link BeregningsgrunnlagGrunnlagEntitet} er opprettet i
 * @return Hvis det finnes et eller fler BeregningsgrunnlagGrunnlagEntitet som har blitt opprettet i {@code stegOpprettet} returneres den som ble opprettet sist
 */
@ApplicationScoped
public class BeregningsgrunnlagRepository {
    private static final String BEHANDLING_ID = "behandlingId";
    private static final String BEREGNINGSGRUNNLAG_TILSTAND = "beregningsgrunnlagTilstand";
    private static final String BEREGNING_AKTIVITET_AGGREGAT = "beregningAktivitetAggregat";
    private static final String BEREGNING_AKTIVITET_OVERSTYRING = "beregningAktivitetOverstyring";
    private static final String BEREGNING_REFUSJON_OVERSTYRINGER = "beregningRefusjonOverstyringer";
    private static final String BUILDER = "beregningsgrunnlagGrunnlagBuilder";
    private EntityManager entityManager;

    protected BeregningsgrunnlagRepository() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(Long behandlingId, Optional<Long> originalBehandlingId,
                                                                                                                 BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> sisteBg = hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, beregningsgrunnlagTilstand);
        if (!sisteBg.isPresent() && originalBehandlingId.isPresent()) {
            return hentSisteBeregningsgrunnlagGrunnlagEntitet(originalBehandlingId.get(), beregningsgrunnlagTilstand);
        }
        return sisteBg;
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentNestSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(Long behandlingId, Optional<Long> originalBehandlingId,
                                                                                                                     BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> sisteBg = hentNestSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, beregningsgrunnlagTilstand);
        if (!sisteBg.isPresent() && originalBehandlingId.isPresent()) {
            return hentNestSisteBeregningsgrunnlagGrunnlagEntitet(originalBehandlingId.get(), beregningsgrunnlagTilstand);
        }
        return sisteBg;
    }


    public BeregningsgrunnlagEntitet hentBeregningsgrunnlagAggregatForBehandling(Long behandlingId) {
        return hentBeregningsgrunnlagForBehandling(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler Beregningsgrunnlag for behandling " + behandlingId));
    }

    public Optional<BeregningsgrunnlagEntitet> hentBeregningsgrunnlagForBehandling(Long behandlingId) {
        return hentBeregningsgrunnlagGrunnlagEntitet(behandlingId).flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
    }

    /**
     * Hent angitt beregningsgrunnlag basert på beregningsgrunnlag id (ikke behandling Id som ellers i interfacet her).
     */
    public Optional<BeregningsgrunnlagEntitet> hentBeregningsgrunnlagForId(Long beregningsgrunnlagId) {
        TypedQuery<BeregningsgrunnlagEntitet> query = entityManager.createQuery(
            "from Beregningsgrunnlag grunnlag " +
                "where grunnlag.id = :beregningsgrunnlagId", BeregningsgrunnlagEntitet.class); //$NON-NLS-1$
        query.setParameter("beregningsgrunnlagId", beregningsgrunnlagId); //$NON-NLS-1$
        return hentUniktResultat(query);
    }

    /**
     * Henter aktivt BeregningsgrunnlagGrunnlagEntitet
     * @param behandlingId en behandlingId
     * @return Hvis det finnes en aktiv {@link BeregningsgrunnlagGrunnlagEntitet} returneres denne
     */
    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentBeregningsgrunnlagGrunnlagEntitet(Long behandlingId) {
        TypedQuery<BeregningsgrunnlagGrunnlagEntitet> query = entityManager.createQuery(
            "from BeregningsgrunnlagGrunnlagEntitet grunnlag " +
                "where grunnlag.behandlingId=:behandlingId " +
                "and grunnlag.aktiv = :aktivt", BeregningsgrunnlagGrunnlagEntitet.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_ID, behandlingId); //$NON-NLS-1$
        query.setParameter("aktivt", true); //$NON-NLS-1$
        return hentUniktResultat(query);
    }

    /**
     * Henter siste {@link BeregningsgrunnlagGrunnlagEntitet} opprettet i et bestemt steg. Ignorerer om grunnlaget er aktivt eller ikke.
     * @param behandlingId en behandlingId
     * @param beregningsgrunnlagTilstand steget {@link BeregningsgrunnlagGrunnlagEntitet} er opprettet i
     * @return Hvis det finnes et eller fler BeregningsgrunnlagGrunnlagEntitet som har blitt opprettet i {@code stegOpprettet} returneres den som ble opprettet sist
     */
    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentSisteBeregningsgrunnlagGrunnlagEntitet(Long behandlingId, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        TypedQuery<BeregningsgrunnlagGrunnlagEntitet> query = entityManager.createQuery(
            "from BeregningsgrunnlagGrunnlagEntitet " +
                "where behandlingId=:behandlingId " +
                "and beregningsgrunnlagTilstand = :beregningsgrunnlagTilstand " +
                "order by opprettetTidspunkt desc, id desc", BeregningsgrunnlagGrunnlagEntitet.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_ID, behandlingId); //$NON-NLS-1$
        query.setParameter(BEREGNINGSGRUNNLAG_TILSTAND, beregningsgrunnlagTilstand); //$NON-NLS-1$
        query.setMaxResults(1);
        return query.getResultStream().findFirst();
    }

    /**
     * Henter nest siste {@link BeregningsgrunnlagGrunnlagEntitet} opprettet i et bestemt steg. Ignorerer om grunnlaget er aktivt eller ikke.
     * @param behandlingId en behandlingId
     * @param beregningsgrunnlagTilstand steget {@link BeregningsgrunnlagGrunnlagEntitet} er opprettet i
     * @return Hvis det finnes kun ett BeregningsgrunnlagGrunnlagEntitet som har blitt opprettet i {@code stegOpprettet} returneres dette grunnlaget
     */
    private Optional<BeregningsgrunnlagGrunnlagEntitet> hentNestSisteBeregningsgrunnlagGrunnlagEntitet(Long behandlingId, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        TypedQuery<BeregningsgrunnlagGrunnlagEntitet> query = entityManager.createQuery(
            "from BeregningsgrunnlagGrunnlagEntitet " +
                "where behandlingId=:behandlingId " +
                "and beregningsgrunnlagTilstand = :beregningsgrunnlagTilstand " +
                "order by opprettetTidspunkt desc, id desc", BeregningsgrunnlagGrunnlagEntitet.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_ID, behandlingId); //$NON-NLS-1$
        query.setParameter(BEREGNINGSGRUNNLAG_TILSTAND, beregningsgrunnlagTilstand); //$NON-NLS-1$
        query.setMaxResults(2);
        return query.getResultStream().min(Comparator.comparing(BeregningsgrunnlagGrunnlagEntitet::getOpprettetTidspunkt));
    }

    /**
     * FORVALTNINGSTJENESTE: Henter grunnlag uten grunnbeløp
     * @return List med grunnlag uten grunnbeløp
     */
    public List<Long> hentBehandlingIdForGrunnlagUtenGrunnbeløp() {
        TypedQuery<Long> query = entityManager.createQuery(
            "select distinct behandlingId from BeregningsgrunnlagGrunnlagEntitet " +
                "where beregningsgrunnlag.grunnbeløp is null " +
                "and beregningsgrunnlagTilstand = :beregningsgrunnlagTilstand", Long.class); //$NON-NLS-1$
        query.setParameter(BEREGNINGSGRUNNLAG_TILSTAND, BeregningsgrunnlagTilstand.OPPRETTET); //$NON-NLS-1$
        return query.getResultList();
    }

    /**
     * FORVALTNINGSTJENESTE: Henter grunnlag uten grunnbeløp
     * @return List med grunnlag uten grunnbeløp
     */
    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentGrunnlagUtenGrunnbeløp(Long behandlingId) {
        TypedQuery<BeregningsgrunnlagGrunnlagEntitet> query = entityManager.createQuery(
            "from BeregningsgrunnlagGrunnlagEntitet " +
                "where behandlingId=:behandlingId " +
                "and beregningsgrunnlag.grunnbeløp is null " +
                "and beregningsgrunnlagTilstand = :beregningsgrunnlagTilstand " +
                "order by opprettetTidspunkt desc, id desc", BeregningsgrunnlagGrunnlagEntitet.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_ID, behandlingId); //$NON-NLS-1$
        query.setParameter(BEREGNINGSGRUNNLAG_TILSTAND, BeregningsgrunnlagTilstand.OPPRETTET); //$NON-NLS-1$
        query.setMaxResults(1);
        return query.getResultStream().findFirst();
    }


    /**
     *
     * For analysering av meldekortfeil og opprydning av disse
     */
    public List<BeregningsgrunnlagGrunnlagEntitet> hentGrunnlagForPotensielleFeilMeldekort() {
        TypedQuery<BeregningsgrunnlagGrunnlagEntitet> query = entityManager.createQuery(
            "select gr from BeregningsgrunnlagGrunnlagEntitet gr " +
                "INNER JOIN Beregningsgrunnlag bg ON gr.beregningsgrunnlag = bg " +
                "INNER JOIN BeregningsgrunnlagAktivitetStatus aks ON aks.beregningsgrunnlag = bg " +
                "where gr.opprettetTidspunkt > :opprettetFom " +
                "and gr.opprettetTidspunkt < :opprettetTom " +
                "and gr.beregningsgrunnlagTilstand = :beregningsgrunnlagTilstand " +
                "and (aks.aktivitetStatus = :status1 OR aks.aktivitetStatus = :status2) " +
                "and gr.aktiv = :aktivt", BeregningsgrunnlagGrunnlagEntitet.class); //$NON-NLS-1$

        BeregningsgrunnlagTilstand beregningsgrunnlagTilstand = BeregningsgrunnlagTilstand.FASTSATT;
        LocalDateTime opprettetFom = LocalDateTime.of(LocalDate.of(2020, 2, 10), LocalTime.NOON);
        LocalDateTime opprettetTom = LocalDateTime.of(LocalDate.of(2020, 3, 10), LocalTime.NOON);
        query.setParameter("opprettetFom", opprettetFom); //$NON-NLS-1$
        query.setParameter("opprettetTom", opprettetTom); //$NON-NLS-1$
        query.setParameter("aktivt", true); //$NON-NLS-1$
        query.setParameter("status1", no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.ARBEIDSAVKLARINGSPENGER); //$NON-NLS-1$
        query.setParameter("status2", no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.DAGPENGER); //$NON-NLS-1$
        query.setParameter(BEREGNINGSGRUNNLAG_TILSTAND, beregningsgrunnlagTilstand); //$NON-NLS-1$
        return query.getResultList();
    }


    /**
     * @deprecated Fjernes etter kjøring er ferdig
     *
     * Oppretter prosesstask for tilbakerulling av saker grunnet splitting av sammenligningsgrunnlag
     */
    @Deprecated
    public void opprettProsesstaskForTilbakerullingAvSakerBeregning() {
        Query query = entityManager.createNativeQuery(
            "INSERT INTO PROSESS_TASK (ID, TASK_TYPE, TASK_PARAMETERE) " +
                "select seq_prosess_task.nextval, 'beregning.tilbakerullingAvSaker', 'behandlingId=' || BEH.ID from FPSAK.BEHANDLING BEH " +
                "WHERE BEH.id in (" +
                "SELECT DISTINCT behid.id from FPSAK.BEHANDLING behid " +
                "INNER JOIN FPSAK.BEHANDLING_STEG_TILSTAND STEG ON STEG.BEHANDLING_ID = behid.ID " +
                "WHERE STEG.BEHANDLING_STEG = 'FORS_BERGRUNN' " +
                "AND STEG.BEHANDLING_STEG_STATUS = 'UTFØRT' " +
                "AND BEH.BEHANDLING_STATUS != 'AVSLU' " +
                "AND BEH.ID IN (" +
                "SELECT GR.BEHANDLING_ID FROM FPSAK.GR_BEREGNINGSGRUNNLAG GR " +
                "INNER JOIN FPSAK.BEREGNINGSGRUNNLAG BG ON GR.BEREGNINGSGRUNNLAG_ID = BG.ID " +
                "INNER JOIN FPSAK.BG_AKTIVITET_STATUS AKS ON AKS.BEREGNINGSGRUNNLAG_ID = BG.ID " +
                "WHERE GR.AKTIV='J' AND  (AKS.AKTIVITET_STATUS = 'AT' OR AKS.AKTIVITET_STATUS = 'FL' OR AKS.AKTIVITET_STATUS = 'AT_FL')))"); //$NON-NLS-1$
        query.executeUpdate();
        entityManager.flush();
    }

    public BeregningSats finnEksaktSats(BeregningSatsType satsType, LocalDate dato) {
        TypedQuery<BeregningSats> query = entityManager.createQuery("from BeregningSats where satsType=:satsType" + //$NON-NLS-1$
                " and periode.fomDato<=:dato" + //$NON-NLS-1$
                " and periode.tomDato>=:dato", BeregningSats.class); //$NON-NLS-1$

        query.setParameter("satsType", satsType); //$NON-NLS-1$
        query.setParameter("dato", dato); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true");//$NON-NLS-1$
        query.getResultList();
        return hentEksaktResultat(query);
    }

    public BeregningsgrunnlagGrunnlagEntitet lagre(Long behandlingId, BeregningsgrunnlagEntitet beregningsgrunnlag, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(beregningsgrunnlag, BEREGNING_AKTIVITET_AGGREGAT);
        Objects.requireNonNull(beregningsgrunnlagTilstand, BEREGNINGSGRUNNLAG_TILSTAND);

        BeregningsgrunnlagGrunnlagBuilder builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medBeregningsgrunnlag(beregningsgrunnlag);
        BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = builder.build(behandlingId, beregningsgrunnlagTilstand);
        lagreOgFlush(behandlingId, grunnlagEntitet);
        return grunnlagEntitet;
    }

    @Deprecated
    // KUN FOR MIGRERING
    public BeregningsgrunnlagGrunnlagEntitet lagreForMigrering(Long behandlingId, BeregningsgrunnlagGrunnlagBuilder builder, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(builder, BUILDER);
        Objects.requireNonNull(beregningsgrunnlagTilstand, BEREGNINGSGRUNNLAG_TILSTAND);
        BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = builder.build(behandlingId, beregningsgrunnlagTilstand);
        lagreOgFlushUtenAktivt(grunnlagEntitet);
        return grunnlagEntitet;
    }

    public BeregningsgrunnlagGrunnlagEntitet lagre(Long behandlingId, BeregningsgrunnlagGrunnlagBuilder builder, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(builder, BUILDER);
        Objects.requireNonNull(beregningsgrunnlagTilstand, BEREGNINGSGRUNNLAG_TILSTAND);
        BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = builder.build(behandlingId, beregningsgrunnlagTilstand);
        lagreOgFlush(behandlingId, grunnlagEntitet);
        return grunnlagEntitet;
    }

    public BeregningsgrunnlagGrunnlagEntitet lagreUtenAktivt(Long behandlingId, BeregningsgrunnlagGrunnlagBuilder builder, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(builder, BUILDER);
        Objects.requireNonNull(beregningsgrunnlagTilstand, BEREGNINGSGRUNNLAG_TILSTAND);
        BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = builder.build(behandlingId, beregningsgrunnlagTilstand);
        grunnlagEntitet.setAktiv(false);
        lagreOgFlushUtenAktivt(grunnlagEntitet);
        return grunnlagEntitet;
    }

    public void lagreRegisterAktiviteter(Long behandlingId,
                                         BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                         BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(beregningAktivitetAggregat, BEREGNING_AKTIVITET_AGGREGAT);
        Objects.requireNonNull(beregningsgrunnlagTilstand, BEREGNINGSGRUNNLAG_TILSTAND);

        BeregningsgrunnlagGrunnlagBuilder builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medRegisterAktiviteter(beregningAktivitetAggregat);
        lagreOgFlush(behandlingId, builder.build(behandlingId, beregningsgrunnlagTilstand));
    }

    public void lagreSaksbehandledeAktiviteter(Long behandlingId,
                                               BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                               BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(beregningAktivitetAggregat, BEREGNING_AKTIVITET_AGGREGAT);
        Objects.requireNonNull(beregningsgrunnlagTilstand, BEREGNINGSGRUNNLAG_TILSTAND);

        BeregningsgrunnlagGrunnlagBuilder builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medSaksbehandletAktiviteter(beregningAktivitetAggregat);
        lagreOgFlush(behandlingId, builder.build(behandlingId, beregningsgrunnlagTilstand));
    }

    public void lagre(Long behandlingId, BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringer) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(beregningAktivitetOverstyringer, BEREGNING_AKTIVITET_OVERSTYRING);
        BeregningsgrunnlagTilstand beregningsgrunnlagTilstand = BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER;

        BeregningsgrunnlagGrunnlagBuilder builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medOverstyring(beregningAktivitetOverstyringer);
        lagreOgFlush(behandlingId, builder.build(behandlingId, beregningsgrunnlagTilstand));
    }

    public void lagre(Long behandlingId, BeregningRefusjonOverstyringerEntitet beregningRefusjonOverstyringer) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(beregningRefusjonOverstyringer, BEREGNING_REFUSJON_OVERSTYRINGER);
        BeregningsgrunnlagTilstand beregningsgrunnlagTilstand = BeregningsgrunnlagTilstand.KOFAKBER_UT;
        BeregningsgrunnlagGrunnlagBuilder builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medRefusjonOverstyring(beregningRefusjonOverstyringer);
        lagreOgFlush(behandlingId, builder.build(behandlingId, beregningsgrunnlagTilstand));
    }
    private void lagreOgFlushUtenAktivt(BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag) {
        lagreGrunnlag(nyttGrunnlag);
        entityManager.flush();
    }

    private void lagreOgFlush(Long behandlingId, BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Optional<BeregningsgrunnlagGrunnlagEntitet> tidligereAggregat = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        if (tidligereAggregat.isPresent()) {
            tidligereAggregat.get().setAktiv(false);
            entityManager.persist(tidligereAggregat.get());
        }
        lagreGrunnlag(nyttGrunnlag);
        entityManager.flush();
    }

    private void lagreGrunnlag(BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag) {
        BeregningAktivitetAggregatEntitet registerAktiviteter = nyttGrunnlag.getRegisterAktiviteter();
        if (registerAktiviteter != null) {
            lagreBeregningAktivitetAggregat(registerAktiviteter);
        }
        nyttGrunnlag.getSaksbehandletAktiviteter().ifPresent(this::lagreBeregningAktivitetAggregat);
        nyttGrunnlag.getOverstyring()
            .ifPresent(this::lagreOverstyring);
        nyttGrunnlag.getBeregningsgrunnlag().ifPresent(entityManager::persist);
        nyttGrunnlag.getRefusjonOverstyringer()
            .ifPresent(this::lagreRefusjonOverstyring);

        entityManager.persist(nyttGrunnlag);

        nyttGrunnlag.getBeregningsgrunnlag().stream()
            .flatMap(beregningsgrunnlagEntitet -> beregningsgrunnlagEntitet.getSammenligningsgrunnlagPrStatusListe().stream())
            .forEach(this::lagreSammenligningsgrunnlagPrStatus);
    }

    private void lagreOverstyring(BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringer) {
        if (beregningAktivitetOverstyringer.getId() == null) {
            entityManager.persist(beregningAktivitetOverstyringer);
            beregningAktivitetOverstyringer.getOverstyringer().forEach(entityManager::persist);
        }
    }

    private void lagreRefusjonOverstyring(BeregningRefusjonOverstyringerEntitet beregningAktivitetOverstyringer) {
        entityManager.persist(beregningAktivitetOverstyringer);
        beregningAktivitetOverstyringer.getRefusjonOverstyringer().forEach(entityManager::persist);

    }

    private void lagreBeregningAktivitetAggregat(BeregningAktivitetAggregatEntitet aggregat) {
        BeregningAktivitetAggregatEntitet entitet = aggregat;
        if (entitet.getId() == null) {
            entityManager.persist(entitet);
            entitet.getBeregningAktiviteter().forEach(entityManager::persist);
        }
    }

    private void lagreSammenligningsgrunnlagPrStatus(SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus) {
        if (sammenligningsgrunnlagPrStatus.getId() == null) {
            entityManager.persist(sammenligningsgrunnlagPrStatus);
        }
    }

    private BeregningsgrunnlagGrunnlagBuilder opprettGrunnlagBuilderFor(Long behandlingId) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlag = entitetOpt.isPresent() ? Optional.of(entitetOpt.get()) : Optional.empty();
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(grunnlag);
    }

    private BeregningsgrunnlagGrunnlagBuilder opprettGrunnlagBuilderForEndring(Long behandlingId) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlag = entitetOpt.isPresent() ? Optional.of(entitetOpt.get()) : Optional.empty();
        return BeregningsgrunnlagGrunnlagBuilder.endre(grunnlag);
    }


    public void deaktiverBeregningsgrunnlagGrunnlagEntitet(Long behandlingId) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        entitetOpt.ifPresent(this::deaktiverBeregningsgrunnlagGrunnlagEntitet);
    }

    private void deaktiverBeregningsgrunnlagGrunnlagEntitet(BeregningsgrunnlagGrunnlagEntitet entitet) {
        setAktivOgLagre(entitet, false);
    }

    private void setAktivOgLagre(BeregningsgrunnlagGrunnlagEntitet entitet, boolean aktiv) {
        entitet.setAktiv(aktiv);
        entityManager.persist(entitet);
        entityManager.flush();
    }

    public boolean reaktiverBeregningsgrunnlagGrunnlagEntitet(Long behandlingId, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> aktivEntitetOpt = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        aktivEntitetOpt.ifPresent(this::deaktiverBeregningsgrunnlagGrunnlagEntitet);
        Optional<BeregningsgrunnlagGrunnlagEntitet> kontrollerFaktaEntitetOpt = hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, beregningsgrunnlagTilstand);
        boolean reaktiverer = kontrollerFaktaEntitetOpt.isPresent();
        kontrollerFaktaEntitetOpt.ifPresent(entitet -> setAktivOgLagre(entitet, true));
        entityManager.flush();
        return reaktiverer;
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> beregningsgrunnlag = hentBeregningsgrunnlagGrunnlagEntitet(gammelBehandlingId);
        beregningsgrunnlag.ifPresent(orig -> lagre(nyBehandlingId, BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.of(Kopimaskin.deepCopy(orig))), beregningsgrunnlagTilstand));
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentBeregningsgrunnlagForPreutfylling(Long behandlingId, Optional<Long> originalBehandlingId,
                                                                                             BeregningsgrunnlagTilstand forrigeTilstand, BeregningsgrunnlagTilstand nesteTilstand) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> sisteBeregningsgrunnlag = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);

        if (sisteBeregningsgrunnlag.isEmpty()) {
            return Optional.empty();
        }

        BeregningsgrunnlagTilstand gjeldendeTilstand = sisteBeregningsgrunnlag.get().getBeregningsgrunnlagTilstand();

        if (gjeldendeTilstand.erFør(nesteTilstand) || gjeldendeTilstand.equals(nesteTilstand)) {
            return sisteBeregningsgrunnlag;
        }

        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagMedNesteTilstandOpt = hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, nesteTilstand);
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagMedForrigeTiltandOpt = hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, forrigeTilstand);

        if (grunnlagMedForrigeTiltandOpt.isEmpty() && originalBehandlingId.isPresent()) {
            grunnlagMedForrigeTiltandOpt = hentSisteBeregningsgrunnlagGrunnlagEntitet(originalBehandlingId.get(), forrigeTilstand);
        }

        if (grunnlagMedNesteTilstandOpt.isEmpty()) {
            return grunnlagMedForrigeTiltandOpt;
        }

        BeregningsgrunnlagGrunnlagEntitet forrigeTilstandGrunnlag = grunnlagMedForrigeTiltandOpt
            .orElseThrow(() -> new IllegalArgumentException("Mangler beregningsgrunnlag med tilstand: " + forrigeTilstand + " for behandling " + behandlingId));

        LocalDateTime nesteTilstandOpprettetTidspunkt = grunnlagMedNesteTilstandOpt.get().getOpprettetTidspunkt();
        LocalDateTime forrigeTilstandOpprettetTitdspunkt = forrigeTilstandGrunnlag.getOpprettetTidspunkt();

        if (nesteTilstandOpprettetTidspunkt.isAfter(forrigeTilstandOpprettetTitdspunkt)) {
            return grunnlagMedNesteTilstandOpt;
        }

        return grunnlagMedForrigeTiltandOpt;
    }
}

