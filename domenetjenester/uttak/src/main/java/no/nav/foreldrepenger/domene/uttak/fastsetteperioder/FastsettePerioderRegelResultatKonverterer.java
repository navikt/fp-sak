package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.mapArbeidsgiver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatDokRegelEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.FastsettePeriodeResultat;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype;

@ApplicationScoped
public class FastsettePerioderRegelResultatKonverterer {

    private FpUttakRepository fpUttakRepository;
    private YtelsesFordelingRepository ytelsesfordelingRepository;

    FastsettePerioderRegelResultatKonverterer() {
        // For CDI
    }

    @Inject
    public FastsettePerioderRegelResultatKonverterer(FpUttakRepository fpUttakRepository,
                                                     YtelsesFordelingRepository ytelsesfordelingRepository) {
        this.fpUttakRepository = fpUttakRepository;
        this.ytelsesfordelingRepository = ytelsesfordelingRepository;
    }

    UttakResultatPerioderEntitet konverter(UttakInput input,
                                           List<FastsettePeriodeResultat> resultat) {
        var ref = input.getBehandlingReferanse();
        Long behandlingId = ref.getBehandlingId();
        OppgittFordelingEntitet oppgittFordeling = hentOppgittFordeling(behandlingId);
        LocalDate søknadMottattDato = input.getSøknadMottattDato();
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();

        List<PeriodeSøknad> periodeSøknader = lagPeriodeSøknader(oppgittFordeling, søknadMottattDato);
        List<FastsettePeriodeResultat> resultatSomSkalKonverteres = resultat.stream()
            .sorted(Comparator.comparing(periodeRes -> periodeRes.getUttakPeriode().getFom()))
            .collect(Collectors.toList());

        var uttakAktiviteter = lagUttakAktiviteter(resultat);
        for (FastsettePeriodeResultat fastsettePeriodeResultat : resultatSomSkalKonverteres) {
            UttakResultatPeriodeEntitet periode = lagUttakResultatPeriode(
                fastsettePeriodeResultat,
                periodeSomHarUtledetResultat(fastsettePeriodeResultat, periodeSøknader),
                uttakAktiviteter, new UttakYrkesaktiviteter(input));
            perioder.leggTilPeriode(periode);
        }
        if (ref.erRevurdering()) {
            prependPerioderFraOriginalBehandling(ref, perioder);
        }
        return perioder;
    }

    private void prependPerioderFraOriginalBehandling(BehandlingReferanse ref, UttakResultatPerioderEntitet perioder) {
        Long originalBehandling = ref.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        Optional<UttakResultatEntitet> opprinneligUttak = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling);
        if (opprinneligUttak.isPresent()) {
            LocalDate endringsdato = ytelsesfordelingRepository.hentAggregat(ref.getBehandlingId()).getGjeldendeEndringsdato();
            List<UttakResultatPeriodeEntitet> perioderFørEndringsdato = FastsettePerioderRevurderingUtil.perioderFørDato(opprinneligUttak.get(),
                endringsdato);
            prependPerioder(perioderFørEndringsdato, perioder);
        }
    }

    private void prependPerioder(List<UttakResultatPeriodeEntitet> perioderFørEndringsdato, UttakResultatPerioderEntitet perioderEtterEndringsdato) {
        for (UttakResultatPeriodeEntitet periodeFørEndringsdato : perioderFørEndringsdato) {
            perioderEtterEndringsdato.leggTilPeriode(periodeFørEndringsdato);
        }
    }

    private List<PeriodeSøknad> lagPeriodeSøknader(OppgittFordelingEntitet oppgittFordeling,
                                                   LocalDate søknadMottattDato) {
        return oppgittFordeling.getOppgittePerioder().stream()
            .map(oppgittPeriode -> lagPeriodeSøknad(oppgittPeriode, søknadMottattDato))
            .collect(Collectors.toList());
    }

    private Set<UttakAktivitetEntitet> lagUttakAktiviteter(List<FastsettePeriodeResultat> resultat) {
        return resultat.stream()
            .flatMap(periode -> periode.getUttakPeriode().getAktiviteter().stream())
            .map(a -> a.getIdentifikator())
            .map(this::lagUttakAktivitet)
            .collect(Collectors.toSet());
    }

    private UttakAktivitetEntitet riktigUttakAktivitet(AktivitetIdentifikator aktivitet,
                                                       Set<UttakAktivitetEntitet> uttakAktiviteter) {
        return uttakAktiviteter.stream()
            .filter(uttakAktivitet -> Objects.equals(lagArbeidType(aktivitet), uttakAktivitet.getUttakArbeidType()) &&
                Objects.equals(aktivitet.getArbeidsforholdId(), uttakAktivitet.getArbeidsforholdRef().getReferanse())
                && Objects.equals(aktivitet.getArbeidsgiverIdentifikator(), uttakAktivitet.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null)))
            .findFirst().orElse(null);
    }

    private UttakResultatPeriodeEntitet lagUttakResultatPeriode(FastsettePeriodeResultat resultat,
                                                                UttakResultatPeriodeSøknadEntitet periodeSøknad,
                                                                Set<UttakAktivitetEntitet> uttakAktiviteter,
                                                                UttakYrkesaktiviteter uttakYrkesaktiviteter) {
        UttakPeriode uttakPeriode = resultat.getUttakPeriode();

        UttakResultatDokRegelEntitet dokRegel = lagDokRegel(resultat);
        UttakResultatPeriodeEntitet periode = lagPeriode(uttakPeriode, dokRegel, periodeSøknad);

        // Legger ikke aktivitet for oppholdsperiode
        if (!erOppholdsperiode(uttakPeriode)) {
            guardMinstEnAktivitet(uttakPeriode);
            for (var aktivitet : uttakPeriode.getAktiviteter()) {
                UttakResultatPeriodeAktivitetEntitet periodeAktivitet = lagPeriodeAktivitet(uttakAktiviteter, uttakPeriode, periode, aktivitet, uttakYrkesaktiviteter);
                periode.leggTilAktivitet(periodeAktivitet);
            }
        }

        return periode;
    }

    private void guardMinstEnAktivitet(UttakPeriode uttakPeriode) {
        if (uttakPeriode.getAktiviteter().isEmpty()) {
            throw new IllegalStateException("Forventer minst en aktivitet i uttaksperiode " + uttakPeriode.getFom()
                + " - " + uttakPeriode.getTom() + " - " + uttakPeriode.getStønadskontotype());
        }
    }

    private UttakResultatPeriodeAktivitetEntitet lagPeriodeAktivitet(Set<UttakAktivitetEntitet> uttakAktiviteter,
                                                                     UttakPeriode uttakPeriode,
                                                                     UttakResultatPeriodeEntitet periode,
                                                                     UttakPeriodeAktivitet aktivitet,
                                                                     UttakYrkesaktiviteter uttakYrkesaktiviteter) {
        UttakAktivitetEntitet uttakAktivitet = riktigUttakAktivitet(aktivitet.getIdentifikator(), uttakAktiviteter);
        return UttakResultatPeriodeAktivitetEntitet.builder(periode, uttakAktivitet)
            .medTrekkonto(UttakEnumMapper.map(uttakPeriode.getStønadskontotype()))
            .medTrekkdager(map(aktivitet))
            .medUtbetalingsgrad(aktivitet.getUtbetalingsgrad())
            .medArbeidsprosent(finnArbeidsprosent(uttakPeriode, aktivitet, uttakYrkesaktiviteter))
            .medErSøktGradering(aktivitet.isSøktGradering())
            .build();
    }

    private BigDecimal finnArbeidsprosent(UttakPeriode uttakPeriode,
                                          UttakPeriodeAktivitet aktivitet,
                                          UttakYrkesaktiviteter uttakYrkesaktiviteter) {
        if (aktivitet.isSøktGradering()) {
            return uttakPeriode.getArbeidsprosent();
        }
        if (erUtsettelsePgaArbeid(uttakPeriode) && erArbeidMedArbeidsgiver(aktivitet)) {
            return uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(mapArbeidsgiver(aktivitet.getIdentifikator()),
                InternArbeidsforholdRef.ref(aktivitet.getIdentifikator().getArbeidsforholdId()), uttakPeriode.getFom());
        }
        return BigDecimal.ZERO;
    }

    private boolean erArbeidMedArbeidsgiver(UttakPeriodeAktivitet aktivitet) {
        return aktivitet.getIdentifikator().getAktivitetType().equals(AktivitetType.ARBEID) &&
            aktivitet.getIdentifikator().getArbeidsgiverIdentifikator() != null;
    }

    private boolean erUtsettelsePgaArbeid(UttakPeriode uttakPeriode) {
        return Objects.equals(uttakPeriode.getUtsettelseÅrsak(), UtsettelseÅrsak.ARBEID);
    }

    private Trekkdager map(UttakPeriodeAktivitet aktivitet) {
        return new Trekkdager(aktivitet.getTrekkdager().decimalValue());
    }

    private UttakResultatPeriodeSøknadEntitet periodeSomHarUtledetResultat(FastsettePeriodeResultat resultat,
                                                                           List<PeriodeSøknad> periodeSøknader) {
        return periodeSøknader.stream()
            .filter(søknad -> søknad.harUtledet(resultat.getUttakPeriode()))
            .map(søknad -> søknad.entitet)
            .findFirst().orElse(null);
    }

    private OppgittFordelingEntitet hentOppgittFordeling(Long behandlingId) {
        return ytelsesfordelingRepository.hentAggregat(behandlingId).getGjeldendeSøknadsperioder();
    }

    private UttakResultatDokRegelEntitet lagDokRegel(FastsettePeriodeResultat resultat) {
        ManuellBehandlingÅrsak manuellBehandlingÅrsak = UttakEnumMapper.map(resultat.getUttakPeriode().getManuellbehandlingårsak());
        UttakResultatDokRegelEntitet.Builder builder = resultat.isManuellBehandling() ?
            UttakResultatDokRegelEntitet.medManuellBehandling(manuellBehandlingÅrsak) :
            UttakResultatDokRegelEntitet.utenManuellBehandling();
        return builder
            .medRegelEvaluering(resultat.getEvalueringResultat())
            .medRegelInput(resultat.getInnsendtGrunnlag())
            .build();
    }

    private UttakAktivitetEntitet lagUttakAktivitet(AktivitetIdentifikator aktivitetIdentifikator) {
        UttakAktivitetEntitet.Builder builder = new UttakAktivitetEntitet.Builder();
        String arbeidsgiverIdentifikator = aktivitetIdentifikator.getArbeidsgiverIdentifikator();
        if (arbeidsgiverIdentifikator != null) {
            InternArbeidsforholdRef arbeidsforholdRef = aktivitetIdentifikator.getArbeidsforholdId() == null ? null
                : InternArbeidsforholdRef.ref(aktivitetIdentifikator.getArbeidsforholdId());
            if (Objects.equals(aktivitetIdentifikator.getArbeidsgiverType(), AktivitetIdentifikator.ArbeidsgiverType.VIRKSOMHET)) {
                builder.medArbeidsforhold(Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator), arbeidsforholdRef);
            } else if (Objects.equals(aktivitetIdentifikator.getArbeidsgiverType(), AktivitetIdentifikator.ArbeidsgiverType.PERSON)) {
                builder.medArbeidsforhold(Arbeidsgiver.person(new AktørId(arbeidsgiverIdentifikator)), arbeidsforholdRef);
            } else {
                throw new IllegalStateException("Støtter ikke arbeidsgiver type " + aktivitetIdentifikator.getArbeidsgiverType());
            }
        }

        return builder
            .medUttakArbeidType(lagArbeidType(aktivitetIdentifikator))
            .build();
    }

    static UttakArbeidType lagArbeidType(AktivitetIdentifikator aktivitetIdentifikator) {
        AktivitetType aktivitetType = aktivitetIdentifikator.getAktivitetType();
        if (AktivitetType.ARBEID.equals(aktivitetType)) {
            return UttakArbeidType.ORDINÆRT_ARBEID;
        }
        if (AktivitetType.FRILANS.equals(aktivitetType)) {
            return UttakArbeidType.FRILANS;
        }
        if (AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(aktivitetType)) {
            return UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
        }
        return UttakArbeidType.ANNET;
    }

    private UttakResultatPeriodeEntitet lagPeriode(UttakPeriode uttakPeriode,
                                                   UttakResultatDokRegelEntitet dokRegel,
                                                   UttakResultatPeriodeSøknadEntitet periodeSøknad) {
        PeriodeResultatType periodeResultatType = UttakEnumMapper.map(uttakPeriode.getPerioderesultattype());
        return new UttakResultatPeriodeEntitet.Builder(uttakPeriode.getFom(), uttakPeriode.getTom())
            .medResultatType(periodeResultatType, UttakEnumMapper.map(periodeResultatType, uttakPeriode.getPeriodeResultatÅrsak()))
            .medDokRegel(dokRegel)
            .medGraderingInnvilget(uttakPeriode.erGraderingInnvilget())
            .medUtsettelseType(toUtsettelseType(uttakPeriode))
            .medGraderingAvslagÅrsak(UttakEnumMapper.map(uttakPeriode.getGraderingIkkeInnvilgetÅrsak()))
            .medOppholdÅrsak(tilOppholdÅrsak(uttakPeriode))
            .medOverføringÅrsak(tilOverføringÅrsak(uttakPeriode))
            .medPeriodeSoknad(periodeSøknad)
            .medSamtidigUttak(uttakPeriode.erSamtidigUttak())
            .medSamtidigUttaksprosent(uttakPeriode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(uttakPeriode.isFlerbarnsdager())
            .build();
    }

    private OppholdÅrsak tilOppholdÅrsak(UttakPeriode uttakPeriode) {
        if (erOppholdsperiode(uttakPeriode)) {
            Stønadskontotype stønadskontotype = uttakPeriode.getStønadskontotype();
            switch (stønadskontotype) {
                case MØDREKVOTE:
                    return OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
                case FEDREKVOTE:
                    return OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
                case FELLESPERIODE:
                    return OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER;
                case FORELDREPENGER:
                    return OppholdÅrsak.KVOTE_FORELDREPENGER_ANNEN_FORELDER;
                default:
                    throw new IllegalArgumentException("Utvikler-feil: Kom ut av regel med stønadskontotype" + stønadskontotype);
            }
        }

        return OppholdÅrsak.UDEFINERT;
    }

    private no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak tilOverføringÅrsak(UttakPeriode uttakPeriode) {
        var overføringÅrsak = uttakPeriode.getOverføringÅrsak();
        if (Objects.equals(overføringÅrsak, OverføringÅrsak.INNLEGGELSE)) {
            return no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
        } else if (Objects.equals(overføringÅrsak, OverføringÅrsak.SYKDOM_ELLER_SKADE)) {
            return no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
        } else if (Objects.equals(overføringÅrsak, OverføringÅrsak.ANNEN_FORELDER_IKKE_RETT)) {
            return no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
        } else if (Objects.equals(overføringÅrsak, OverføringÅrsak.ALENEOMSORG)) {
            return no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.ALENEOMSORG;
        } else {
            return no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.UDEFINERT;
        }
    }

    private boolean erOppholdsperiode(UttakPeriode uttakPeriode) {
        return uttakPeriode.getOppholdÅrsak() != null;
    }

    private UttakUtsettelseType toUtsettelseType(UttakPeriode uttakPeriode) {
        if (uttakPeriode.getUtsettelseÅrsak() != null) {
            return UttakEnumMapper.map(uttakPeriode.getUtsettelseÅrsak());
        }
        return UttakUtsettelseType.UDEFINERT;
    }

    private PeriodeSøknad lagPeriodeSøknad(OppgittPeriodeEntitet oppgittPeriode, LocalDate søknadMottattDato) {
        UttakResultatPeriodeSøknadEntitet.Builder builder = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medGraderingArbeidsprosent(oppgittPeriode.getArbeidsprosent())
            .medUttakPeriodeType(oppgittPeriode.getPeriodeType())
            .medMottattDato(søknadMottattDato)
            .medMottattDatoTemp(oppgittPeriode.getMottattDato())
            .medMorsAktivitet(oppgittPeriode.getMorsAktivitet())
            .medSamtidigUttak(oppgittPeriode.isSamtidigUttak())
            .medSamtidigUttaksprosent(oppgittPeriode.getSamtidigUttaksprosent());
        UttakResultatPeriodeSøknadEntitet entitet = builder.build();

        return new PeriodeSøknad(entitet, oppgittPeriode.getFom(), oppgittPeriode.getTom());
    }

    private static class PeriodeSøknad {
        private final UttakResultatPeriodeSøknadEntitet entitet;
        private final LocalDate fom;
        private final LocalDate tom;

        private PeriodeSøknad(UttakResultatPeriodeSøknadEntitet entitet, LocalDate fom, LocalDate tom) {
            this.entitet = entitet;
            this.fom = fom;
            this.tom = tom;
        }

        boolean harUtledet(UttakPeriode uttakPeriode) {
            return (uttakPeriode.getFom().isEqual(fom) || uttakPeriode.getFom().isAfter(fom))
                && (uttakPeriode.getTom().isEqual(tom) || uttakPeriode.getTom().isBefore(tom));
        }
    }
}
