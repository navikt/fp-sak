package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatMedUttaksplanDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatPeriodeAndelDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.UttakDto;

@ApplicationScoped
public class BeregningsresultatMedUttaksplanMapper {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningsresultatMedUttaksplanMapper() {
        // For inject
    }

    @Inject
    public BeregningsresultatMedUttaksplanMapper(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    BeregningsresultatMedUttaksplanDto lagBeregningsresultatMedUttaksplan(BehandlingReferanse behandlingReferanse,
                                                                          BehandlingBeregningsresultatEntitet beregningsresultatAggregat,
                                                                          Optional<ForeldrepengerUttak> uttak) {
        return new BeregningsresultatMedUttaksplanDto(lagPerioder(behandlingReferanse.behandlingId(), beregningsresultatAggregat.getBgBeregningsresultatFP(), uttak));
    }

    List<BeregningsresultatPeriodeDto> lagPerioder(long behandlingId, BeregningsresultatEntitet beregningsresultat, Optional<ForeldrepengerUttak> uttak) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        var beregningsresultatPerioder = beregningsresultat.getBeregningsresultatPerioder();
        var andelTilSisteUtbetalingsdatoMap = finnSisteUtbetalingdatoForAlleAndeler(beregningsresultatPerioder);
        return beregningsresultatPerioder.stream()
            .sorted(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(beregningsresultatPeriode -> BeregningsresultatPeriodeDto.build()
                .medFom(beregningsresultatPeriode.getBeregningsresultatPeriodeFom())
                .medTom(beregningsresultatPeriode.getBeregningsresultatPeriodeTom())
                .medDagsats(beregningsresultatPeriode.getDagsats())
                .medAndeler(lagAndeler(beregningsresultatPeriode, uttak, andelTilSisteUtbetalingsdatoMap, iayGrunnlag))
                .create())
            .toList();
    }

    List<BeregningsresultatPeriodeAndelDto> lagAndeler(BeregningsresultatPeriode beregningsresultatPeriode,
                                                       Optional<ForeldrepengerUttak> uttak,
                                                       Map<AktivitetStatusMedIdentifikator, Optional<LocalDate>> andelTilSisteUtbetalingsdatoMap,
                                                       Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag) {

        var beregningsresultatAndelList = beregningsresultatPeriode.getBeregningsresultatAndelList();

        // grupper alle andeler som har samme aktivitetstatus og arbeidsforholdId og legg dem i en tuple med hendholdsvis brukers og arbeidsgivers andel
        var andelListe = genererAndelListe(beregningsresultatAndelList);
        return andelListe.stream()
            .map(andelPar -> {
                var brukersAndel = andelPar.bruker();
                var arbeidsgiversAndel = andelPar.arbeidsgiver();
                var arbeidsgiver = Optional.ofNullable(brukersAndel.arbeidsgiver());
                var dtoBuilder = BeregningsresultatPeriodeAndelDto.build()
                    .medRefusjon(arbeidsgiversAndel.map(LokalBRAndel::dagsats).orElse(0))
                    .medTilSøker(brukersAndel.dagsats())
                    .medUtbetalingsgrad(brukersAndel.utbetalingsgrad())
                    .medSisteUtbetalingsdato(andelTilSisteUtbetalingsdatoMap.getOrDefault(genererAndelKey(brukersAndel), Optional.empty()).orElse(null))
                    .medAktivitetstatus(brukersAndel.aktivitetStatus())
                    .medArbeidsforholdId(brukersAndel.arbeidsforholdRef() != null
                        ? brukersAndel.arbeidsforholdRef().getReferanse() : null)
                    .medAktørId(arbeidsgiver.filter(Arbeidsgiver::erAktørId).map(Arbeidsgiver::getAktørId).map(AktørId::getId).orElse(null))
                    .medArbeidsforholdType(brukersAndel.arbeidsforholdType())
                    .medUttak(lagUttak(uttak, beregningsresultatPeriode, brukersAndel))
                    .medStillingsprosent(brukersAndel.stillingsprosent());
                var internArbeidsforholdId = brukersAndel.arbeidsforholdRef() != null ? brukersAndel.arbeidsforholdRef().getReferanse() : null;
                dtoBuilder.medArbeidsforholdId(internArbeidsforholdId);
                iayGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon).ifPresent(arbeidsforholdInformasjon -> {
                    if (internArbeidsforholdId != null && arbeidsgiver.isPresent()) {
                        var eksternArbeidsforholdRef = arbeidsforholdInformasjon.finnEkstern(arbeidsgiver.get(), brukersAndel.arbeidsforholdRef);
                        dtoBuilder.medEksternArbeidsforholdId(eksternArbeidsforholdRef.getReferanse());
                    }
                });
                arbeidsgiver.ifPresent(arb -> settArbeidsgiverfelter(arb, dtoBuilder));
                return dtoBuilder
                    .create();
            })
            .toList();
    }

    private void settArbeidsgiverfelter(Arbeidsgiver arb, BeregningsresultatPeriodeAndelDto.Builder dtoBuilder) {
        dtoBuilder.medArbeidsgiverReferanse(arb.getIdentifikator());
    }

    private record AktivitetStatusMedIdentifikator(AktivitetStatus aktivitetStatus, Optional<String> idenfifikator) {}

    private Map<AktivitetStatusMedIdentifikator, Optional<LocalDate>> finnSisteUtbetalingdatoForAlleAndeler(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        Collector<BeregningsresultatAndel, ?, Optional<LocalDate>> maxTomDatoCollector = Collectors.mapping(andel -> andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeTom(),
            Collectors.maxBy(Comparator.naturalOrder()));
        return beregningsresultatPerioder.stream()
            .flatMap(brp -> brp.getBeregningsresultatAndelList().stream())
            .filter(andel -> andel.getDagsats() > 0)
            .collect(Collectors.groupingBy(this::genererAndelKey, maxTomDatoCollector));
    }

    private AktivitetStatusMedIdentifikator genererAndelKey(BeregningsresultatAndel andel) {
        return new AktivitetStatusMedIdentifikator(andel.getAktivitetStatus(), finnSekundærIdentifikator(andel.getArbeidsgiver(), andel.getArbeidsforholdRef()));
    }

    private AktivitetStatusMedIdentifikator genererAndelKey(LokalBRAndel andel) {
        return new AktivitetStatusMedIdentifikator(andel.aktivitetStatus, finnSekundærIdentifikator(Optional.ofNullable(andel.arbeidsgiver()), andel.arbeidsforholdRef()));
    }

    private record AndelerBrukerAG(LokalBRAndel bruker, Optional<LokalBRAndel> arbeidsgiver) {}

    private record LokalBRAndel(Boolean brukerErMottaker, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef,
                                OpptjeningAktivitetType arbeidsforholdType, Integer dagsats, BigDecimal stillingsprosent,
                                BigDecimal utbetalingsgrad, Integer dagsatsFraBg, BeregningsresultatPeriode beregningsresultatPeriode,
                                AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori) {

        static LokalBRAndel fraAndel(BeregningsresultatAndel andel) {
            return new LokalBRAndel(andel.erBrukerMottaker(), andel.getArbeidsgiver().orElse(null), andel.getArbeidsforholdRef(),
                andel.getArbeidsforholdType(), andel.getDagsats(), andel.getStillingsprosent(), andel.getUtbetalingsgrad(), andel.getDagsatsFraBg(),
                andel.getBeregningsresultatPeriode(), andel.getAktivitetStatus(), andel.getInntektskategori());
        }

        static LokalBRAndel slåSammen(LokalBRAndel andel, Integer dagsats, Integer dagsatsFraBg) {
            return new LokalBRAndel(andel.brukerErMottaker(), andel.arbeidsgiver(), andel.arbeidsforholdRef(),
                andel.arbeidsforholdType(), dagsats, andel.stillingsprosent(), andel.utbetalingsgrad(), dagsatsFraBg,
                andel.beregningsresultatPeriode(), andel.aktivitetStatus(), andel.inntektskategori());
        }
    }

    private List<AndelerBrukerAG> genererAndelListe(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        var collect = beregningsresultatAndelList.stream()
            .collect(Collectors.groupingBy(this::genererAndelKey));

        return collect.values().stream().map(andeler -> {
            var brukerAndel = andeler.stream()
                .filter(BeregningsresultatAndel::erBrukerMottaker)
                .map(LokalBRAndel::fraAndel)
                .reduce(this::slåSammenAndeler)
                .orElseThrow(() -> new IllegalStateException("Utvilkerfeil: Mangler andel for bruker, men skal alltid ha andel for bruker her."));

            var arbeidsgiverAndel = andeler.stream()
                .filter(a -> !a.erBrukerMottaker())
                .map(LokalBRAndel::fraAndel)
                .reduce(this::slåSammenAndeler);

            return new AndelerBrukerAG(brukerAndel, arbeidsgiverAndel);
        })
            .toList();
    }

    private Optional<String> finnSekundærIdentifikator(Optional<Arbeidsgiver> arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        // Denne metoden finner sekundæridentifikator for andelen, etter aktivitetstatus.
        // Mulige identifikatorer i prioritert rekkefølge:
        // 1. arbeidsforholdId
        // 2. orgNr
        if (arbeidsforholdRef != null && arbeidsforholdRef.getReferanse() != null) {
            return Optional.of(arbeidsforholdRef.getReferanse());
        }
        return arbeidsgiver.map(Arbeidsgiver::getIdentifikator);
    }

    private UttakDto lagUttak(Optional<ForeldrepengerUttak> uttak,
                                        BeregningsresultatPeriode beregningsresultatPeriode,
                                        LokalBRAndel brukersAndel) {

        if (uttak.isEmpty()) {
            return UttakDto.build().create();
        }

        var perioder = uttak.get().getGjeldendePerioder();
        var uttakDto = perioder.stream()
            .findAny()
            .map(uttakPerArbeidsforhold -> finnTilhørendeUttakPeriodeAktivitet(perioder, beregningsresultatPeriode))
            .flatMap(uttakPeriode -> lagUttakDto(uttakPeriode, brukersAndel));
        return uttakDto.orElseGet(() -> UttakDto.build().create());
    }

    private Optional<UttakDto> lagUttakDto(ForeldrepengerUttakPeriode uttakPeriode, LokalBRAndel brukersAndel) {
        var aktiviteter = uttakPeriode.getAktiviteter();
        var korrektUttakAndelOpt = finnKorrektUttaksAndel(brukersAndel, aktiviteter);
        return korrektUttakAndelOpt.map(foreldrepengerUttakPeriodeAktivitet -> UttakDto.build()
            .medStønadskontoType(foreldrepengerUttakPeriodeAktivitet.getTrekkonto())
            .medPeriodeResultatType(uttakPeriode.getResultatType())
            .medGradering(erGraderingInnvilgetForAktivitet(uttakPeriode, foreldrepengerUttakPeriodeAktivitet))
            .create());
    }

    private boolean erGraderingInnvilgetForAktivitet(ForeldrepengerUttakPeriode uttakPeriode,
                                                     ForeldrepengerUttakPeriodeAktivitet korrektUttakAndel) {
        return uttakPeriode.isGraderingInnvilget() && korrektUttakAndel.isSøktGraderingForAktivitetIPeriode();
    }

    private Optional<ForeldrepengerUttakPeriodeAktivitet> finnKorrektUttaksAndel(LokalBRAndel brukersAndel, List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
        if (brukersAndel.aktivitetStatus().equals(AktivitetStatus.FRILANSER)) {
            return førsteAvType(aktiviteter, UttakArbeidType.FRILANS);
        }
        if (brukersAndel.aktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return førsteAvType(aktiviteter, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
        }
        if (brukersAndel.aktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)) {
            return finnKorrektArbeidstakerAndel(brukersAndel, aktiviteter);
        }
        return førsteAvType(aktiviteter, UttakArbeidType.ANNET);
    }

    private Optional<ForeldrepengerUttakPeriodeAktivitet> førsteAvType(List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter, UttakArbeidType type) {
        return aktiviteter.stream()
            .filter(a -> a.getUttakArbeidType().equals(type))
            .findFirst();
    }

    private Optional<ForeldrepengerUttakPeriodeAktivitet> finnKorrektArbeidstakerAndel(LokalBRAndel brukersAndel, List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
        var korrekteAktiviteter = finnKorrekteAktiviteter(brukersAndel, aktiviteter);
        if (korrekteAktiviteter.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(korrekteAktiviteter.get(0));
    }

    private List<ForeldrepengerUttakPeriodeAktivitet> finnKorrekteAktiviteter(LokalBRAndel brukersAndel, List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
        return aktiviteter.stream()
            .filter(aktivitet -> Objects.equals(brukersAndel.arbeidsgiver(), aktivitet.getArbeidsgiver().orElse(null)))
            .filter(aktivitet -> Objects.equals(brukersAndel.arbeidsforholdRef(), aktivitet.getArbeidsforholdRef()))
            .filter(aktivitet -> Objects.equals(UttakArbeidType.ORDINÆRT_ARBEID, aktivitet.getUttakArbeidType()))
            .toList();
    }

    private ForeldrepengerUttakPeriode finnTilhørendeUttakPeriodeAktivitet(Collection<ForeldrepengerUttakPeriode> uttakResultatPerioder,
                                                                           BeregningsresultatPeriode beregningsresultatPeriode) {
        return uttakResultatPerioder.stream()
            .filter(uttakPeriode -> !uttakPeriode.getFom().isAfter(beregningsresultatPeriode.getBeregningsresultatPeriodeFom()))
            .filter(uttakPeriode -> !uttakPeriode.getTom().isBefore(beregningsresultatPeriode.getBeregningsresultatPeriodeTom()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("BeregningsresultatPeriode tilhører ikke noen periode fra UttakResultatEntitet"));
    }

    private LokalBRAndel slåSammenAndeler(LokalBRAndel a, LokalBRAndel b) {
        var førsteArbeidsforholdId = a.arbeidsforholdRef;
        var andreArbeidsforholdId = b.arbeidsforholdRef();
        var harUlikeArbeidsforholdIder = false;
        if (førsteArbeidsforholdId != null && andreArbeidsforholdId != null) {
            harUlikeArbeidsforholdIder = !Objects.equals(førsteArbeidsforholdId.getReferanse(), andreArbeidsforholdId.getReferanse());
        }
        if (harUlikeArbeidsforholdIder
            || a.utbetalingsgrad().compareTo(b.utbetalingsgrad()) != 0
            || a.stillingsprosent().compareTo(b.stillingsprosent()) != 0
            || !a.beregningsresultatPeriode().equals(b.beregningsresultatPeriode())) {
            throw new IllegalStateException("Utviklerfeil: Andeler som slås sammen skal ikke ha ulikt arbeidsforhold, periode, stillingsprosent eller utbetalingsgrad");
        }
        return LokalBRAndel.slåSammen(a, a.dagsats() + b.dagsats(), a.dagsatsFraBg() + b.dagsatsFraBg());
    }
}
