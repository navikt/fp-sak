package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.RelevanteArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.EndringsdatoRevurderingUtleder;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.FastsettUttaksgrunnlagFeil;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class EndringsdatoRevurderingUtlederImpl implements EndringsdatoRevurderingUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(EndringsdatoRevurderingUtlederImpl.class);
    private static final Comparator<LocalDate> LOCAL_DATE_COMPARATOR = Comparator.comparing(LocalDate::toEpochDay);

    private FpUttakRepository fpUttakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private RelevanteArbeidsforholdTjeneste relevanteArbeidsforholdTjeneste;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository; // Kun for logging

    @Inject
    public EndringsdatoRevurderingUtlederImpl(UttakRepositoryProvider repositoryProvider,
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
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingRepository = behandlingRepository;
    }

    EndringsdatoRevurderingUtlederImpl() {
        // CDI
    }

    @Override
    public LocalDate utledEndringsdato(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();
        var årsaker = behandlingRepository.hentBehandling(ref.behandlingId()).getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType).collect(Collectors.toSet());
        var endringsdatoTypeEnumSet = utledEndringsdatoTyper(input);
        if (endringsdatoTypeEnumSet.isEmpty()) {
            endringsdatoTypeEnumSet.add(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
            LOG.info("Behandling behandlingId={} årsaker {} Fant ingen endringstyper. Bruker FUD",  behandlingId, årsaker);
        } else {
            LOG.info("Behandling behandlingId={} årsaker {} endringstyper {}", ref.behandlingId(), årsaker, endringsdatoTypeEnumSet);
        }
        var endringsdato = utledEndringsdato(ref, endringsdatoTypeEnumSet, input, input.getYtelsespesifiktGrunnlag());
        return endringsdato.orElseThrow(() -> FastsettUttaksgrunnlagFeil.kunneIkkeUtledeEndringsdato(behandlingId));
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
        nesteSakEndringstype(fpGrunnlag).ifPresent(endringsdatoTypeEnumSet::add);

        return endringsdatoTypeEnumSet;
    }

    private EnumSet<EndringsdatoType> utledEndringsdatoTypeBerørtBehandling(UttakInput input) {

        if (arbeidsforholdRelevantForUttakErEndret(input)) {
            return EnumSet.of(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
        }
        return EnumSet.of(EndringsdatoType.ENDRINGSDATO_I_BEHANDLING_SOM_FØRTE_TIL_BERØRT_BEHANDLING);
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

    private Optional<EndringsdatoType> nesteSakEndringstype(ForeldrepengerGrunnlag fpGrunnlag) {
        return fpGrunnlag.getNesteSakGrunnlag().isPresent() ? Optional.of(EndringsdatoType.NESTE_STØNADSPERIODE) : Optional.empty();
    }

    private Optional<LocalDate> finnNesteStønadsperiode(BehandlingReferanse ref, ForeldrepengerGrunnlag fpGrunnlag) {
        return fpGrunnlag.getNesteSakGrunnlag().map(NesteSakGrunnlagEntitet::getStartdato).map(Virkedager::justerHelgTilMandag);
    }

    private Optional<EndringsdatoType> førsteUttaksdatoGjeldendeVedtakEndringsdato(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        if (input.isBehandlingManueltOpprettet() || input.isOpplysningerOmDødEndret()
            || arbeidsforholdRelevantForUttakErEndret(input) || endretDekningsgrad(ref)) {

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
        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            if (erFørsteUttaksdatoSøknadEtterSisteUttaksdatoGjeldendeVedtak(input.getBehandlingReferanse())) {
                return Optional.of(EndringsdatoType.SISTE_UTTAKSDATO_GJELDENDE_VEDTAK);
            }
            return Optional.of(EndringsdatoType.FØRSTE_UTTAKSDATO_SØKNAD);
        }
        return Optional.empty();
    }

    private Optional<EndringsdatoType> fødselHarSkjeddSidenForrigeBehandlingEndringsdato(BehandlingReferanse revurdering,
                                                                                         ForeldrepengerGrunnlag fpGrunnlag) {
        if (fødselHarSkjeddSidenForrigeBehandling(fpGrunnlag)) {
            var fødselsdato = fpGrunnlag.getFamilieHendelser()
                .getOverstyrtEllerBekreftet()
                .orElseThrow()
                .getFamilieHendelseDato(); // NOSONAR
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

    private boolean erFørsteUttaksdatoSøknadEtterSisteUttaksdatoGjeldendeVedtak(BehandlingReferanse revurdering) {
        var revurderingId = revurdering.behandlingId();
        var førstegangsBehandling = finnForrigeBehandling(revurdering);
        if (finnSisteUttaksdatoGjeldendeVedtak(førstegangsBehandling).isPresent() && finnFørsteUttaksdatoSøknad(
            revurderingId).isPresent()) {
            return finnFørsteUttaksdatoSøknad(revurderingId).get()
                .isAfter(Virkedager.plusVirkedager(finnSisteUttaksdatoGjeldendeVedtak(førstegangsBehandling).get(), 1));
        }
        return false;
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
        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        if (uttakResultat.isEmpty()) {
            return Optional.empty();
        }
        var uttakPerioder = uttakResultat.get().getGjeldendePerioder().getPerioder();
        var førsteDatoSomIkkeErTaptTilAnnenpart = uttakPerioder.stream()
            .filter(p -> !VedtaksperioderHelper.avslåttPgaAvTaptPeriodeTilAnnenpart(p))
            .min(Comparator.comparing(UttakResultatPeriodeEntitet::getFom))
            .map(UttakResultatPeriodeEntitet::getFom);
        // Alle perioder er tapt til annenpart
        if (førsteDatoSomIkkeErTaptTilAnnenpart.isEmpty()) {
            return uttakPerioder.stream().map(UttakResultatPeriodeEntitet::getFom).min(Comparator.naturalOrder());
        }
        return førsteDatoSomIkkeErTaptTilAnnenpart;
    }

    private Optional<LocalDate> finnSisteUttaksdatoGjeldendeVedtak(Long revurderingId) {
        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(revurderingId);
        if (uttakResultat.isEmpty()) {
            return Optional.empty();
        }
        var uttakPerioder = uttakResultat.get().getGjeldendePerioder().getPerioder();
        return uttakPerioder.stream()
            .max(Comparator.comparing(UttakResultatPeriodeEntitet::getTom))
            .map(UttakResultatPeriodeEntitet::getTom);
    }

    private Optional<LocalDate> finnFørsteUttaksdatoSøknad(Long behandlingId) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        return ytelseFordelingAggregat.getOppgittFordeling()
            .getOppgittePerioder()
            .stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(LOCAL_DATE_COMPARATOR);
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
        var original = revurdering.getOriginalBehandlingId().flatMap(this::finnManueltSattFørsteUttaksdato);
        var aktuell = finnManueltSattFørsteUttaksdato(revurdering.behandlingId());
        return aktuell.isPresent() && !Objects.equals(original, aktuell);
    }

    private Optional<LocalDate> finnManueltSattFørsteUttaksdato(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
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
                case FØRSTE_UTTAKSDATO_SØKNAD -> finnFørsteUttaksdatoSøknad(ref.behandlingId()).ifPresent(datoer::add);
                case SISTE_UTTAKSDATO_GJELDENDE_VEDTAK -> finnEndringsdatoForEndringssøknad(ref, datoer);
                case ENDRINGSDATO_I_BEHANDLING_SOM_FØRTE_TIL_BERØRT_BEHANDLING -> finnEndringsdatoForBerørtBehandling(ref, uttakInput, fpGrunnlag, datoer);
                case MANUELT_SATT_FØRSTE_UTTAKSDATO -> finnManueltSattFørsteUttaksdato(ref.behandlingId()).ifPresent(datoer::add);
                case OMSORGSOVERTAKELSEDATO -> fpGrunnlag.getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getOmsorgsovertakelse()
                    .ifPresent(datoer::add);
                case ANKOMST_NORGE_DATO -> fpGrunnlag.getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getAnkomstNorge()
                    .ifPresent(datoer::add);
                case FØRSTE_UTTAKSDATO_SØKNAD_FORRIGE_BEHANDLING -> finnFørsteUttaksdatoSøknadForrigeBehandling(ref).ifPresent(datoer::add);
                case NESTE_STØNADSPERIODE -> finnNesteStønadsperiode(ref, fpGrunnlag).ifPresent(datoer::add);
                default -> new IllegalStateException("Støtter ikke EndringsdatoType. " + endringsdatoType);
            }
        }

        var tidligst = datoer.stream().min(LOCAL_DATE_COMPARATOR);
        return tidligst.orElseThrow(
            () -> new IllegalStateException("Finner ikke endringsdato. " + endringsdatoer)); // NOSONAR
    }

    private Optional<LocalDate> finnFørsteUttaksdatoSøknadForrigeBehandling(BehandlingReferanse revurdering) {
        return finnFørsteUttaksdatoSøknad(finnForrigeBehandling(revurdering));
    }

    private void finnEndringsdatoForEndringssøknad(BehandlingReferanse revurdering, Set<LocalDate> datoer) {
        var datoen = finnSisteUttaksdatoGjeldendeVedtak(finnForrigeBehandling(revurdering));
        if (datoen.isPresent()) {
            datoer.add(Virkedager.plusVirkedager(datoen.get(), 1));
        }
    }

    private void finnEndringsdatoForBerørtBehandling(BehandlingReferanse ref,
                                                     UttakInput uttakInput,
                                                     ForeldrepengerGrunnlag fpGrunnlag,
                                                     Set<LocalDate> datoer) {
        var annenpartBehandling = fpGrunnlag.getAnnenpart()
            .orElseThrow(() -> new IllegalStateException(
                "Utviklerfeil: Berørt behandling uten innvilget vedtak annen forelders behandling - skal ikke skje"))
            .gjeldendeVedtakBehandlingId();
        var annenpartsFørsteUttaksdato = finnFørsteUttaksdato(annenpartBehandling);
        var førsteUttaksdatoGjeldendeVedtak = finnFørsteUttaksdato(finnForrigeBehandling(ref));

        LocalDate klassiskdato = null;
        if (førsteUttaksdatoGjeldendeVedtak.isPresent() && annenpartsFørsteUttaksdato.isPresent()) {
            klassiskdato = førsteUttaksdatoGjeldendeVedtak.get().isAfter(annenpartsFørsteUttaksdato.get()) ? førsteUttaksdatoGjeldendeVedtak.get() : annenpartsFørsteUttaksdato.get();
        } else {
            if (førsteUttaksdatoGjeldendeVedtak.isPresent()) {
                klassiskdato = førsteUttaksdatoGjeldendeVedtak.get();
            }
            if (annenpartsFørsteUttaksdato.isPresent()) {
                klassiskdato = annenpartsFørsteUttaksdato.get();
            }
        }
        final var klassiskUtledetDato = klassiskdato;
        var annenpartsEndringsdato = finnEndringsdato(annenpartBehandling).or(() -> Optional.ofNullable(klassiskUtledetDato));
        if (annenpartsEndringsdato.isEmpty()) {
            LOG.info("BERØRT ENDRINGSDATO: Annenparts endringsdato og klassisk dato er tom for behandling {}", ref.behandlingId());
        } else if (klassiskUtledetDato == null) {
            LOG.info("BERØRT ENDRINGSDATO: Annenparts endringsdato finnes for behandling {} klassisk dato tom, endringsdato {}", ref.behandlingId(), annenpartsEndringsdato.get());
        }
        var beregnetEndringsdato = beregnetEndringsdatoBerørtBehandling(uttakInput, annenpartBehandling);
        var beregnetEndringsdatoStr = beregnetEndringsdato.map(LocalDate::toString).orElse("");
        if (beregnetEndringsdato.isEmpty()) {
            LOG.info("BERØRT ENDRINGSDATO: fant ikke beregnet dato for behandling {}", ref.behandlingId());
        }
        annenpartsEndringsdato.ifPresent(apd -> {
            if (klassiskUtledetDato != null && apd.isBefore(klassiskUtledetDato)) {
                LOG.info("BERØRT ENDRINGSDATO: FØR behandling {} klassisk dato {} endringsdato {} beregnet {}",
                    ref.behandlingId(), klassiskUtledetDato, apd, beregnetEndringsdatoStr);
                datoer.add(klassiskUtledetDato);
            } else {
                if (apd.equals(klassiskUtledetDato)) {
                    LOG.info("BERØRT ENDRINGSDATO: LIK behandling {} klassisk dato {} endringsdato {} beregnet {}",
                        ref.behandlingId(), klassiskUtledetDato, apd, beregnetEndringsdatoStr);
                    datoer.add(apd);
                } else if (beregnetEndringsdato.filter(bed -> bed.isAfter(apd)).isPresent()) {
                    LOG.info("BERØRT ENDRINGSDATO: BEREGN behandling {} klassisk dato {} endringsdato {} beregnet {}",
                        ref.behandlingId(), klassiskUtledetDato, apd, beregnetEndringsdatoStr);
                    datoer.add(apd);
                } else {
                    LOG.info("BERØRT ENDRINGSDATO: ETTER behandling {} klassisk dato {} endringsdato {} beregnet {}",
                        ref.behandlingId(), klassiskUtledetDato, apd, beregnetEndringsdatoStr);
                }
                datoer.add(apd);
            }
        });
    }

    private Optional<LocalDate> beregnetEndringsdatoBerørtBehandling(UttakInput input, Long utløsendeBehandlingId) {
        var utløsendeUttak = hentUttak(utløsendeBehandlingId).orElseGet(() -> new ForeldrepengerUttak(List.of()));
        var berørtUttak = hentUttak(input.getBehandlingReferanse().originalBehandlingId());
        return EndringsdatoBerørtUtleder.utledEndringsdatoForBerørtBehandling(utløsendeUttak,
            ytelsesFordelingRepository.hentAggregatHvisEksisterer(utløsendeBehandlingId),
            behandlingsresultatRepository.hent(utløsendeBehandlingId),
            stønadskontoSaldoTjeneste.erOriginalNegativSaldoPåNoenKontoForsiktig(input), // Gambler på samme resultat for input fra begge partene
            berørtUttak,
            input,
            "Berørt endringsdato");
    }

    private Optional<ForeldrepengerUttak> hentUttak(Long behandling) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandling);
    }

    private Long finnForrigeBehandling(BehandlingReferanse behandling) {
        return behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException(
                "Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
    }
}
