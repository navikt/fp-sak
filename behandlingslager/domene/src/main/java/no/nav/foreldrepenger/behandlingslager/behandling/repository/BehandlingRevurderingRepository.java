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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
public class BehandlingRevurderingRepository {

    private static final String AVSLUTTET_KEY = "avsluttet";

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private SøknadRepository søknadRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    BehandlingRevurderingRepository() {
    }

    @Inject
    public BehandlingRevurderingRepository(@VLPersistenceUnit EntityManager entityManager,
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

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering og det ikke finnes åpen behandling */
    public List<Tuple<Long, AktørId>> finnSakerMedBehovForGrunnbeløpRegulering(long gjeldende, long forrige, long forrigeAvkortingMultiplikator,
                                                                               LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har en avsluttet behandling med gammel sats og er avkortet til 6G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen avsluttet behandling med ny sats
         * - Saken har ikke noen åpne ytelsesbehandlinger
         */
        List<String> avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
        List<String> ytelseBehandling = BehandlingType.getYtelseBehandlingTyper().stream().map(BehandlingType::getKode).collect(Collectors.toList());
        Query query = getEntityManager().createNativeQuery(
            "SELECT DISTINCT f.id , br.aktoer_id " +
                "from Fagsak f join bruker br on f.bruker_id=br.id " +
                "where f.id in ( " +
                "  select beh.fagsak_id from behandling beh " +
                "    join behandling_resultat br on (br.behandling_id=beh.id and br.behandling_resultat_type in (:restyper)) " +
                "    join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=beh.id and grbg.aktiv = 'J') " +
                "    join BEREGNINGSGRUNNLAG bglag on grbg.beregningsgrunnlag_id=bglag.id " +
                "    join BEREGNINGSGRUNNLAG_PERIODE bgper on grbg.beregningsgrunnlag_id=bgper.beregningsgrunnlag_id  " +
                "    join BR_RESULTAT_BEHANDLING grbr on (grbr.behandling_id=beh.id and grbr.aktiv = 'J') " +
                "  where beh.behandling_status in (:avsluttet) " +
                "    and grbr.BG_BEREGNINGSRESULTAT_FP_ID in ( " +
                "      select beregningsresultat_fp_id from BR_PERIODE " +
                "      group by beregningsresultat_fp_id " +
                "      having min(br_periode_fom) >= :fomdato ) " +
                "    and bgper.avkortet_pr_aar is not null and bgper.avkortet_pr_aar = (bglag.grunnbeloep * :avkorting )  " +
                "    and bglag.grunnbeloep=:gmlsats ) " +
                " and f.id not in ( " +
                "    select beh.fagsak_id from behandling beh " +
                "    join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=beh.id and grbg.aktiv='J') " +
                "    join BEREGNINGSGRUNNLAG bglag on grbg.beregningsgrunnlag_id=bglag.id " +
                "    where beh.behandling_status in ( :avsluttet ) " +
                "      and bglag.grunnbeloep= :nysats ) " +
                " and f.id not in (" +
                "    select beh.fagsak_id from behandling beh " +
                "    where beh.behandling_status not in ( :avsluttet) " +
                "      and beh.behandling_type in (:ytelse) ) "); //$NON-NLS-1$
        query.setParameter("fomdato", gjeldendeFom); //$NON-NLS-1$
        query.setParameter("nysats", gjeldende); //$NON-NLS-1$ 7
        query.setParameter("gmlsats", forrige); //$NON-NLS-1$
        query.setParameter("avkorting", forrigeAvkortingMultiplikator); //$NON-NLS-1$
        query.setParameter("restyper", List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.FORELDREPENGER_ENDRET.getKode())); //$NON-NLS-1$
        query.setParameter(AVSLUTTET_KEY, avsluttendeStatus); // $NON-NLS-1$
        query.setParameter("ytelse", ytelseBehandling); //$NON-NLS-1$
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
        List<String> avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
        List<String> ytelseBehandling = BehandlingType.getYtelseBehandlingTyper().stream().map(BehandlingType::getKode).collect(Collectors.toList());
        Query query = getEntityManager().createNativeQuery(
            "SELECT DISTINCT f.id, bru.aktoer_id " +
                "from Fagsak f join bruker bru on f.bruker_id=bru.id " +
                "  join behandling b on (fagsak_id = f.id and behandling_status in (:avsluttet) ) " +
                "  join behandling_resultat brs on (brs.behandling_id=b.id and brs.behandling_resultat_type in (:restyper)) " +
                "  join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=b.id and grbg.aktiv='J') " +
                "  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:asarena) ) " +
                "  join ( select grbr.behandling_id, min(brp.br_periode_fom) as fom " +
                "         from BR_RESULTAT_BEHANDLING grbr " +
                "         join BR_PERIODE brp on grbr.BG_BEREGNINGSRESULTAT_FP_ID = brp.beregningsresultat_fp_id " +
                "         where grbr.aktiv='J' " +
                "         group by grbr.behandling_id " +
                "         ) ford ON ford.behandling_id = b.id and ford.fom >= :fomdato and ford.fom <= :satsdato " +
                "where nvl(b.sist_oppdatert_tidspunkt, b.opprettet_tid) < :satsdato " +
                "  and b.id not in (select ib.id from behandling ib " +
                "     join behandling_resultat ibrs on (ibrs.behandling_id=ib.id and ibrs.behandling_resultat_type in (:restyper)) " +
                "     join GR_BEREGNINGSGRUNNLAG igrbg on (igrbg.behandling_id=ib.id and igrbg.aktiv='J') " +
                "     JOIN BG_AKTIVITET_STATUS ibgs ON (ibgs.BEREGNINGSGRUNNLAG_ID = igrbg.BEREGNINGSGRUNNLAG_ID and ibgs.AKTIVITET_STATUS in (:asarena) ) " +
                "    where nvl(ib.sist_oppdatert_tidspunkt, ib.opprettet_tid) >= :satsdato " +
                "     and ib.behandling_status in (:avsluttet) ) " +
                "  and f.id not in (select beh.fagsak_id from behandling beh " +
                "    where beh.behandling_status not in (:avsluttet) " +
                "      and beh.behandling_type in (:ytelse) ) "); //$NON-NLS-1$
        query.setParameter("fomdato", gjeldendeFom); //$NON-NLS-1$
        query.setParameter("satsdato", nySatsDato); //$NON-NLS-1$
        query.setParameter("asarena", List.of(AktivitetStatus.ARBEIDSAVKLARINGSPENGER.getKode(), AktivitetStatus.DAGPENGER.getKode())); //$NON-NLS-1$
        query.setParameter("restyper", List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.FORELDREPENGER_ENDRET.getKode())); //$NON-NLS-1$
        query.setParameter(AVSLUTTET_KEY, avsluttendeStatus); // $NON-NLS-1$
        query.setParameter("ytelse", ytelseBehandling); //$NON-NLS-1$
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new Tuple<>(((BigDecimal) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }
}
