package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class BehandlingRevurderingRepository {

    private static final String AVSLUTTET_KEY = "avsluttet";

    private static final List<String> RES_TYPER_REGULERING = List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.FORELDREPENGER_ENDRET.getKode(),
        BehandlingResultatType.INGEN_ENDRING.getKode(), BehandlingResultatType.OPPHØR.getKode());
    private static final List<String> STATUS_FERDIG = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
    private static final List<String> YTELSE_TYPER = BehandlingType.getYtelseBehandlingTyper().stream().map(BehandlingType::getKode).collect(Collectors.toList());

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private SøknadRepository søknadRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    BehandlingRevurderingRepository() {
    }

    @Inject
    public BehandlingRevurderingRepository( EntityManager entityManager,
                                               BehandlingRepository behandlingRepository,
                                               FagsakRelasjonRepository fagsakRelasjonRepository,
                                               SøknadRepository søknadRepository,
                                               BehandlingLåsRepository behandlingLåsRepository) {

        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository);
        this.fagsakRelasjonRepository = Objects.requireNonNull(fagsakRelasjonRepository);
        this.søknadRepository = Objects.requireNonNull(søknadRepository);
        this.behandlingLåsRepository = Objects.requireNonNull(behandlingLåsRepository);
    }

    EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Hent første henlagte endringssøknad etter siste innvilgede behandlinger for en fagsak
     */
    public List<Behandling> finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, "fagsakId"); // NOSONAR //$NON-NLS-1$

        var sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);

        if (sisteInnvilgede.isPresent()) {
            final var behandlingsIder = finnHenlagteBehandlingerEtter(fagsakId, sisteInnvilgede.get());
            for (var behandlingId : behandlingsIder) {
                behandlingLåsRepository.taLås(behandlingId);
            }
            return behandlingsIder.stream()
                .map(behandlingId -> behandlingRepository.hentBehandling(behandlingId))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Optional<Behandling> hentAktivIkkeBerørtEllerSisteYtelsesbehandling(Long fagsakId) {
        // Det kan ligge avsluttet berørt opprettet senere enn åpen behandling
        return finnÅpenogKøetYtelsebehandling(fagsakId).stream()
            .filter(SpesialBehandling::erIkkeSpesialBehandling)
            .findFirst()
            .or(() -> hentSisteYtelsesbehandling(fagsakId));
    }

    private Optional<Behandling> hentSisteYtelsesbehandling(Long fagsakId) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
    }

    private List<Long> finnHenlagteBehandlingerEtter(Long fagsakId, Behandling sisteInnvilgede) {
        var query = getEntityManager().createQuery("""
            SELECT b.id FROM Behandling b WHERE b.fagsak.id=:fagsakId
             AND b.behandlingType=:type
             AND b.opprettetTidspunkt >= to_timestamp(:etterTidspunkt)
             AND EXISTS (SELECT r FROM Behandlingsresultat r
                WHERE r.behandling=b
                AND r.behandlingResultatType IN :henlagtKoder)
             ORDER BY b.opprettetTidspunkt ASC
            """, Long.class);
        query.setParameter("fagsakId", fagsakId); //$NON-NLS-1$
        query.setParameter("type", BehandlingType.REVURDERING);
        query.setParameter("henlagtKoder", BehandlingResultatType.getAlleHenleggelseskoder());
        query.setParameter("etterTidspunkt", sisteInnvilgede.getOpprettetDato());
        return query.getResultList();
    }

    public Optional<Behandling> finnÅpenYtelsesbehandling(Long fagsakId) {
        var åpenBehandling = finnÅpenogKøetYtelsebehandling(fagsakId).stream()
            .filter(beh -> !beh.erKøet())
            .collect(Collectors.toList());
        check(åpenBehandling.size() <= 1, "Kan maks ha én åpen ytelsesbehandling"); //$NON-NLS-1$
        return optionalFirst(åpenBehandling);
    }

    public Optional<Behandling> finnKøetYtelsesbehandling(Long fagsakId) {
        var køetBehandling = finnÅpenogKøetYtelsebehandling(fagsakId).stream()
            .filter(Behandling::erKøet)
            .collect(Collectors.toList());
        check(køetBehandling.size() <= 1, "Kan maks ha én køet ytelsesbehandling"); //$NON-NLS-1$
        return optionalFirst(køetBehandling);
    }

    private List<Behandling> finnÅpenogKøetYtelsebehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, "fagsakId"); // NOSONAR //$NON-NLS-1$

        var query = getEntityManager().createQuery(
            "SELECT b.id " +
                "from Behandling b " +
                "where fagsak.id=:fagsakId " +
                "and status not in (:avsluttet) " +
                "and behandlingType in (:behandlingType) " +
                "order by opprettetTidspunkt desc", //$NON-NLS-1$
            Long.class);
        query.setParameter("fagsakId", fagsakId); //$NON-NLS-1$
        query.setParameter(AVSLUTTET_KEY, BehandlingStatus.getFerdigbehandletStatuser()); // $NON-NLS-1$
        query.setParameter("behandlingType", BehandlingType.getYtelseBehandlingTyper()); //$NON-NLS-1$

        var behandlingIder = query.getResultList();
        for (var behandlingId : behandlingIder) {
            behandlingLåsRepository.taLås(behandlingId);
        }
        final var behandlinger = behandlingIder.stream()
            .map(behandlingId -> behandlingRepository.hentBehandling(behandlingId))
            .collect(Collectors.toList());
        check(behandlinger.size() <= 2, "Kan maks ha én åpen og én køet ytelsesbehandling"); //$NON-NLS-1$
        check(behandlinger.stream().filter(Behandling::erKøet).count() <= 1, "Kan maks ha én køet ytelsesbehandling"); //$NON-NLS-1$
        check(behandlinger.stream().filter(it -> !it.erKøet()).count() <= 1, "Kan maks ha én åpen ytelsesbehandling"); //$NON-NLS-1$

        return behandlinger;
    }

    public Optional<Behandling> finnKøetBehandlingMedforelder(Fagsak fagsak) {
        return finnFagsakPåMedforelder(fagsak).flatMap(fs -> finnKøetYtelsesbehandling(fs.getId()));
    }

    public Optional<Fagsak> finnFagsakPåMedforelder(Fagsak fagsak) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak).flatMap(fr -> fr.getRelatertFagsak(fagsak));
    }

    public Optional<Behandling> finnSisteVedtatteIkkeHenlagteBehandlingForMedforelder(Fagsak fagsak) {
        return finnFagsakPåMedforelder(fagsak).flatMap(fs -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fs.getId()));
    }

    public Optional<Behandling> finnSisteInnvilgetBehandlingForMedforelder(Fagsak fagsak) {
        return finnFagsakPåMedforelder(fagsak).flatMap(fs -> behandlingRepository.finnSisteInnvilgetBehandling(fs.getId()));
    }

    public Optional<LocalDate> finnSøknadsdatoFraHenlagtBehandling(Behandling behandling) {
        var henlagteBehandlinger = finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(behandling.getFagsak().getId());
        var søknad = finnFørsteSøknadBlantBehandlinger(henlagteBehandlinger);
        if (søknad.isPresent()) {
            return Optional.ofNullable(søknad.get().getSøknadsdato());
        }
        return Optional.empty();
    }

    private Optional<SøknadEntitet> finnFørsteSøknadBlantBehandlinger(List<Behandling> behandlinger) {
        return behandlinger.stream()
            .map(behandling -> søknadRepository.hentSøknadHvisEksisterer(behandling.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private static void check(boolean check, String message) {
        if (!check) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Optional<Behandling> optionalFirst(List<Behandling> behandlinger) {
        return behandlinger.isEmpty() ? Optional.empty() : Optional.of(behandlinger.get(0));
    }

    private static final String SØKERE_MED_FLERE_SVP_SAKER = """
        select u.aktoer_id
        from bruker u join fagsak f on f.bruker_id=u.id
        where ytelse_type=:ytelse
        group by u.aktoer_id
        having count(distinct saksnummer) > 1
        """;

    private static final String REGULERING_SELECT_STD_FP = """
        SELECT DISTINCT f.id , bru.aktoer_id
          from Fagsak f join bruker bru on f.bruker_id=bru.id
          join behandling b on b.fagsak_id=f.id
          join behandling_resultat br on (br.behandling_id=b.id and br.behandling_resultat_type in (:restyper))
          join (select beh.fagsak_id fsmax, max(brsq.opprettet_tid) maxbr from behandling beh
                join behandling_resultat brsq on (brsq.behandling_id=beh.id and brsq.behandling_resultat_type in (:restyper))
                where beh.behandling_type in (:ytelse) and beh.behandling_status in (:avsluttet)
                group by beh.fagsak_id) on (fsmax=b.fagsak_id and br.opprettet_tid = maxbr)
          join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=b.id and grbg.aktiv = 'J')
          join BEREGNINGSGRUNNLAG bglag on grbg.beregningsgrunnlag_id=bglag.id
          join br_sats sats on (sats.verdi = bglag.grunnbeloep and sats_type=:grunnbelop)
          join (select ur.behandling_resultat_id bruttak, min(per.fom) uttakfom
            from fpsak.uttak_resultat_periode per
            join fpsak.uttak_resultat ur on per.uttak_resultat_perioder_id = nvl(ur.overstyrt_perioder_id,ur.opprinnelig_perioder_id)
            where ur.aktiv = 'J'
            and (per.PERIODE_RESULTAT_AARSAK = :sokfrist or per.PERIODE_RESULTAT_TYPE = :utinnvilg)
            group by ur.behandling_resultat_id) futtak on (futtak.bruttak = br.id)
        """;

    private static final String REGULERING_WHERE_STD_FP = """
        where b.behandling_status in (:avsluttet) and b.behandling_type in (:ytelse)
          and futtak.uttakfom > sats.tom
          and futtak.uttakfom >= :fomdato
          and f.id not in ( select beh.fagsak_id from behandling beh
            where beh.behandling_status not in (:avsluttet) and beh.behandling_type in (:ytelse)
              and beh.id not in (select ba.behandling_id from behandling_arsak ba where behandling_arsak_type in (:berort)) )
        """;

    /** Plukker ut aktørId på søkere med flere SVP saker */
    public List<AktørId> finnAktørerMedFlereSVPSaker() {
        var query = getEntityManager().createNativeQuery(SØKERE_MED_FLERE_SVP_SAKER).setParameter("ytelse", "SVP");
        @SuppressWarnings("unchecked")
        List<String> resultatList = query.getResultList();
        return resultatList.stream().map(AktørId::new).collect(Collectors.toList());
    }

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering over 6G og det ikke finnes åpen behandling */
    public List<FagsakIdAktørId> finnSakerMedBehovForGrunnbeløpRegulering(LocalDate gjeldendeFom, long forrigeAvkortingMultiplikator) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, brutto overstiger 6G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(
            REGULERING_SELECT_STD_FP + """
                    join (select beregningsgrunnlag_id bgid, max(brutto_pr_aar) brutto
                        from BEREGNINGSGRUNNLAG_PERIODE
                        group by beregningsgrunnlag_id
                    ) bgmax on bgmax.bgid=grbg.beregningsgrunnlag_id
                    """ +
                REGULERING_WHERE_STD_FP +
                "  and bgmax.brutto >= (bglag.grunnbeloep * :avkorting ) ")
            .setParameter("avkorting", forrigeAvkortingMultiplikator);
        setStandardParametersFP(query, gjeldendeFom);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdAktørId(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering (MS under 3G) og det ikke finnes åpen behandling */
    public List<FagsakIdAktørId> finnSakerMedBehovForMilSivRegulering(LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, status MS, brutto understiger 3G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(
            REGULERING_SELECT_STD_FP + """
                  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:milsiv) )
                  join (select beregningsgrunnlag_id bgid, min(brutto_pr_aar) brutto
                        from BEREGNINGSGRUNNLAG_PERIODE
                        group by beregningsgrunnlag_id
                    ) bgmin on bgmin.bgid=grbg.beregningsgrunnlag_id
                """ +
                REGULERING_WHERE_STD_FP +
                "  and bgmin.brutto <= (bglag.grunnbeloep * :avkorting ) " )
            .setParameter("avkorting", 3)
            .setParameter("milsiv", List.of(AktivitetStatus.MILITÆR_ELLER_SIVIL.getKode()));
        setStandardParametersFP(query, gjeldendeFom);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdAktørId(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering (SN og kombinasjon) og det ikke finnes åpen behandling */
    public List<FagsakIdAktørId> finnSakerMedBehovForNæringsdrivendeRegulering(LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, status SN/KOMB og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(
            REGULERING_SELECT_STD_FP +
                "  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:snring) ) " +
                REGULERING_WHERE_STD_FP)
            .setParameter("snring", List.of(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.getKode(), AktivitetStatus.KOMBINERT_AT_SN.getKode(),
                AktivitetStatus.KOMBINERT_FL_SN.getKode(), AktivitetStatus.KOMBINERT_AT_FL_SN.getKode()));
        setStandardParametersFP(query, gjeldendeFom);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdAktørId(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    /** Liste av fagsakId, aktørId for saker som trenger Arena-regulering og det ikke finnes åpen behandling */
    public List<FagsakIdAktørId> finnSakerMedBehovForArenaRegulering(LocalDate gjeldendeFom, LocalDate nySatsDato) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som er beregnet med AAP/DP og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         * OBS på regulering utenom G-sato - da trengs en fom-dato og kriteriet "and futtak.uttakfom >= input-fom" (isf > sats.tom)
         */
        var query = getEntityManager().createNativeQuery(
            REGULERING_SELECT_STD_FP +
                "  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:asarena) ) " +
                REGULERING_WHERE_STD_FP +
                "and nvl(b.sist_oppdatert_tidspunkt, b.opprettet_tid) < :satsdato ")
            .setParameter("satsdato", nySatsDato)
            .setParameter("asarena", List.of(AktivitetStatus.ARBEIDSAVKLARINGSPENGER.getKode(), AktivitetStatus.DAGPENGER.getKode()));
        setStandardParametersFP(query, gjeldendeFom);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdAktørId(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    private void setStandardParametersFP(Query query, LocalDate gjeldendeFom) {
        query.setParameter("restyper", RES_TYPER_REGULERING)
            .setParameter(AVSLUTTET_KEY, STATUS_FERDIG)
            .setParameter("fomdato", gjeldendeFom)
            .setParameter("ytelse", YTELSE_TYPER)
            .setParameter("berort", BehandlingÅrsakType.alleTekniskeÅrsaker().stream().map(BehandlingÅrsakType::getKode).collect(Collectors.toList()))
            .setParameter("grunnbelop", BeregningSatsType.GRUNNBELØP.getKode())
            .setParameter("sokfrist", PeriodeResultatÅrsak.SØKNADSFRIST.getKode())
            .setParameter("utinnvilg", PeriodeResultatType.INNVILGET.getKode());
    }

    private static final String REGULERING_SELECT_STD_SVP = """
        SELECT DISTINCT f.id , bru.aktoer_id
          from Fagsak f join bruker bru on f.bruker_id=bru.id
          join behandling b on b.fagsak_id=f.id
          join behandling_resultat br on (br.behandling_id=b.id and br.behandling_resultat_type in (:restyper))
          join (select beh.fagsak_id fsmax, max(brsq.opprettet_tid) maxbr from behandling beh
                join behandling_resultat brsq on (brsq.behandling_id=beh.id and brsq.behandling_resultat_type in (:restyper))
                where beh.behandling_type in (:ytelse) and beh.behandling_status in (:avsluttet)
                group by beh.fagsak_id) on (fsmax=b.fagsak_id and br.opprettet_tid = maxbr)
          join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=b.id and grbg.aktiv = 'J')
          join BEREGNINGSGRUNNLAG bglag on grbg.beregningsgrunnlag_id=bglag.id
          join br_sats sats on (sats.verdi = bglag.grunnbeloep and sats_type = :grunnbelop)
          join BR_RESULTAT_BEHANDLING grbr on (grbr.behandling_id=b.id and grbr.aktiv = 'J')
          join (select BEREGNINGSRESULTAT_FP_ID brfpid, min(BR_PERIODE_FOM) fom
                from br_periode brp left join br_andel bra on bra.br_periode_id = brp.id
                where (dagsats>0) group by BEREGNINGSRESULTAT_FP_ID
               ) brnetto on brnetto.brfpid=grbr.BG_BEREGNINGSRESULTAT_FP_ID
        """;

    private static final String REGULERING_WHERE_STD_SVP = """
        where b.behandling_status in (:avsluttet) and b.behandling_type in (:ytelse)
          and brnetto.fom > sats.tom
          and brnetto.fom >= :fomdato
          and f.ytelse_type = :svpytelse
          and f.id not in ( select beh.fagsak_id from behandling beh
            where beh.behandling_status not in (:avsluttet) and beh.behandling_type in (:ytelse) )
        """;

    private void setStandardParametersSVP(Query query, LocalDate gjeldendeFom) {
        query.setParameter("restyper", RES_TYPER_REGULERING)
            .setParameter(AVSLUTTET_KEY, STATUS_FERDIG)
            .setParameter("fomdato", gjeldendeFom)
            .setParameter("ytelse", YTELSE_TYPER)
            .setParameter("svpytelse", FagsakYtelseType.SVANGERSKAPSPENGER.getKode())
            .setParameter("grunnbelop", BeregningSatsType.GRUNNBELØP.getKode());
    }

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering over 6G og det ikke finnes åpen behandling */
    public List<FagsakIdAktørId> finnSakerMedBehovForGrunnbeløpReguleringSVP(LocalDate gjeldendeFom, long forrigeAvkortingMultiplikator) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, brutto overstiger 6G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(
            REGULERING_SELECT_STD_SVP + """
                    join (select beregningsgrunnlag_id bgid, max(brutto_pr_aar) brutto
                        from BEREGNINGSGRUNNLAG_PERIODE
                        group by beregningsgrunnlag_id
                    ) bgmax on bgmax.bgid=grbg.beregningsgrunnlag_id
                    """ +
                REGULERING_WHERE_STD_SVP +
                "  and bgmax.brutto >= (bglag.grunnbeloep * :avkorting ) ")
            .setParameter("avkorting", forrigeAvkortingMultiplikator); //$NON-NLS-1$
        setStandardParametersSVP(query, gjeldendeFom);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdAktørId(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering (MS under 3G) og det ikke finnes åpen behandling */
    public List<FagsakIdAktørId> finnSakerMedBehovForMilSivReguleringSVP(LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, status MS, brutto understiger 3G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(
            REGULERING_SELECT_STD_SVP + """
                  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:milsiv) )
                  join (select beregningsgrunnlag_id bgid, min(brutto_pr_aar) brutto
                        from BEREGNINGSGRUNNLAG_PERIODE
                        group by beregningsgrunnlag_id
                    ) bgmin on bgmin.bgid=grbg.beregningsgrunnlag_id
                """ +
                REGULERING_WHERE_STD_SVP +
                "  and bgmin.brutto <= (bglag.grunnbeloep * :avkorting ) " )
            .setParameter("avkorting", 3)
            .setParameter("milsiv", List.of(AktivitetStatus.MILITÆR_ELLER_SIVIL.getKode())); //$NON-NLS-1$
        setStandardParametersSVP(query, gjeldendeFom);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdAktørId(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering (SN og kombinasjon) og det ikke finnes åpen behandling */
    public List<FagsakIdAktørId> finnSakerMedBehovForNæringsdrivendeReguleringSVP(LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, status SN/KOMB og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(
            REGULERING_SELECT_STD_SVP +
                "  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:snring) ) " +
                REGULERING_WHERE_STD_SVP )
            .setParameter("snring", List.of(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.getKode(), AktivitetStatus.KOMBINERT_AT_SN.getKode(),
                AktivitetStatus.KOMBINERT_FL_SN.getKode(), AktivitetStatus.KOMBINERT_AT_FL_SN.getKode())); //$NON-NLS-1$
        setStandardParametersSVP(query, gjeldendeFom);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdAktørId(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    public static record FagsakIdAktørId(Long fagsakId, AktørId aktørId) { }
}
