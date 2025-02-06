package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.RelevanteArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.exception.TekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class EndringsdatoRevurderingUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(EndringsdatoRevurderingUtleder.class);

    private FpUttakRepository fpUttakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private RelevanteArbeidsforholdTjeneste relevanteArbeidsforholdTjeneste;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private BehandlingRepository behandlingRepository; // Kun for logging

    @Inject
    public EndringsdatoRevurderingUtleder(UttakRepositoryProvider repositoryProvider,
                                          BehandlingRepository behandlingRepository,
                                          DekningsgradTjeneste dekningsgradTjeneste,
                                          RelevanteArbeidsforholdTjeneste relevanteArbeidsforholdTjeneste,
                                          StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste) {
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.relevanteArbeidsforholdTjeneste = relevanteArbeidsforholdTjeneste;
        this.uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    EndringsdatoRevurderingUtleder() {
        // CDI
    }

    public LocalDate utledEndringsdato(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();
        var årsaker = behandlingRepository.hentBehandlingHvisFinnes(ref.behandlingUuid())
            .map(Behandling::getBehandlingÅrsaker).orElse(List.of()).stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType).collect(Collectors.toSet());
        var endringsdatoTypeEnumSet = utledEndringsdatoTyper(input);
        if (endringsdatoTypeEnumSet.isEmpty()) {
            endringsdatoTypeEnumSet.add(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
            LOG.info("Behandling behandlingId={} årsaker {} Fant ingen endringstyper. Bruker FUD",  behandlingId, årsaker);
        } else {
            LOG.info("Behandling behandlingId={} årsaker {} endringstyper {}", ref.behandlingId(), årsaker, endringsdatoTypeEnumSet);
        }
        var endringsdato = utledEndringsdato(ref, endringsdatoTypeEnumSet, input, input.getYtelsespesifiktGrunnlag());
        return endringsdato.orElseThrow(() -> new TekniskException("FP-282721", "Kunne ikke utlede endringsdato for revurdering med behandlingId=" + behandlingId));
    }

    private Optional<LocalDate> utledEndringsdato(BehandlingReferanse ref,
                                                  EnumSet<EndringsdatoType> endringsdatoTypeEnumSet,
                                                  UttakInput uttakInput,
                                                  ForeldrepengerGrunnlag fpGrunnlag) {
        var endringsdato = finnFørsteDato(endringsdatoTypeEnumSet, ref, uttakInput, fpGrunnlag);
        // Sjekk om endringsdato overlapper med tidligere vedtak. Hvis periode i tidligere vedtak er manuelt behandling så skal endringsdato flyttes
        // start av perioden
        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(finnForrigeBehandling(ref));
        if (uttakResultat.isEmpty()) {
            return Optional.of(endringsdato);
        }
        var vedtattPeriodeMedOverlapp = finnVedtattPeriodeMedOverlapp(endringsdato, uttakResultat.get());
        if (vedtattPeriodeMedOverlapp.isPresent() && vedtattPeriodeMedOverlapp.get().isManueltBehandlet()) {
            return Optional.ofNullable(vedtattPeriodeMedOverlapp.get().getFom());

        }
        return Optional.of(endringsdato);
    }

    private EnumSet<EndringsdatoType> utledEndringsdatoTyper(UttakInput input) {
        if (input.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            return utledEndringsdatoTypeBerørtBehandling(input);
        }
        var ref = input.getBehandlingReferanse();
        var endringsdatoTypeEnumSet = EnumSet.noneOf(EndringsdatoType.class);
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        fødselHarSkjeddSidenForrigeBehandlingEndringsdato(ref, fpGrunnlag).ifPresent(endringsdatoTypeEnumSet::add);
        erEndringssøknadEndringsdato(input).ifPresent(endringsdatoTypeEnumSet::add);
        manueltSattFørsteUttaksdatoEndringsdato(ref).ifPresent(endringsdatoTypeEnumSet::add);
        førsteUttaksdatoGjeldendeVedtakEndringsdato(input).ifPresent(endringsdatoTypeEnumSet::add);
        adopsjonEndringsdato(fpGrunnlag).ifPresent(endringsdatoTypeEnumSet::add);
        forrigeBehandlingUtenUttakEndringsdato(ref).ifPresent(endringsdatoTypeEnumSet::add);
        nesteSakEndringstype(ref, fpGrunnlag).ifPresent(endringsdatoTypeEnumSet::add);
        pleiepengerEndringstype(fpGrunnlag).ifPresent(endringsdatoTypeEnumSet::add);
        if (endretDekningsgrad(ref)) {
            endringsdatoTypeEnumSet.add(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
        }

        return endringsdatoTypeEnumSet;
    }

    private EnumSet<EndringsdatoType> utledEndringsdatoTypeBerørtBehandling(UttakInput input) {
        if (arbeidsforholdRelevantForUttakErEndret(input) || harAnnenpartEndretStønadskonto(input)) {
            return EnumSet.of(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
        }
        return EnumSet.of(EndringsdatoType.ENDRINGSDATO_I_BEHANDLING_SOM_FØRTE_TIL_BERØRT_BEHANDLING);
    }

    private boolean harAnnenpartEndretStønadskonto(UttakInput input) {
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var annenpartBehandling = annenpartsBehandling(fpGrunnlag);
        var utløsendeUttak = hentUttak(annenpartBehandling);
        var berørtUttak = hentUttak(input.getBehandlingReferanse().originalBehandlingId());
        return UtregnetStønadskontoTjeneste.harEndretStrukturEllerRedusertAntallStønadsdager(
            berørtUttak.map(ForeldrepengerUttak::getStønadskontoBeregning).orElse(Map.of()), utløsendeUttak.map(ForeldrepengerUttak::getStønadskontoBeregning).orElse(Map.of()));
    }

    private static Long annenpartsBehandling(ForeldrepengerGrunnlag fpGrunnlag) {
        return fpGrunnlag.getAnnenpart()
            .orElseThrow(
                () -> new IllegalStateException("Utviklerfeil: Berørt behandling uten innvilget vedtak annen forelders behandling - skal ikke skje"))
            .gjeldendeVedtakBehandlingId();
    }

    private boolean arbeidsforholdRelevantForUttakErEndret(UttakInput input) {
        return relevanteArbeidsforholdTjeneste.arbeidsforholdRelevantForUttakErEndretSidenForrigeBehandling(input);
    }

    private Optional<EndringsdatoType> forrigeBehandlingUtenUttakEndringsdato(BehandlingReferanse ref) {
        if (!forrigeBehandlingHarUttaksresultat(ref)) {
            return Optional.of(EndringsdatoType.FØRSTE_UTTAKSDATO_SØKNAD_FORRIGE_BEHANDLING);
        }
        return Optional.empty();
    }

    private Optional<EndringsdatoType> adopsjonEndringsdato(ForeldrepengerGrunnlag fpGrunnlag) {
        if (!fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel()) {
            return Optional.ofNullable(finnEndringsdatoTypeVedAdopsjon(fpGrunnlag));
        }
        return Optional.empty();
    }

    private Optional<EndringsdatoType> nesteSakEndringstype(BehandlingReferanse ref, ForeldrepengerGrunnlag fpGrunnlag) {
        var sisteUttakdato = finnSisteUttaksdatoGjeldendeVedtak(finnForrigeBehandling(ref))
            .map(d -> Virkedager.plusVirkedager(d, 1));
        // Gi bare utslag dersom neste stønadsperiode finnes of begynner på eller før nåværende siste uttaksdato
        return fpGrunnlag.getNesteSakGrunnlag()
            .filter(neste -> sisteUttakdato.filter(d -> neste.getStartdato().isBefore(d)).isPresent())
            .isPresent() ? Optional.of(EndringsdatoType.NESTE_STØNADSPERIODE) : Optional.empty();
    }

    private Optional<EndringsdatoType> pleiepengerEndringstype(ForeldrepengerGrunnlag fpGrunnlag) {
        return fpGrunnlag.isOppdagetPleiepengerOverlappendeUtbetaling() ? Optional.of(EndringsdatoType.VEDTAK_PLEIEPENGER) : Optional.empty();
    }

    private Optional<LocalDate> finnNesteStønadsperiode(ForeldrepengerGrunnlag fpGrunnlag) {
        return fpGrunnlag.getNesteSakGrunnlag().map(NesteSakGrunnlagEntitet::getStartdato).map(Virkedager::justerHelgTilMandag);
    }

    private Optional<EndringsdatoType> førsteUttaksdatoGjeldendeVedtakEndringsdato(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var fpGrunnlag = (ForeldrepengerGrunnlag) input.getYtelsespesifiktGrunnlag();
        if (input.isBehandlingManueltOpprettet() || fpGrunnlag.isDødsfall() || arbeidsforholdRelevantForUttakErEndret(input) ||
            endretDekningsgrad(ref) || input.harBehandlingÅrsak(BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE) ||
            input.harBehandlingÅrsak(BehandlingÅrsakType.FEIL_PRAKSIS_IVERKS_UTSET)) {

            return Optional.of(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
        }
        return Optional.empty();
    }

    private Optional<EndringsdatoType> manueltSattFørsteUttaksdatoEndringsdato(BehandlingReferanse ref) {
        if (harManueltSattFørsteUttaksdato(ref)) {
            return Optional.of(EndringsdatoType.MANUELT_SATT_FØRSTE_UTTAKSDATO);
        }
        return Optional.empty();
    }

    private Optional<EndringsdatoType> erEndringssøknadEndringsdato(UttakInput input) {
        return input.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER) ? Optional.of(EndringsdatoType.ENDRINGSSØKNAD) : Optional.empty();
    }

    private Optional<EndringsdatoType> fødselHarSkjeddSidenForrigeBehandlingEndringsdato(BehandlingReferanse revurdering,
                                                                                         ForeldrepengerGrunnlag fpGrunnlag) {
        if (fødselHarSkjeddSidenForrigeBehandling(fpGrunnlag)) {
            var fødselsdato = fpGrunnlag.getFamilieHendelser()
                .getOverstyrtEllerBekreftet()
                .orElseThrow()
                .getFamilieHendelseDato();
            var førsteUttaksdato = finnFørsteUttaksdato(finnForrigeBehandling(revurdering));
            if (førsteUttaksdato.isEmpty() || fødselsdato.isBefore(førsteUttaksdato.get())) {
                return Optional.of(EndringsdatoType.FØDSELSDATO);
            }
            return Optional.of(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
        }
        return Optional.empty();
    }

    private boolean endretDekningsgrad(BehandlingReferanse ref) {
        return dekningsgradTjeneste.behandlingHarEndretDekningsgrad(ref);
    }

    private boolean forrigeBehandlingHarUttaksresultat(BehandlingReferanse revurdering) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(finnForrigeBehandling(revurdering)).isPresent();
    }

    private boolean fødselHarSkjeddSidenForrigeBehandling(ForeldrepengerGrunnlag fpGrunnlag) {
        var fødselsdatoForrigeBehandling = fpGrunnlag.getOriginalBehandling()
            .orElseThrow()
            .getFamilieHendelser()
            .getOverstyrtEllerBekreftet()
            .flatMap(FamilieHendelse::getFødselsdato);
        var fødselsdatoRevurdering = fpGrunnlag.getFamilieHendelser()
            .getOverstyrtEllerBekreftet()
            .flatMap(FamilieHendelse::getFødselsdato);
        return fødselsdatoForrigeBehandling.isEmpty() && fødselsdatoRevurdering.isPresent();
    }

    private Optional<LocalDate> finnFørsteUttaksdato(Long behandlingId) {
        var uttakPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId)
            .map(UttakResultatEntitet::getGjeldendePerioder).map(UttakResultatPerioderEntitet::getPerioder).orElse(List.of());
        return uttakPerioder.stream()
            .filter(p -> !VedtaksperioderHelper.avslåttPgaAvTaptPeriodeTilAnnenpart(p))
            .map(UttakResultatPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder())
            .or(() -> uttakPerioder.stream()
                .map(UttakResultatPeriodeEntitet::getFom)
                .min(Comparator.naturalOrder()));
    }

    private LocalDateTimeline<Boolean> tidslinjeUttakMedUtbetaling(Long behandlingId) {
        var utbetalt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId)
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder).orElse(List.of()).stream()
            .filter(p -> p.getAktiviteter().stream().anyMatch(a -> a.getUtbetalingsgrad().harUtbetaling() && a.getTrekkdager().merEnn0()))
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .toList();
        return new LocalDateTimeline<>(utbetalt, StandardCombinators::alwaysTrueForMatch);
    }

    private Optional<LocalDate> finnSisteUttaksdatoGjeldendeVedtak(Long revurderingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(revurderingId)
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder).orElse(List.of()).stream()
            .map(UttakResultatPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnFørsteUttaksdatoSøknad(Long behandlingId) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        return ytelseFordelingAggregat.getOppgittFordeling()
            .getPerioder()
            .stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnEndringsdato(Long behandlingId) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        return ytelseFordelingAggregat.getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getGjeldendeEndringsdato);
    }

    private Optional<UttakResultatPeriodeEntitet> finnVedtattPeriodeMedOverlapp(LocalDate førsteUttaksdatoSøknad,
                                                                                UttakResultatEntitet uttakResultat) {
        var uttakPerioder = uttakResultat.getGjeldendePerioder().getPerioder();
        return uttakPerioder.stream().filter(p -> p.overlapper(førsteUttaksdatoSøknad)).findFirst();
    }

    private boolean harManueltSattFørsteUttaksdato(BehandlingReferanse revurdering) {
        return finnManueltSattFørsteUttaksdato(revurdering).isPresent();
    }

    private Optional<LocalDate> finnManueltSattFørsteUttaksdato(BehandlingReferanse revurdering) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(revurdering.behandlingId())
            .flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);
    }

    private EndringsdatoType finnEndringsdatoTypeVedAdopsjon(ForeldrepengerGrunnlag fpGrunnlag) {
        if (fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getAnkomstNorge().isPresent()) {
            return EndringsdatoType.ANKOMST_NORGE_DATO;
        }
        return EndringsdatoType.OMSORGSOVERTAKELSEDATO;
    }

    private LocalDate finnFørsteDato(Set<EndringsdatoType> endringsdatoer,
                                     BehandlingReferanse ref,
                                     UttakInput uttakInput,
                                     ForeldrepengerGrunnlag fpGrunnlag) {
        Set<LocalDate> datoer = new HashSet<>();

        for (var endringsdatoType : endringsdatoer) {
            switch (endringsdatoType) {
                case FØDSELSDATO -> fpGrunnlag.getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getFødselsdato()
                    .ifPresent(datoer::add);
                case FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK -> finnFørsteUttaksdato(finnForrigeBehandling(ref)).ifPresent(datoer::add);
                case ENDRINGSSØKNAD -> finnEndringsdatoForEndringssøknad(ref, datoer);
                case ENDRINGSDATO_I_BEHANDLING_SOM_FØRTE_TIL_BERØRT_BEHANDLING -> finnEndringsdatoForBerørtBehandling(ref, uttakInput, fpGrunnlag, datoer);
                case MANUELT_SATT_FØRSTE_UTTAKSDATO -> finnManueltSattFørsteUttaksdato(ref).ifPresent(datoer::add);
                case OMSORGSOVERTAKELSEDATO -> fpGrunnlag.getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getOmsorgsovertakelse()
                    .ifPresent(datoer::add);
                case ANKOMST_NORGE_DATO -> fpGrunnlag.getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getAnkomstNorge()
                    .ifPresent(datoer::add);
                case FØRSTE_UTTAKSDATO_SØKNAD_FORRIGE_BEHANDLING -> finnFørsteUttaksdatoSøknadForrigeBehandling(ref).ifPresent(datoer::add);
                case NESTE_STØNADSPERIODE -> finnNesteStønadsperiode(fpGrunnlag).ifPresent(datoer::add);
                case VEDTAK_PLEIEPENGER -> førsteDatoMedOverlappPleiepenger(uttakInput).ifPresent(datoer::add);
            }
        }

        var tidligst = datoer.stream().min(Comparator.naturalOrder());
        return tidligst.orElseThrow(
            () -> new IllegalStateException("Finner ikke endringsdato. " + endringsdatoer));
    }

    private Optional<LocalDate> finnFørsteUttaksdatoSøknadForrigeBehandling(BehandlingReferanse revurdering) {
        return finnFørsteUttaksdatoSøknad(finnForrigeBehandling(revurdering));
    }

    private void finnEndringsdatoForEndringssøknad(BehandlingReferanse revurdering, Set<LocalDate> datoer) {
        var førsteSøknadsdato = finnFørsteUttaksdatoSøknad(revurdering.behandlingId());
        var sisteUttakdato = finnSisteUttaksdatoGjeldendeVedtak(revurdering.originalBehandlingId())
            .map(d -> Virkedager.plusVirkedager(d, 1));
        // Bruk min(siste uttaksdato + 1, tidligste dato fra søknad) - siste uttak med mindre første søknad er tidligere
        var endringsdato = sisteUttakdato.filter(sud -> førsteSøknadsdato.filter(fsd -> fsd.isBefore(sud)).isEmpty())
            .or(() -> førsteSøknadsdato);
        endringsdato.ifPresent(datoer::add);
    }

    private Optional<LocalDate> førsteDatoMedOverlappPleiepenger(UttakInput input) {
        var tidslinjeUtbetalt = tidslinjeUttakMedUtbetaling(finnForrigeBehandling(input.getBehandlingReferanse()));
        var segmentPleiepenger = PleiepengerJustering.pleiepengerUtsettelser(input.getBehandlingReferanse().aktørId(), input.getIayGrunnlag()).stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .toList();
        var tidslinjePleiepenger = new LocalDateTimeline<>(segmentPleiepenger, StandardCombinators::alwaysTrueForMatch);
        var tidslinjeOverlapp = tidslinjeUtbetalt.intersection(tidslinjePleiepenger);
        Optional<LocalDate> førsteOverlapp = !tidslinjeOverlapp.isEmpty() ? Optional.of(tidslinjeOverlapp.getMinLocalDate()) : Optional.empty();
        var søknadsPerioder = ytelsesFordelingRepository.hentAggregatHvisEksisterer(input.getBehandlingReferanse().behandlingId())
            .map(YtelseFordelingAggregat::getOppgittFordeling)
            .map(OppgittFordelingEntitet::getPerioder).orElse(List.of()).stream()
            .filter(p -> !p.isOpphold() && !p.isUtsettelse())
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .toList();
        if (!søknadsPerioder.isEmpty()) {
            var overlappSøknadTidslinje = new LocalDateTimeline<>(søknadsPerioder, StandardCombinators::alwaysTrueForMatch).intersection(tidslinjePleiepenger);
            if (!overlappSøknadTidslinje.isEmpty() && førsteOverlapp.filter(fo -> fo.isBefore(overlappSøknadTidslinje.getMinLocalDate())).isEmpty()) {
                førsteOverlapp = Optional.of(overlappSøknadTidslinje.getMinLocalDate());
            }
        }
        return førsteOverlapp;
    }

    private void finnEndringsdatoForBerørtBehandling(BehandlingReferanse ref,
                                                     UttakInput uttakInput,
                                                     ForeldrepengerGrunnlag fpGrunnlag,
                                                     Set<LocalDate> datoer) {
        var annenpartBehandling = annenpartsBehandling(fpGrunnlag);
        var annenpartsFørsteUttaksdato = finnFørsteUttaksdato(annenpartBehandling);
        var førsteUttaksdatoGjeldendeVedtak = finnFørsteUttaksdato(finnForrigeBehandling(ref));

        var senesteFørsteUttakDato = førsteUttaksdatoGjeldendeVedtak
            .filter(førsteUttaksdato -> annenpartsFørsteUttaksdato.filter(førsteUttaksdato::isAfter).isPresent())
            .or(() -> annenpartsFørsteUttaksdato)
            .or(() -> førsteUttaksdatoGjeldendeVedtak) // Annen part har typisk utsatt start. Kan være hull første 6 uker.
            .orElseThrow(); // Da skulle det ikke ha blitt berørt i første omgang

        var senestAvFørsteUttaksdatoEllerAnnenpartsEndringsdato = finnEndringsdato(annenpartBehandling)
            .filter(senesteFørsteUttakDato::isBefore).orElse(senesteFørsteUttakDato);

        var beregnetEndringsdato = beregnetEndringsdatoBerørtBehandling(uttakInput, annenpartBehandling)
            .filter(senestAvFørsteUttaksdatoEllerAnnenpartsEndringsdato::isBefore)
            .orElse(senestAvFørsteUttaksdatoEllerAnnenpartsEndringsdato);

        LOG.info("BERØRT ENDRINGSDATO: behandling {} klassisk dato {} endringsdato {} beregnet {}", ref.behandlingId(), senesteFørsteUttakDato,
            senestAvFørsteUttaksdatoEllerAnnenpartsEndringsdato, beregnetEndringsdato);
        datoer.add(beregnetEndringsdato);
    }

    private Optional<LocalDate> beregnetEndringsdatoBerørtBehandling(UttakInput input, Long utløsendeBehandlingId) {
        var utløsendeUttak = hentUttak(utløsendeBehandlingId).orElseGet(() -> new ForeldrepengerUttak(List.of()));
        var originalBehandlingId = input.getBehandlingReferanse().originalBehandlingId();
        var berørtUttak = hentUttak(originalBehandlingId);
        return EndringsdatoBerørtUtleder.utledEndringsdatoForBerørtBehandling(utløsendeUttak,
            ytelsesFordelingRepository.hentAggregat(utløsendeBehandlingId),
            stønadskontoSaldoTjeneste.erOriginalNegativSaldoPåNoenKontoForsiktig(input), // Gambler på samme resultat for input fra begge partene
            berørtUttak,
            input,
            "Berørt endringsdato");
    }

    private Optional<ForeldrepengerUttak> hentUttak(Long behandling) {
        return uttakTjeneste.hentHvisEksisterer(behandling);
    }

    private Long finnForrigeBehandling(BehandlingReferanse behandling) {
        return behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException(
                "Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
    }
}
