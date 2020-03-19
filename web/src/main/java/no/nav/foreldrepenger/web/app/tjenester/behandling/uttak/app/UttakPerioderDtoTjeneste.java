package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPerioderDto;

@ApplicationScoped
public class UttakPerioderDtoTjeneste {
    private UttakRepository uttakRepository;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private ArbeidsgiverDtoTjeneste arbeidsgiverDtoTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public UttakPerioderDtoTjeneste() {
        // For CDI
    }

    @Inject
    public UttakPerioderDtoTjeneste(UttakRepository uttakRepository,
                                        RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                        YtelsesFordelingRepository ytelsesFordelingRepository,
                                        ArbeidsgiverDtoTjeneste arbeidsgiverDtoTjeneste,
                                        BehandlingsresultatRepository behandlingsresultatRepository,
                                        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.uttakRepository = uttakRepository;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.arbeidsgiverDtoTjeneste = arbeidsgiverDtoTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public Optional<UttakResultatPerioderDto> mapFra(Behandling behandling) {
        Optional<YtelseFordelingAggregat> ytelseFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId());
        Optional<UttakResultatEntitet> uttaksresultatAnnenPart = annenPartUttak(behandling);
        UttakResultatPerioderDto perioder = new UttakResultatPerioderDto(finnUttakResultatPerioderSøker(behandling.getId()),
            uttaksresultatAnnenPart.map(this::finnUttakResultatPerioderAnnenpart).orElse(Collections.emptyList()),
            ytelseFordeling.map(yf -> UttakOmsorgUtil.harAnnenForelderRett(yf, uttaksresultatAnnenPart)).orElse(false),
            ytelseFordeling.map(UttakOmsorgUtil::harAleneomsorg).orElse(false));
        return Optional.of(perioder);
    }

    private Optional<UttakResultatEntitet> annenPartUttak(Behandling behandling) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingsresultat.isPresent()) {
            if (behandlingsresultat.get().getBehandlingVedtak() != null) {
                return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeUttaksplanPåVedtakstidspunkt(behandling);
            }
        }
        return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattUttaksplan(behandling.getFagsak().getSaksnummer());
    }

    private List<UttakResultatPeriodeDto> finnUttakResultatPerioderSøker(Long behandling) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandling);

        if (uttakResultat.isEmpty()) {
            return Collections.emptyList();
        }

        return finnUttakResultatPerioder(uttakResultat.get());
    }

    private List<UttakResultatPeriodeDto> finnUttakResultatPerioderAnnenpart(UttakResultatEntitet uttaksresultatAnnenPart) {
        return finnUttakResultatPerioder(uttaksresultatAnnenPart);
    }

    private List<UttakResultatPeriodeDto> finnUttakResultatPerioder(UttakResultatEntitet uttakResultat) {
        UttakResultatPerioderEntitet gjeldenePerioder = uttakResultat.getGjeldendePerioder();

        List<UttakResultatPeriodeDto> list = new ArrayList<>();

        var behandlingId = uttakResultat.getBehandlingsresultat().getBehandlingId();
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        for (UttakResultatPeriodeEntitet entitet : gjeldenePerioder.getPerioder()) {
            UttakResultatPeriodeDto periode = map(entitet, iayGrunnlag);
            list.add(periode);
        }

        return sortedByFom(list);
    }

    private UttakResultatPeriodeDto map(UttakResultatPeriodeEntitet entitet, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        UttakResultatPeriodeDto.Builder builder = new UttakResultatPeriodeDto.Builder()
            .medTidsperiode(entitet.getFom(), entitet.getTom())
            .medManuellBehandlingÅrsak(entitet.getManuellBehandlingÅrsak())
            .medUtsettelseType(entitet.getUtsettelseType())
            .medPeriodeResultatType(entitet.getPeriodeResultatType())
            .medBegrunnelse(entitet.getBegrunnelse())
            .medPeriodeResultatÅrsak(entitet.getPeriodeResultatÅrsak())
            .medFlerbarnsdager(entitet.isFlerbarnsdager())
            .medSamtidigUttak(entitet.isSamtidigUttak())
            .medSamtidigUttaksprosent(entitet.getSamtidigUttaksprosent())
            .medGraderingInnvilget(entitet.isGraderingInnvilget())
            .medGraderingAvslåttÅrsak(entitet.getGraderingAvslagÅrsak())
            .medOppholdÅrsak(entitet.getOppholdÅrsak());
        if (entitet.getPeriodeSøknad().isPresent()) {
            UttakResultatPeriodeSøknadEntitet periodeSøknad = entitet.getPeriodeSøknad().get();
            builder.medPeriodeType(periodeSøknad.getUttakPeriodeType());
        }
        UttakResultatPeriodeDto periode = builder.build();

        for (UttakResultatPeriodeAktivitetEntitet aktivitet : entitet.getAktiviteter()) {
            periode.leggTilAktivitet(map(aktivitet, inntektArbeidYtelseGrunnlag));
        }
        return periode;
    }

    private List<UttakResultatPeriodeDto> sortedByFom(List<UttakResultatPeriodeDto> list) {
        return list
            .stream()
            .sorted(Comparator.comparing(UttakResultatPeriodeDto::getFom))
            .collect(Collectors.toList());
    }

    private UttakResultatPeriodeAktivitetDto map(UttakResultatPeriodeAktivitetEntitet aktivitet, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        UttakResultatPeriodeAktivitetDto.Builder builder = new UttakResultatPeriodeAktivitetDto.Builder()
            .medProsentArbeid(aktivitet.getArbeidsprosent())
            .medGradering(aktivitet.isSøktGradering())
            .medTrekkdager(aktivitet.getTrekkdager())
            .medStønadskontoType(aktivitet.getTrekkonto())
            .medUttakArbeidType(aktivitet.getUttakArbeidType());
        mapArbeidsforhold(aktivitet, builder, inntektArbeidYtelseGrunnlag);
        if (!aktivitet.getPeriode().opprinneligSendtTilManueltBehandling()) {
            builder.medUtbetalingsgrad(aktivitet.getUtbetalingsprosent());
        }
        return builder.build();
    }

    private void mapArbeidsforhold(UttakResultatPeriodeAktivitetEntitet aktivitet, UttakResultatPeriodeAktivitetDto.Builder builder, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        var arbeidsgiverOptional = aktivitet.getUttakAktivitet().getArbeidsgiver();
        List<ArbeidsforholdOverstyring> overstyringer = inntektArbeidYtelseGrunnlag.map(InntektArbeidYtelseGrunnlag::getArbeidsforholdOverstyringer).orElse(Collections.emptyList());
        var arbeidsgiverDto = arbeidsgiverOptional.map(arbgiver -> arbeidsgiverDtoTjeneste.mapFra(arbgiver, overstyringer)).orElse(null);
        var internArbeidsforholdId = aktivitet.getArbeidsforholdId();
        if (internArbeidsforholdId != null && inntektArbeidYtelseGrunnlag.isPresent() && inntektArbeidYtelseGrunnlag.get().getArbeidsforholdInformasjon().isPresent() && arbeidsgiverOptional.isPresent()) {
            var eksternArbeidsforholdId = inntektArbeidYtelseGrunnlag.get().getArbeidsforholdInformasjon().get().finnEkstern(arbeidsgiverOptional.get(), InternArbeidsforholdRef.ref(internArbeidsforholdId));
            builder.medArbeidsforhold(internArbeidsforholdId, eksternArbeidsforholdId.getReferanse(), arbeidsgiverDto);
        } else {
            builder.medArbeidsforhold(null, null, arbeidsgiverDto);
        }
    }
}
