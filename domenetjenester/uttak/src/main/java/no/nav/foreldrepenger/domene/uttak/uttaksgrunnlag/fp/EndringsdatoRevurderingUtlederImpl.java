package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.RelevanteArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
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

    @Inject
    public EndringsdatoRevurderingUtlederImpl(UttakRepositoryProvider repositoryProvider,
                                              DekningsgradTjeneste dekningsgradTjeneste,
                                              RelevanteArbeidsforholdTjeneste relevanteArbeidsforholdTjeneste) {
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.relevanteArbeidsforholdTjeneste = relevanteArbeidsforholdTjeneste;
    }

    EndringsdatoRevurderingUtlederImpl() {
        // CDI
    }

    @Override
    public LocalDate utledEndringsdato(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();
        var endringsdatoTypeEnumSet = utledEndringsdatoTyper(input);
        if (endringsdatoTypeEnumSet.isEmpty()) {
            endringsdatoTypeEnumSet.add(EndringsdatoType.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
            LOG.info("Kunne ikke utlede endringsdato for revurdering med behandlingId=" + behandlingId
                + ". Satte FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.");
        }
        var endringsdato = utledEndringsdato(ref, endringsdatoTypeEnumSet, input.getYtelsespesifiktGrunnlag());
        return endringsdato.orElseThrow(() -> FastsettUttaksgrunnlagFeil.kunneIkkeUtledeEndringsdato(behandlingId));
    }

    private Optional<LocalDate> utledEndringsdato(BehandlingReferanse ref,
                                                  EnumSet<EndringsdatoType> endringsdatoTypeEnumSet,
                                                  ForeldrepengerGrunnlag fpGrunnlag) {
        var endringsdato = finnFørsteDato(endringsdatoTypeEnumSet, ref, fpGrunnlag);
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
        if (førsteDatoSomIkkeErTaptTilAnnenpart.isEmpty() && uttakPerioder.size() > 0) {
            return Optional.ofNullable(uttakPerioder.get(0).getFom());
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
                                     ForeldrepengerGrunnlag fpGrunnlag) {
        Set<LocalDate> datoer = new HashSet<>();

        for (var endringsdatoType : endringsdatoer) {
            switch (endringsdatoType) {
                case FØDSELSDATO -> fpGrunnlag.getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getFødselsdato()
                    .ifPresent(datoer::add);
                case FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK -> finnFørsteUttaksdato(finnForrigeBehandling(ref)).ifPresent(
                    datoer::add);
                case FØRSTE_UTTAKSDATO_SØKNAD -> finnFørsteUttaksdatoSøknad(ref.behandlingId()).ifPresent(
                    datoer::add);
                case SISTE_UTTAKSDATO_GJELDENDE_VEDTAK -> finnEndringsdatoForEndringssøknad(ref, datoer);
                case ENDRINGSDATO_I_BEHANDLING_SOM_FØRTE_TIL_BERØRT_BEHANDLING -> finnEndringsdatoForBerørtBehandling(
                    ref, fpGrunnlag, datoer);
                case MANUELT_SATT_FØRSTE_UTTAKSDATO -> finnManueltSattFørsteUttaksdato(ref).ifPresent(datoer::add);
                case OMSORGSOVERTAKELSEDATO -> fpGrunnlag.getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getOmsorgsovertakelse()
                    .ifPresent(datoer::add);
                case ANKOMST_NORGE_DATO -> fpGrunnlag.getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getAnkomstNorge()
                    .ifPresent(datoer::add);
                case FØRSTE_UTTAKSDATO_SØKNAD_FORRIGE_BEHANDLING -> finnFørsteUttaksdatoSøknadForrigeBehandling(
                    ref).ifPresent(datoer::add);
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
                                                     ForeldrepengerGrunnlag fpGrunnlag,
                                                     Set<LocalDate> datoer) {
        var annenpartBehandling = fpGrunnlag.getAnnenpart()
            .orElseThrow(() -> new IllegalStateException(
                "Utviklerfeil: Berørt behandling uten innvilget vedtak annen forelders behandling - skal ikke skje"))
            .gjeldendeVedtakBehandlingId();
        var annenpartsFørsteUttaksdato = finnFørsteUttaksdato(annenpartBehandling);
        var førsteUttaksdatoGjeldendeVedtak = finnFørsteUttaksdato(finnForrigeBehandling(ref));

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
            .orElseThrow(() -> new IllegalStateException(
                "Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
    }
}
