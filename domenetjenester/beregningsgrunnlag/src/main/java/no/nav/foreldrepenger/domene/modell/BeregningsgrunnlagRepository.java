package no.nav.foreldrepenger.domene.modell;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.domene.modell.sporing.KopierRegelsporing;


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
        var sisteBg = hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, beregningsgrunnlagTilstand);
        if (!sisteBg.isPresent() && originalBehandlingId.isPresent()) {
            return hentSisteBeregningsgrunnlagGrunnlagEntitet(originalBehandlingId.get(), beregningsgrunnlagTilstand);
        }
        return sisteBg;
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlingerEtterTidspunkt(Long behandlingId, Optional<Long> originalBehandlingId,
                                                                                                                 LocalDateTime opprettetEtter,
                                                                                                                 BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        var sisteBg = hentSisteBeregningsgrunnlagGrunnlagEntitetOpprettetEtter(behandlingId, opprettetEtter, beregningsgrunnlagTilstand);
        if (!sisteBg.isPresent() && originalBehandlingId.isPresent()) {
            return hentSisteBeregningsgrunnlagGrunnlagEntitetOpprettetEtter(originalBehandlingId.get(), opprettetEtter, beregningsgrunnlagTilstand);
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
        var query = entityManager.createQuery(
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
        var query = entityManager.createQuery(
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
        var query = entityManager.createQuery(
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
     * Henter siste {@link BeregningsgrunnlagGrunnlagEntitet} opprettet i et bestemt steg etter et gitt tidspunkt. Ignorerer om grunnlaget er aktivt eller ikke.
     * @param behandlingId en behandlingId
     * @param opprettetEtter tidligeste tidspunkt for oppprettelse
     * @param beregningsgrunnlagTilstand steget {@link BeregningsgrunnlagGrunnlagEntitet} er opprettet i
     * @return Hvis det finnes et eller fler BeregningsgrunnlagGrunnlagEntitet som har blitt opprettet i {@code stegOpprettet} returneres den som ble opprettet sist
     */
    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentSisteBeregningsgrunnlagGrunnlagEntitetOpprettetEtter(Long behandlingId, LocalDateTime opprettetEtter,
                                                                                                                BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        var query = entityManager.createQuery(
            "from BeregningsgrunnlagGrunnlagEntitet " +
                "where behandlingId=:behandlingId " +
                "and beregningsgrunnlagTilstand = :beregningsgrunnlagTilstand " +
                "and opprettetTidspunkt > :opprettetTidspunktMin " +
                "order by opprettetTidspunkt desc, id desc", BeregningsgrunnlagGrunnlagEntitet.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_ID, behandlingId); //$NON-NLS-1$
        query.setParameter(BEREGNINGSGRUNNLAG_TILSTAND, beregningsgrunnlagTilstand); //$NON-NLS-1$
        query.setParameter("opprettetTidspunktMin", opprettetEtter); //$NON-NLS-1$
        query.setMaxResults(1);
        return query.getResultStream().findFirst();
    }

    /**
     *
     * For analysering av meldekortfeil og opprydning av disse
     */
    public List<BeregningsgrunnlagGrunnlagEntitet> hentGrunnlagForPotensielleFeilMeldekort() {
        var query = entityManager.createQuery(
            "select gr from BeregningsgrunnlagGrunnlagEntitet gr " +
                "INNER JOIN Beregningsgrunnlag bg ON gr.beregningsgrunnlag = bg " +
                "INNER JOIN BeregningsgrunnlagAktivitetStatus aks ON aks.beregningsgrunnlag = bg " +
                "where gr.opprettetTidspunkt > :opprettetFom " +
                "and gr.opprettetTidspunkt < :opprettetTom " +
                "and gr.beregningsgrunnlagTilstand = :beregningsgrunnlagTilstand " +
                "and (aks.aktivitetStatus = :status1 OR aks.aktivitetStatus = :status2) " +
                "and gr.aktiv = :aktivt", BeregningsgrunnlagGrunnlagEntitet.class); //$NON-NLS-1$

        var beregningsgrunnlagTilstand = BeregningsgrunnlagTilstand.FASTSATT;
        var opprettetFom = LocalDateTime.of(LocalDate.of(2020, 2, 10), LocalTime.NOON);
        var opprettetTom = LocalDateTime.of(LocalDate.of(2020, 3, 10), LocalTime.NOON);
        query.setParameter("opprettetFom", opprettetFom); //$NON-NLS-1$
        query.setParameter("opprettetTom", opprettetTom); //$NON-NLS-1$
        query.setParameter("aktivt", true); //$NON-NLS-1$
        query.setParameter("status1", no.nav.foreldrepenger.domene.modell.AktivitetStatus.ARBEIDSAVKLARINGSPENGER); //$NON-NLS-1$
        query.setParameter("status2", no.nav.foreldrepenger.domene.modell.AktivitetStatus.DAGPENGER); //$NON-NLS-1$
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
        var query = entityManager.createNativeQuery(
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
        var query = entityManager.createQuery("from BeregningSats where satsType=:satsType" + //$NON-NLS-1$
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

        var builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medBeregningsgrunnlag(beregningsgrunnlag);
        var grunnlagEntitet = builder.build(behandlingId, beregningsgrunnlagTilstand);
        lagreOgFlush(behandlingId, grunnlagEntitet);
        return grunnlagEntitet;
    }

    public BeregningsgrunnlagGrunnlagEntitet lagre(Long behandlingId, BeregningsgrunnlagGrunnlagBuilder builder, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(builder, BUILDER);
        Objects.requireNonNull(beregningsgrunnlagTilstand, BEREGNINGSGRUNNLAG_TILSTAND);
        var grunnlagEntitet = builder.build(behandlingId, beregningsgrunnlagTilstand);
        lagreOgFlush(behandlingId, grunnlagEntitet);
        return grunnlagEntitet;
    }

    public void lagreSaksbehandledeAktiviteter(Long behandlingId,
                                               BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                               BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(beregningAktivitetAggregat, BEREGNING_AKTIVITET_AGGREGAT);
        Objects.requireNonNull(beregningsgrunnlagTilstand, BEREGNINGSGRUNNLAG_TILSTAND);

        var builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medSaksbehandletAktiviteter(beregningAktivitetAggregat);
        lagreOgFlush(behandlingId, builder.build(behandlingId, beregningsgrunnlagTilstand));
    }

    public void lagre(Long behandlingId, BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringer) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(beregningAktivitetOverstyringer, BEREGNING_AKTIVITET_OVERSTYRING);
        var beregningsgrunnlagTilstand = BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER;

        var builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medOverstyring(beregningAktivitetOverstyringer);
        lagreOgFlush(behandlingId, builder.build(behandlingId, beregningsgrunnlagTilstand));
    }

    public void lagre(Long behandlingId, BeregningRefusjonOverstyringerEntitet beregningRefusjonOverstyringer) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        Objects.requireNonNull(beregningRefusjonOverstyringer, BEREGNING_REFUSJON_OVERSTYRINGER);
        var beregningsgrunnlagTilstand = BeregningsgrunnlagTilstand.KOFAKBER_UT;
        var builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medRefusjonOverstyring(beregningRefusjonOverstyringer);
        lagreOgFlush(behandlingId, builder.build(behandlingId, beregningsgrunnlagTilstand));
    }

    private void lagreOgFlush(Long behandlingId, BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID);
        if (erLagret(nyttGrunnlag)) {
            throw new IllegalStateException("Kan ikke lagre ned et allerede lagret grunnlag.");
        }
        var tidligereAggregat = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        if (tidligereAggregat.isPresent()) {
            if (tidligereAggregat.get().getBeregningsgrunnlagTilstand().erFør(nyttGrunnlag.getBeregningsgrunnlagTilstand())) {
                KopierRegelsporing.kopierRegelsporingerTilGrunnlag(nyttGrunnlag, tidligereAggregat);
                // Kopierer besteberegning
                tidligereAggregat.get().getBeregningsgrunnlag()
                    .flatMap(BeregningsgrunnlagEntitet::getBesteberegninggrunnlag)
                    .map(BesteberegninggrunnlagEntitet::new)
                    .ifPresent(bbGrunnlag -> nyttGrunnlag.getBeregningsgrunnlag().ifPresent(bg -> bg.setBesteberegninggrunnlag(bbGrunnlag)));
            }
            tidligereAggregat.get().setAktiv(false);
            entityManager.persist(tidligereAggregat.get());
        }
        lagreGrunnlag(nyttGrunnlag);
        entityManager.flush();
    }

    private boolean erLagret(BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag) {
        return nyttGrunnlag.getId() != null;
    }

    private void lagreGrunnlag(BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag) {
        var registerAktiviteter = nyttGrunnlag.getRegisterAktiviteter();
        if (registerAktiviteter != null) {
            lagreBeregningAktivitetAggregat(registerAktiviteter);
        }
        nyttGrunnlag.getSaksbehandletAktiviteter().ifPresent(this::lagreBeregningAktivitetAggregat);
        nyttGrunnlag.getOverstyring()
            .ifPresent(this::lagreOverstyring);
        nyttGrunnlag.getBeregningsgrunnlag().ifPresent(entityManager::persist);
        nyttGrunnlag.getRefusjonOverstyringer()
            .ifPresent(this::lagreRefusjonOverstyringer);

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

    private void lagreRefusjonOverstyringer(BeregningRefusjonOverstyringerEntitet beregningAktivitetOverstyringer) {
        entityManager.persist(beregningAktivitetOverstyringer);
        beregningAktivitetOverstyringer.getRefusjonOverstyringer().forEach(this::lagreRefusjonOverstyring);
    }

    private void lagreRefusjonOverstyring(BeregningRefusjonOverstyringEntitet beregningAktivitetOverstyring) {
        entityManager.persist(beregningAktivitetOverstyring);
        beregningAktivitetOverstyring.getRefusjonPerioder().forEach(entityManager::persist);
    }


    private void lagreBeregningAktivitetAggregat(BeregningAktivitetAggregatEntitet aggregat) {
        if (aggregat.getId() == null) {
            entityManager.persist(aggregat);
            aggregat.getBeregningAktiviteter().forEach(entityManager::persist);
        }
    }

    private void lagreSammenligningsgrunnlagPrStatus(SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus) {
        if (sammenligningsgrunnlagPrStatus.getId() == null) {
            entityManager.persist(sammenligningsgrunnlagPrStatus);
        }
    }

    private BeregningsgrunnlagGrunnlagBuilder opprettGrunnlagBuilderFor(Long behandlingId) {
        var entitetOpt = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlag = entitetOpt.isPresent() ? Optional.of(entitetOpt.get()) : Optional.empty();
        return BeregningsgrunnlagGrunnlagBuilder.kopi(grunnlag);
    }

    public void deaktiverBeregningsgrunnlagGrunnlagEntitet(Long behandlingId) {
        var entitetOpt = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
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
        var aktivEntitetOpt = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        aktivEntitetOpt.ifPresent(this::deaktiverBeregningsgrunnlagGrunnlagEntitet);
        var kontrollerFaktaEntitetOpt = hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, beregningsgrunnlagTilstand);
        var reaktiverer = kontrollerFaktaEntitetOpt.isPresent();
        kontrollerFaktaEntitetOpt.ifPresent(entitet -> setAktivOgLagre(entitet, true));
        entityManager.flush();
        return reaktiverer;
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        var beregningsgrunnlag = hentBeregningsgrunnlagGrunnlagEntitet(gammelBehandlingId);
        beregningsgrunnlag.ifPresent(orig -> lagre(nyBehandlingId, BeregningsgrunnlagGrunnlagBuilder.kopi(orig), beregningsgrunnlagTilstand));
    }

    public boolean oppdaterGrunnlagMedGrunnbeløp(Long gammelBehandlingId, Long nyBehandlingId, BeregningsgrunnlagTilstand tilstand) {
        var beregningsgrunnlag = hentSisteBeregningsgrunnlagGrunnlagEntitet(gammelBehandlingId, tilstand);
        if (beregningsgrunnlag.isPresent()) {
            if (beregningsgrunnlag.get().getBeregningsgrunnlag().isPresent()) {
                var bg = beregningsgrunnlag.get().getBeregningsgrunnlag().orElseThrow(() -> new IllegalStateException("Skal ha BG"));
                var beregningSats = finnEksaktSats(BeregningSatsType.GRUNNBELØP, bg.getSkjæringstidspunkt());
                lagre(nyBehandlingId, BeregningsgrunnlagGrunnlagBuilder.kopi(beregningsgrunnlag.get())
                    .medBeregningsgrunnlag(BeregningsgrunnlagEntitet.builder(bg).medGrunnbeløp(BigDecimal.valueOf(beregningSats.getVerdi())).build()), tilstand);
            } else {
                lagre(nyBehandlingId, BeregningsgrunnlagGrunnlagBuilder.kopi(beregningsgrunnlag), tilstand);
            }
            return true;
        }
        return false;
    }

    /**
     *
     * @param behandlingId id på nåværende behandling
     * @param originalBehandlingId id på orginal behandling, om den finnes.
     * @param forrigeTilstand tilstanden før aksjonspunkt er løst (altså tilstanden som returneres fra steget)
     * @param nesteTilstand tilstanden etter at aksjonspunktet er løst.
     * @return finner grunnlaget som skal brukes til preutfylling i GUI. Enten det forrige grunnlaget som ble
     * hadde denne tilstanden eller grunlaget i orginalbehandlingen som hadde denne tilstanden om det finnes.
     */
    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentBeregningsgrunnlagForPreutfylling(Long behandlingId, Optional<Long> originalBehandlingId,
                                                                                             BeregningsgrunnlagTilstand forrigeTilstand, BeregningsgrunnlagTilstand nesteTilstand) {
        var sisteBeregningsgrunnlag = hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);

        if (sisteBeregningsgrunnlag.isEmpty()) {
            return Optional.empty();
        }

        var gjeldendeTilstand = sisteBeregningsgrunnlag.get().getBeregningsgrunnlagTilstand();

        if (gjeldendeTilstand.erFør(forrigeTilstand)) {
            return Optional.empty();
        }

        if (gjeldendeTilstand.equals(forrigeTilstand) || gjeldendeTilstand.equals(nesteTilstand)) {
            return sisteBeregningsgrunnlag;
        }

        var grunnlagMedNesteTilstandOpt = hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, nesteTilstand);
        var grunnlagMedForrigeTiltandOpt = hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, forrigeTilstand);

        if (grunnlagMedForrigeTiltandOpt.isEmpty() && originalBehandlingId.isPresent()) {
            grunnlagMedForrigeTiltandOpt = hentSisteBeregningsgrunnlagGrunnlagEntitet(originalBehandlingId.get(), forrigeTilstand);
        }

        if (grunnlagMedNesteTilstandOpt.isEmpty()) {
            return grunnlagMedForrigeTiltandOpt;
        }

        var forrigeTilstandGrunnlag = grunnlagMedForrigeTiltandOpt
            .orElseThrow(() -> new IllegalArgumentException("Mangler beregningsgrunnlag med tilstand: " + forrigeTilstand + " for behandling " + behandlingId));

        var nesteTilstandOpprettetTidspunkt = grunnlagMedNesteTilstandOpt.get().getOpprettetTidspunkt();
        var forrigeTilstandOpprettetTitdspunkt = forrigeTilstandGrunnlag.getOpprettetTidspunkt();

        if (nesteTilstandOpprettetTidspunkt.isAfter(forrigeTilstandOpprettetTitdspunkt)) {
            return grunnlagMedNesteTilstandOpt;
        }

        return grunnlagMedForrigeTiltandOpt;
    }
}

