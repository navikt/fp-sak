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
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.util.Tuple;

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

        Optional<Behandling> sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);

        if (sisteInnvilgede.isPresent()) {
            final List<Long> behandlingsIder = finnHenlagteBehandlingerEtter(fagsakId, sisteInnvilgede.get());
            for (Long behandlingId : behandlingsIder) {
                behandlingLåsRepository.taLås(behandlingId);
            }
            return behandlingsIder.stream()
                .map(behandlingId -> behandlingRepository.hentBehandling(behandlingId))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Optional<Behandling> hentSisteYtelsesbehandling(Long fagsakId) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
    }

    private List<Long> finnHenlagteBehandlingerEtter(Long fagsakId, Behandling sisteInnvilgede) {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT b.id FROM Behandling b WHERE b.fagsak.id=:fagsakId " +
                " AND b.behandlingType=:type" +
                " AND b.opprettetTidspunkt >= to_timestamp(:etterTidspunkt)" +
                " AND EXISTS (SELECT r FROM Behandlingsresultat r" +
                "    WHERE r.behandling=b " +
                "    AND r.behandlingResultatType IN :henlagtKoder)" +
                " ORDER BY b.opprettetTidspunkt ASC", //$NON-NLS-1$
            Long.class);
        query.setParameter("fagsakId", fagsakId); //$NON-NLS-1$
        query.setParameter("type", BehandlingType.REVURDERING);
        query.setParameter("henlagtKoder", BehandlingResultatType.getAlleHenleggelseskoder());
        query.setParameter("etterTidspunkt", sisteInnvilgede.getOpprettetDato());
        return query.getResultList();
    }

    public Optional<Behandling> finnÅpenYtelsesbehandling(Long fagsakId) {
        List<Behandling> åpenBehandling = finnÅpenogKøetYtelsebehandling(fagsakId).stream()
            .filter(beh -> !beh.erKøet())
            .collect(Collectors.toList());
        check(åpenBehandling.size() <= 1, "Kan maks ha én åpen ytelsesbehandling"); //$NON-NLS-1$
        return optionalFirst(åpenBehandling);
    }

    public Optional<Behandling> finnKøetYtelsesbehandling(Long fagsakId) {
        List<Behandling> køetBehandling = finnÅpenogKøetYtelsebehandling(fagsakId).stream()
            .filter(Behandling::erKøet)
            .collect(Collectors.toList());
        check(køetBehandling.size() <= 1, "Kan maks ha én køet ytelsesbehandling"); //$NON-NLS-1$
        return optionalFirst(køetBehandling);
    }

    private List<Behandling> finnÅpenogKøetYtelsebehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, "fagsakId"); // NOSONAR //$NON-NLS-1$

        TypedQuery<Long> query = getEntityManager().createQuery(
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

        List<Long> behandlingIder = query.getResultList();
        for (Long behandlingId : behandlingIder) {
            behandlingLåsRepository.taLås(behandlingId);
        }
        final List<Behandling> behandlinger = behandlingIder.stream()
            .map(behandlingId -> behandlingRepository.hentBehandling(behandlingId))
            .collect(Collectors.toList());
        check(behandlinger.size() <= 2, "Kan maks ha én åpen og én køet ytelsesbehandling"); //$NON-NLS-1$
        check(behandlinger.stream().filter(Behandling::erKøet).count() <= 1, "Kan maks ha én køet ytelsesbehandling"); //$NON-NLS-1$
        check(behandlinger.stream().filter(it -> !it.erKøet()).count() <= 1, "Kan maks ha én åpen ytelsesbehandling"); //$NON-NLS-1$

        return behandlinger;
    }

    public Optional<Behandling> finnÅpenBehandlingMedforelder(Fagsak fagsak) {
        Optional<FagsakRelasjon> fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak);
        if (!fagsakRelasjon.isPresent() || !fagsakRelasjon.get().getFagsakNrTo().isPresent()) {
            return Optional.empty();
        }

        Long fagsakIdEn = fagsakRelasjon.get().getFagsakNrEn().getId();
        Long fagsakIdTo = fagsakRelasjon.get().getFagsakNrTo().get().getId();
        Long fagsakIdMedforelder = fagsakIdEn.equals(fagsak.getId()) ? fagsakIdTo : fagsakIdEn;

        return finnÅpenYtelsesbehandling(fagsakIdMedforelder);
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
        List<Behandling> henlagteBehandlinger = finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(behandling.getFagsak().getId());
        Optional<SøknadEntitet> søknad = finnFørsteSøknadBlantBehandlinger(henlagteBehandlinger);
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

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering over 6G og det ikke finnes åpen behandling */
    public List<Tuple<Long, AktørId>> finnSakerMedBehovForGrunnbeløpRegulering(long forrigeSats, long forrigeAvkortingMultiplikator,
                                                                               LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, brutto overstiger 6G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen avsluttet behandling med ny sats
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         * OBS: De som kun har innvilget utsettelse (ingen utbetaling) får riktig beregning når det kommer endringssøknad med uttak
         */
        Query query = getEntityManager().createNativeQuery(
            "SELECT DISTINCT f.id , bru.aktoer_id " +
                " from Fagsak f join bruker bru on f.bruker_id=bru.id " +
                "  join behandling b on b.fagsak_id=f.id " +
                "  join behandling_resultat br on (br.behandling_id=b.id and br.behandling_resultat_type in (:restyper)) " +
                "  join (select beh.fagsak_id fsmax, max(brsq.opprettet_tid) maxbr from behandling beh  " +
                "        join behandling_resultat brsq on (brsq.behandling_id=beh.id and brsq.behandling_resultat_type in (:restyper)) " +
                "        where beh.behandling_type in (:ytelse) and beh.behandling_status in (:avsluttet) " +
                "        group by beh.fagsak_id) on (fsmax=b.fagsak_id and br.opprettet_tid = maxbr) " +
                "  join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=b.id and grbg.aktiv = 'J') " +
                "  join BEREGNINGSGRUNNLAG bglag on grbg.beregningsgrunnlag_id=bglag.id " +
                "  join (select beregningsgrunnlag_id bgid, max(brutto_pr_aar) brutto " +
                "        from BEREGNINGSGRUNNLAG_PERIODE " +
                "        group by beregningsgrunnlag_id " +
                "    ) bgmax on bgmax.bgid=grbg.beregningsgrunnlag_id " +
                "  join BR_RESULTAT_BEHANDLING grbr on (grbr.behandling_id=b.id and grbr.aktiv = 'J') " +
                "  join (select BEREGNINGSRESULTAT_FP_ID brfpid, min(BR_PERIODE_FOM) fom " +
                "        from br_periode brp  " +
                "        group by BEREGNINGSRESULTAT_FP_ID " +
                "    ) brbrutto on brbrutto.brfpid=grbr.BG_BEREGNINGSRESULTAT_FP_ID " +
                "  join (select BEREGNINGSRESULTAT_FP_ID brfpid, min(BR_PERIODE_FOM) fom " +
                "    from br_periode brp left join br_andel bra on bra.br_periode_id = brp.id " +
                "    where (dagsats>0) " +
                "    group by BEREGNINGSRESULTAT_FP_ID " +
                "  ) brnetto on brnetto.brfpid=grbr.BG_BEREGNINGSRESULTAT_FP_ID  " +
                "where b.behandling_status in (:avsluttet) and b.behandling_type in (:ytelse) " +
                "  and brbrutto.fom is not null and brnetto.fom is not null " +
                "  and brbrutto.fom >= :fomdato  " +
                "  and bgmax.brutto >= (bglag.grunnbeloep * :avkorting ) " +
                "  and bglag.grunnbeloep=:gmlsats  " +
                "  and f.id not in ( select beh.fagsak_id from behandling beh " +
                "    where beh.behandling_status not in (:avsluttet) and beh.behandling_type in (:ytelse)  " +
                "      and beh.id not in (select ba.behandling_id from behandling_arsak ba where behandling_arsak_type=:berort) ) " ); //$NON-NLS-1$
        query.setParameter("fomdato", gjeldendeFom); //$NON-NLS-1$
        query.setParameter("gmlsats", forrigeSats); //$NON-NLS-1$
        query.setParameter("avkorting", forrigeAvkortingMultiplikator); //$NON-NLS-1$
        query.setParameter("restyper", RES_TYPER_REGULERING); //$NON-NLS-1$
        query.setParameter(AVSLUTTET_KEY, STATUS_FERDIG); // $NON-NLS-1$
        query.setParameter("ytelse", YTELSE_TYPER); //$NON-NLS-1$
        query.setParameter("berort", BehandlingÅrsakType.BERØRT_BEHANDLING.getKode()); //$NON-NLS-1$
        query.setMaxResults(1000);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new Tuple<>(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering (MS under 3G) og det ikke finnes åpen behandling */
    public List<Tuple<Long, AktørId>> finnSakerMedBehovForMilSivRegulering(long gjeldendeSats, long forrigeSats, LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, status MS, brutto understiger 3G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen avsluttet behandling med ny sats
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         * OBS: De som kun har innvilget utsettelse (ingen utbetaling) får riktig beregning når det kommer endringssøknad med uttak
         */
        Query query = getEntityManager().createNativeQuery(
            "SELECT DISTINCT f.id , bru.aktoer_id " +
                " from Fagsak f join bruker bru on f.bruker_id=bru.id " +
                "  join behandling b on b.fagsak_id=f.id " +
                "  join behandling_resultat br on (br.behandling_id=b.id and br.behandling_resultat_type in (:restyper)) " +
                "  join (select beh.fagsak_id fsmax, max(brsq.opprettet_tid) maxbr from behandling beh  " +
                "        join behandling_resultat brsq on (brsq.behandling_id=beh.id and brsq.behandling_resultat_type in (:restyper)) " +
                "        where beh.behandling_type in (:ytelse) and beh.behandling_status in (:avsluttet) " +
                "        group by beh.fagsak_id) on (fsmax=b.fagsak_id and br.opprettet_tid = maxbr) " +
                "  join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=b.id and grbg.aktiv = 'J') " +
                "  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:milsiv) ) " +
                "  join BEREGNINGSGRUNNLAG bglag on grbg.beregningsgrunnlag_id=bglag.id " +
                "  join (select beregningsgrunnlag_id bgid, min(brutto_pr_aar) brutto " +
                "        from BEREGNINGSGRUNNLAG_PERIODE " +
                "        group by beregningsgrunnlag_id " +
                "    ) bgmin on bgmin.bgid=grbg.beregningsgrunnlag_id " +
                "  join BR_RESULTAT_BEHANDLING grbr on (grbr.behandling_id=b.id and grbr.aktiv = 'J') " +
                "  join (select BEREGNINGSRESULTAT_FP_ID brfpid, min(BR_PERIODE_FOM) fom " +
                "        from br_periode brp  " +
                "        group by BEREGNINGSRESULTAT_FP_ID " +
                "    ) brbrutto on brbrutto.brfpid=grbr.BG_BEREGNINGSRESULTAT_FP_ID " +
                "where b.behandling_status in (:avsluttet) and b.behandling_type in (:ytelse) " +
                "  and brbrutto.fom is not null " +
                "  and brbrutto.fom >= :fomdato  " +
                "  and bgmin.brutto <= (:nysats * :avkorting ) " +
                "  and bglag.grunnbeloep=:gmlsats  " +
                "  and f.id not in ( select beh.fagsak_id from behandling beh " +
                "    where beh.behandling_status not in (:avsluttet) and beh.behandling_type in (:ytelse)  " +
                "      and beh.id not in (select ba.behandling_id from behandling_arsak ba where behandling_arsak_type=:berort) ) " ); //$NON-NLS-1$
        query.setParameter("fomdato", gjeldendeFom); //$NON-NLS-1$
        query.setParameter("gmlsats", forrigeSats); //$NON-NLS-1$
        query.setParameter("nysats", gjeldendeSats); //$NON-NLS-1$
        query.setParameter("avkorting", 3); //$NON-NLS-1$
        query.setParameter("restyper", RES_TYPER_REGULERING); //$NON-NLS-1$
        query.setParameter("milsiv", List.of(AktivitetStatus.MILITÆR_ELLER_SIVIL.getKode())); //$NON-NLS-1$
        query.setParameter(AVSLUTTET_KEY, STATUS_FERDIG); // $NON-NLS-1$
        query.setParameter("ytelse", YTELSE_TYPER); //$NON-NLS-1$
        query.setParameter("berort", BehandlingÅrsakType.BERØRT_BEHANDLING.getKode()); //$NON-NLS-1$
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new Tuple<>(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    /** Liste av fagsakId, aktørId for saker som trenger Arena-regulering og det ikke finnes åpen behandling */
    public List<Tuple<Long, AktørId>> finnSakerMedBehovForArenaRegulering(LocalDate gjeldendeFom, LocalDate nySatsDato) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som er beregnet med AAP/DP og der FP-startdato overlapper inputIntervall
         * - Saken har ikke noen beregninger opprettet på eller etter dato for ny sats
         * - Saken har ikke noen åpne ytelsesbehandlinger
         */
        Query query = getEntityManager().createNativeQuery(
            "SELECT DISTINCT f.id , bru.aktoer_id " +
                " from Fagsak f join bruker bru on f.bruker_id=bru.id " +
                "  join behandling b on b.fagsak_id=f.id " +
                "  join behandling_resultat br on (br.behandling_id=b.id and br.behandling_resultat_type in (:restyper)) " +
                "  join (select beh.fagsak_id fsmax, max(brsq.opprettet_tid) maxbr from behandling beh  " +
                "        join behandling_resultat brsq on (brsq.behandling_id=beh.id and brsq.behandling_resultat_type in (:restyper)) " +
                "        where beh.behandling_type in (:ytelse) and beh.behandling_status in (:avsluttet) " +
                "        group by beh.fagsak_id) on (fsmax=b.fagsak_id and br.opprettet_tid = maxbr) " +
                "  join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=b.id and grbg.aktiv = 'J') " +
                "  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:asarena) ) " +
                "  join BR_RESULTAT_BEHANDLING grbr on (grbr.behandling_id=b.id and grbr.aktiv = 'J') " +
                "  join (select BEREGNINGSRESULTAT_FP_ID brfpid, min(BR_PERIODE_FOM) fom " +
                "        from br_periode brp  " +
                "        group by BEREGNINGSRESULTAT_FP_ID " +
                "    ) brbrutto on brbrutto.brfpid=grbr.BG_BEREGNINGSRESULTAT_FP_ID " +
                "where b.opprettet_tid < :satsdato " +
                "  and b.behandling_status in (:avsluttet) and b.behandling_type in (:ytelse) " +
                "  and brbrutto.fom is not null " +
                "  and brbrutto.fom >= :fomdato " +
                "  and f.id not in ( select beh.fagsak_id from behandling beh " +
                "    where beh.behandling_status not in (:avsluttet) and beh.behandling_type in (:ytelse)  " +
                "      and beh.id not in (select ba.behandling_id from behandling_arsak ba where behandling_arsak_type=:berort) ) " ); //$NON-NLS-1$
        query.setParameter("fomdato", gjeldendeFom); //$NON-NLS-1$
        query.setParameter("satsdato", nySatsDato); //$NON-NLS-1$
        query.setParameter("asarena", List.of(AktivitetStatus.ARBEIDSAVKLARINGSPENGER.getKode(), AktivitetStatus.DAGPENGER.getKode())); //$NON-NLS-1$
        query.setParameter("restyper", RES_TYPER_REGULERING); //$NON-NLS-1$
        query.setParameter(AVSLUTTET_KEY, STATUS_FERDIG); // $NON-NLS-1$
        query.setParameter("ytelse", YTELSE_TYPER); //$NON-NLS-1$
        query.setParameter("berort", BehandlingÅrsakType.BERØRT_BEHANDLING.getKode()); //$NON-NLS-1$
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new Tuple<>(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }
}
