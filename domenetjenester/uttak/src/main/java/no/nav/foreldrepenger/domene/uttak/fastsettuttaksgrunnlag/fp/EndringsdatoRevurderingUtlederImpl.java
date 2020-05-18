package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Collection;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.EndringsdatoRevurderingUtleder;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.FastsettUttaksgrunnlagFeil;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;
import no.nav.vedtak.exception.VLException;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class EndringsdatoRevurderingUtlederImpl implements EndringsdatoRevurderingUtleder {

    private static final Logger log = LoggerFactory.getLogger(EndringsdatoRevurderingUtlederImpl.class);
    private static final Comparator<LocalDate> LOCAL_DATE_COMPARATOR = Comparator.comparing(LocalDate::toEpochDay);

    private UttakRepository uttakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private DekningsgradTjeneste dekningsgradTjeneste;

    @Inject
    public EndringsdatoRevurderingUtlederImpl(UttakRepositoryProvider repositoryProvider,
                                              DekningsgradTjeneste dekningsgradTjeneste) {
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    EndringsdatoRevurderingUtlederImpl() {
        // CDI
    }

    @Override
    public LocalDate utledEndringsdato(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        Long behandlingId = ref.getBehandlingId();
        EnumSet<EndringsdatoType> endringsdatoTypeEnumSet = utledEndringsdatoTyper(input);
        if (endringsdatoTypeEnumSet.isEmpty()) {
            endringsdatoTypeEnumSet.add(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
            log.info("Kunne ikke utlede endringsdato for revurdering med behandlingId=" + behandlingId + ". Satte FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.");
        }
        Optional<LocalDate> endringsdato = utledEndringsdato(ref, endringsdatoTypeEnumSet, input.getYtelsespesifiktGrunnlag());
        return endringsdato.orElseThrow(() -> kanIkkeUtledeException(behandlingId));
    }

    private Optional<LocalDate> utledEndringsdato(BehandlingReferanse ref,
                                                  EnumSet<EndringsdatoType> endringsdatoTypeEnumSet,
                                                  ForeldrepengerGrunnlag fpGrunnlag) {
        LocalDate endringsdato = finnFørsteDato(endringsdatoTypeEnumSet, ref, fpGrunnlag);
        // Sjekk om endringsdato overlapper med tidligere vedtak. Hvis periode i tidligere vedtak er manuelt behandling så skal endringsdato flyttes
        // start av perioden
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(finnForrigeBehandling(ref));
        if (uttakResultat.isEmpty()) {
            return Optional.of(endringsdato);
        }
        Optional<UttakResultatPeriodeEntitet> vedtattPeriodeMedOverlapp = finnVedtattPeriodeMedOverlapp(endringsdato, uttakResultat.get());
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
        EnumSet<EndringsdatoType> endringsdatoTypeEnumSet = EnumSet.noneOf(EndringsdatoType.class);
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        fødselHarSkjeddSidenForrigeBehandlingEndringsdato(ref, fpGrunnlag).ifPresent(endringsdatoTypeEnumSet::add);
        erEndringssøknadEndringsdato(input).ifPresent(endringsdatoTypeEnumSet::add);
        manueltSattFørsteUttaksdatoEndringsdato(ref).ifPresent(endringsdatoTypeEnumSet::add);
        førsteUttaksdatoGjeldendeVedtakEndringsdato(input).ifPresent(endringsdatoTypeEnumSet::add);
        adopsjonEndringsdato(fpGrunnlag).ifPresent(endringsdatoTypeEnumSet::add);
        forrigeBehandlingUtenUttakEndringsdato(ref).ifPresent(endringsdatoTypeEnumSet::add);

        return endringsdatoTypeEnumSet;
    }

    private EnumSet<EndringsdatoType> utledEndringsdatoTypeBerørtBehandling(UttakInput input) {

        if (arbeidsforholdRelevantForUttakErEndret(input)) {
            return EnumSet.of(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
        }
        return EnumSet.of(EndringsdatoType.ENDRINGSDATO_I_BEHANDLING_SOM_FØRTE_TIL_BERØRT_BEHANDLING);
    }

    private Optional<EndringsdatoType> forrigeBehandlingUtenUttakEndringsdato(BehandlingReferanse ref) {
        Optional<EndringsdatoType> endringsdatoType = Optional.empty();
        if (!forrigeBehandlingHarUttaksresultat(ref)) {
            endringsdatoType = Optional.of(EndringsdatoType.FØRSTE_UTTAKSDATO_SØKNAD_FORRIGE_BEHANDLING);
        }
        return endringsdatoType;
    }

    private Optional<EndringsdatoType> adopsjonEndringsdato(ForeldrepengerGrunnlag fpGrunnlag) {
        Optional<EndringsdatoType> endringsdatoType = Optional.empty();
        if (!fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel()) {
            endringsdatoType = Optional.ofNullable(finnEndringsdatoTypeVedAdopsjon(fpGrunnlag));
        }
        return endringsdatoType;
    }

    private Optional<EndringsdatoType> førsteUttaksdatoGjeldendeVedtakEndringsdato(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        Optional<EndringsdatoType> endringsdato = Optional.empty();
        if (input.isBehandlingManueltOpprettet() ||
            input.isOpplysningerOmDødEndret() ||
            arbeidsforholdRelevantForUttakErEndret(input) ||
            endretDekningsgrad(ref)) {

            endringsdato = Optional.of(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);

        }
        return endringsdato;
    }

    private Optional<EndringsdatoType> manueltSattFørsteUttaksdatoEndringsdato(BehandlingReferanse ref) {
        Optional<EndringsdatoType> endringsdato = Optional.empty();
        if (harManueltSattFørsteUttaksdato(ref)) {
            endringsdato = Optional.of(EndringsdatoType.MANUELT_SATT_FØRSTE_UTTAKSDATO);
        }
        return endringsdato;
    }

    private Optional<EndringsdatoType> erEndringssøknadEndringsdato(UttakInput input) {
        Optional<EndringsdatoType> endringsdato = Optional.empty();
        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            if (erFørsteUttaksdatoSøknadEtterSisteUttaksdatoGjeldendeVedtak(input.getBehandlingReferanse())) {
                endringsdato = Optional.of(EndringsdatoType.SISTE_UTTAKSDATO_GJELDENDE_VEDTAK);
            } else {
                endringsdato = Optional.of(EndringsdatoType.FØRSTE_UTTAKSDATO_SØKNAD);
            }
        }
        return endringsdato;
    }

    private Optional<EndringsdatoType> fødselHarSkjeddSidenForrigeBehandlingEndringsdato(BehandlingReferanse revurdering, ForeldrepengerGrunnlag fpGrunnlag) {
        Optional<EndringsdatoType> endringsdato = Optional.empty();
        if (fødselHarSkjeddSidenForrigeBehandling(fpGrunnlag)) {
            LocalDate fødselsdato = fpGrunnlag.getFamilieHendelser().getOverstyrtEllerBekreftet().orElseThrow().getFamilieHendelseDato(); // NOSONAR
            Optional<LocalDate> førsteUttaksdato = finnFørsteUttaksdato(finnForrigeBehandling(revurdering));
            if (førsteUttaksdato.isEmpty() || fødselsdato.isBefore(førsteUttaksdato.get())) {
                endringsdato = Optional.of(EndringsdatoType.FØDSELSDATO);
            } else {
                endringsdato = Optional.of(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
            }
        }
        return endringsdato;
    }

    private boolean endretDekningsgrad(BehandlingReferanse ref) {
        return dekningsgradTjeneste.behandlingHarEndretDekningsgrad(ref);
    }

    private boolean erFørsteUttaksdatoSøknadEtterSisteUttaksdatoGjeldendeVedtak(BehandlingReferanse revurdering) {
        Long revurderingId = revurdering.getBehandlingId();
        Long førstegangsBehandling = finnForrigeBehandling(revurdering);
        if (finnSisteUttaksdatoGjeldendeVedtak(førstegangsBehandling).isPresent() && finnFørsteUttaksdatoSøknad(revurderingId).isPresent()) {
            return finnFørsteUttaksdatoSøknad(revurderingId).get()
                .isAfter(Virkedager.plusVirkedager(finnSisteUttaksdatoGjeldendeVedtak(førstegangsBehandling).get(), 1));
        }
        return false;
    }

    private boolean forrigeBehandlingHarUttaksresultat(BehandlingReferanse revurdering) {
        return uttakRepository.hentUttakResultatHvisEksisterer(finnForrigeBehandling(revurdering)).isPresent();
    }

    private boolean fødselHarSkjeddSidenForrigeBehandling(ForeldrepengerGrunnlag fpGrunnlag) {
        Optional<LocalDate> fødselsdatoForrigeBehandling = fpGrunnlag.getOriginalBehandling().orElseThrow().getFamilieHendelser().getOverstyrtEllerBekreftet().flatMap(FamilieHendelse::getFødselsdato);
        Optional<LocalDate> fødselsdatoRevurdering = fpGrunnlag.getFamilieHendelser().getOverstyrtEllerBekreftet().flatMap(FamilieHendelse::getFødselsdato);
        return fødselsdatoForrigeBehandling.isEmpty() && fødselsdatoRevurdering.isPresent();
    }

    private Optional<LocalDate> finnFørsteUttaksdato(Long behandlingId) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        if (uttakResultat.isEmpty()) {
            return Optional.empty();
        }
        List<UttakResultatPeriodeEntitet> uttakPerioder = uttakResultat.get()
            .getGjeldendePerioder().getPerioder();
        Optional<LocalDate> førsteDatoSomIkkeErTaptTilAnnenpart = uttakPerioder.stream()
            .filter(p -> !VedtaksperioderHelper.avslåttPgaAvTaptPeriodeTilAnnenpart(p))
            .min(Comparator.comparing(UttakResultatPeriodeEntitet::getFom))
            .map(UttakResultatPeriodeEntitet::getFom);
        // Alle perioder er tapt til annenpart
        if (førsteDatoSomIkkeErTaptTilAnnenpart.isEmpty() && uttakPerioder.size() > 0) {
            return Optional.ofNullable(uttakPerioder.get(0).getFom());
        }
        return førsteDatoSomIkkeErTaptTilAnnenpart;
    }

    private Optional<LocalDate> finnSisteUttaksdatoGjeldendeVedtak(Long revurderingId) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(revurderingId);
        if (uttakResultat.isEmpty()) {
            return Optional.empty();
        }
        List<UttakResultatPeriodeEntitet> uttakPerioder = uttakResultat.get()
            .getGjeldendePerioder().getPerioder();
        return uttakPerioder.stream()
            .max(Comparator.comparing(UttakResultatPeriodeEntitet::getTom))
            .map(UttakResultatPeriodeEntitet::getTom);
    }

    private Optional<LocalDate> finnFørsteUttaksdatoSøknad(Long behandlingId) {
        YtelseFordelingAggregat ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        return ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder().stream()
            .map(OppgittPeriodeEntitet::getFom).min(LOCAL_DATE_COMPARATOR);
    }

    private Optional<UttakResultatPeriodeEntitet> finnVedtattPeriodeMedOverlapp(LocalDate førsteUttaksdatoSøknad, UttakResultatEntitet uttakResultat) {
        List<UttakResultatPeriodeEntitet> uttakPerioder = uttakResultat.getGjeldendePerioder().getPerioder();
        return uttakPerioder
            .stream()
            .filter(p -> p.overlapper(førsteUttaksdatoSøknad))
            .findFirst();
    }

    private boolean harManueltSattFørsteUttaksdato(BehandlingReferanse revurdering) {
        return finnManueltSattFørsteUttaksdato(revurdering).isPresent();
    }

    private Optional<LocalDate> finnManueltSattFørsteUttaksdato(BehandlingReferanse revurdering) {
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(revurdering.getBehandlingId());
        if (ytelseFordelingAggregat.isPresent()) {
            return ytelseFordelingAggregat.get().getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);
        }
        return Optional.empty();
    }

    private boolean arbeidsforholdRelevantForUttakErEndret(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var forrigeBehandling = finnForrigeBehandling(ref);
        var uttakForrigeBehandling = uttakRepository.hentUttakResultatHvisEksisterer(forrigeBehandling);
        if (uttakForrigeBehandling.isEmpty()) {
            return false;
        }
        var bgStatuser = input.getBeregningsgrunnlagStatuser();
        var uttakAktiviteter = aktiviteterIUttak(uttakForrigeBehandling.get());
        for (var status : bgStatuser) {
            if (!finnesAktivitetIUttakLikBgStatus(uttakAktiviteter, status)) {
                return true;
            }
        }
        for (UttakAktivitetEntitet uttakAktivitet : uttakAktiviteter) {
            if (!finnesBgStatusLikUttakAktivitet(bgStatuser, uttakAktivitet)) {
                return true;
            }
        }
        if (bgStatuser.size() > 1 || uttakAktiviteter.size() > 1) {
            return aktivitetIFørsteUttaksperiodeHarStartdatoEtterPerioden(input, uttakForrigeBehandling.get());
        }
        return false;
    }

    private boolean aktivitetIFørsteUttaksperiodeHarStartdatoEtterPerioden(UttakInput input, UttakResultatEntitet uttakForrigeBehandling) {
        var gjeldendePerioder = uttakForrigeBehandling.getGjeldendePerioder().getPerioder();
        if (gjeldendePerioder.isEmpty()) {
            return false;
        }
        var førstePeriodeForrigeUttak = gjeldendePerioder.get(0);
        return førstePeriodeForrigeUttak.getAktiviteter().stream().anyMatch(a -> startdatoEtterDato(a, førstePeriodeForrigeUttak.getFom(), new UttakYrkesaktiviteter(input)));
    }

    private boolean startdatoEtterDato(UttakResultatPeriodeAktivitetEntitet aktivitet, LocalDate dato, UttakYrkesaktiviteter yrkesaktiviteter) {
        if (aktivitet.getUttakAktivitet().getArbeidsgiver().isEmpty()) {
            return false;
        }
        var startdato = yrkesaktiviteter.finnStartdato(aktivitet.getUttakAktivitet().getArbeidsgiver().get(),
            aktivitet.getUttakAktivitet().getArbeidsforholdRef()).orElse(LocalDate.MIN);
        return startdato.isAfter(dato);
    }

    private boolean finnesBgStatusLikUttakAktivitet(Collection<BeregningsgrunnlagStatus> statuser, UttakAktivitetEntitet uttakAktivitet) {
        return statuser.stream().anyMatch(status -> aktivitetLikBgStatus(status, uttakAktivitet));
    }

    private Set<UttakAktivitetEntitet> aktiviteterIUttak(UttakResultatEntitet uttak) {
        return uttak.getGjeldendePerioder().getPerioder()
            .stream()
            .flatMap(p -> p.getAktiviteter().stream())
            .map(aktivitet -> aktivitet.getUttakAktivitet())
            .collect(Collectors.toSet());
    }

    private boolean finnesAktivitetIUttakLikBgStatus(Set<UttakAktivitetEntitet> uttakAktiviteter, BeregningsgrunnlagStatus bgStatus) {
        return uttakAktiviteter.stream().anyMatch(aktivitet -> aktivitetLikBgStatus(bgStatus, aktivitet));
    }

    private boolean aktivitetLikBgStatus(BeregningsgrunnlagStatus bgStatus, UttakAktivitetEntitet aktivitet) {
        if (aktivitet.getUttakArbeidType().equals(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return bgStatus.erSelvstendigNæringsdrivende();
        }
        if (aktivitet.getUttakArbeidType().equals(UttakArbeidType.FRILANS)) {
            return bgStatus.erFrilanser();
        }
        if (aktivitet.getUttakArbeidType().equals(UttakArbeidType.ORDINÆRT_ARBEID)) {
            return ordinærtArbeidAktivitetLikStatus(bgStatus, aktivitet);
        }
        if (aktivitet.getUttakArbeidType().equals(UttakArbeidType.ANNET)) {
            return !bgStatus.erArbeidstaker() && !bgStatus.erFrilanser() && !bgStatus.erSelvstendigNæringsdrivende();
        }
        throw new IllegalStateException("Ukjent uttakArbeidType " + aktivitet.getUttakArbeidType());
    }

    private boolean ordinærtArbeidAktivitetLikStatus(BeregningsgrunnlagStatus status, UttakAktivitetEntitet a) {
        if (!status.erArbeidstaker()) {
            return false;
        }
        Optional<Arbeidsgiver> arbeidsgiver = status.getArbeidsgiver();
        if (arbeidsgiver.isEmpty()) {
            return a.getArbeidsgiver().isEmpty();
        }
        if (a.getArbeidsgiver().isEmpty()) {
            return false;
        }
        Optional<InternArbeidsforholdRef> arbeidsforholdRef = status.getArbeidsforholdRef();
        return Objects.equals(a.getArbeidsgiver().get().getIdentifikator(), arbeidsgiver.get().getIdentifikator()) &&
            Objects.equals(a.getArbeidsforholdRef(), arbeidsforholdRef.orElse(InternArbeidsforholdRef.nullRef()));
    }

    private EndringsdatoType finnEndringsdatoTypeVedAdopsjon(ForeldrepengerGrunnlag fpGrunnlag) {
        if (fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getAnkomstNorge().isPresent()) {
            return EndringsdatoType.ANKOMST_NORGE_DATO;
        } else {
            return EndringsdatoType.OMSORGSOVERTAKELSEDATO;
        }
    }

    private LocalDate finnFørsteDato(Set<EndringsdatoType> endringsdatoer, BehandlingReferanse ref, ForeldrepengerGrunnlag fpGrunnlag) {
        Set<LocalDate> datoer = new HashSet<>();

        for (EndringsdatoType endringsdatoType : endringsdatoer) {
            switch (endringsdatoType) {
                case FØDSELSDATO:
                    fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFødselsdato().ifPresent(datoer::add);
                    break;
                case FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK:
                    finnFørsteUttaksdato(finnForrigeBehandling(ref)).ifPresent(datoer::add);
                    break;
                case FØRSTE_UTTAKSDATO_SØKNAD:
                    finnFørsteUttaksdatoSøknad(ref.getBehandlingId()).ifPresent(datoer::add);
                    break;
                case SISTE_UTTAKSDATO_GJELDENDE_VEDTAK:
                    finnEndringsdatoForEndringssøknad(ref, datoer);
                    break;
                case ENDRINGSDATO_I_BEHANDLING_SOM_FØRTE_TIL_BERØRT_BEHANDLING:
                    finnEndringsdatoForBerørtBehandling(ref, fpGrunnlag, datoer);
                    break;
                case MANUELT_SATT_FØRSTE_UTTAKSDATO:
                    finnManueltSattFørsteUttaksdato(ref).ifPresent(datoer::add);
                    break;
                case OMSORGSOVERTAKELSEDATO:
                    fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getOmsorgsovertakelse().ifPresent(datoer::add);
                    break;
                case ANKOMST_NORGE_DATO:
                    fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getAnkomstNorge().ifPresent(datoer::add);
                    break;
                case FØRSTE_UTTAKSDATO_SØKNAD_FORRIGE_BEHANDLING:
                    finnFørsteUttaksdatoSøknadForrigeBehandling(ref).ifPresent(datoer::add);
                    break;
                default:
                    new IllegalStateException("Støtter ikke EndringsdatoType. " + endringsdatoType);
            }
        }

        Optional<LocalDate> tidligst = datoer.stream().min(LOCAL_DATE_COMPARATOR);
        return tidligst.orElseThrow(() -> new IllegalStateException("Finner ikke endringsdato. " + endringsdatoer)); // NOSONAR
    }

    private Optional<LocalDate> finnFørsteUttaksdatoSøknadForrigeBehandling(BehandlingReferanse revurdering) {
        return finnFørsteUttaksdatoSøknad(finnForrigeBehandling(revurdering));
    }

    private void finnEndringsdatoForEndringssøknad(BehandlingReferanse revurdering, Set<LocalDate> datoer) {
        Optional<LocalDate> datoen = finnSisteUttaksdatoGjeldendeVedtak(finnForrigeBehandling(revurdering));
        if (datoen.isPresent()) {
            datoer.add(Virkedager.plusVirkedager(datoen.get(), 1));
        }
    }

    private void finnEndringsdatoForBerørtBehandling(BehandlingReferanse ref, ForeldrepengerGrunnlag fpGrunnlag, Set<LocalDate> datoer) {
        var annenpartBehandling = fpGrunnlag.getAnnenpart()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Berørt behandling uten innvilget vedtak annen forelders behandling - skal ikke skje"))
            .getGjeldendeVedtakBehandlingId();
        Optional<LocalDate> annenpartsFørsteUttaksdato = finnFørsteUttaksdato(annenpartBehandling);
        Optional<LocalDate> førsteUttaksdatoGjeldendeVedtak = finnFørsteUttaksdato(finnForrigeBehandling(ref));

        if (førsteUttaksdatoGjeldendeVedtak.isPresent() && annenpartsFørsteUttaksdato.isPresent()) {
            if (førsteUttaksdatoGjeldendeVedtak.get().isAfter(annenpartsFørsteUttaksdato.get())) {
                datoer.add(førsteUttaksdatoGjeldendeVedtak.get());
            } else {
                datoer.add(annenpartsFørsteUttaksdato.get());
            }
        } else {
            if (førsteUttaksdatoGjeldendeVedtak.isPresent()) {
                datoer.add(førsteUttaksdatoGjeldendeVedtak.get());
            }
            if (annenpartsFørsteUttaksdato.isPresent()) {
                datoer.add(annenpartsFørsteUttaksdato.get());
            }
        }
    }

    private Long finnForrigeBehandling(BehandlingReferanse behandling) {
        return behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
    }

    private VLException kanIkkeUtledeException(Long behandlingId) {
        return FastsettUttaksgrunnlagFeil.FACTORY.kunneIkkeUtledeEndringsdato(behandlingId).toException();
    }
}
