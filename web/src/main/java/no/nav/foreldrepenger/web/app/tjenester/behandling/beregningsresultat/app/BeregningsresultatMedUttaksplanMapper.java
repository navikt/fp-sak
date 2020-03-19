package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatMedUttaksplanDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatPeriodeAndelDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.UttakDto;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
public class BeregningsresultatMedUttaksplanMapper {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningsresultatMedUttaksplanMapper() {
        // For inject
    }

    @Inject
    public BeregningsresultatMedUttaksplanMapper(ArbeidsgiverTjeneste arbeidsgiverTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    BeregningsresultatMedUttaksplanDto lagBeregningsresultatMedUttaksplan(
        Behandling behandling,
        BehandlingBeregningsresultatEntitet beregningsresultatAggregat, Optional<UttakResultatEntitet> uttakResultat
    ) {
        return BeregningsresultatMedUttaksplanDto.build()
            .medSokerErMor(getSøkerErMor(behandling))
            .medOpphoersdato(getOpphørsdato(uttakResultat).orElse(null))
            .medPerioder(lagPerioder(behandling.getId(), beregningsresultatAggregat.getBgBeregningsresultatFP(), uttakResultat))
            .medUtbetPerioder(beregningsresultatAggregat.getUtbetBeregningsresultatFP() == null ? null : lagPerioder(behandling.getId(), beregningsresultatAggregat.getUtbetBeregningsresultatFP(), uttakResultat))
            .medSkalHindreTilbaketrekk(beregningsresultatAggregat.skalHindreTilbaketrekk().orElse(null))
            .create();
    }

    private Optional<LocalDate> getOpphørsdato(Optional<UttakResultatEntitet> uttakResultat) {
        if (!uttakResultat.isPresent() || uttakResultat.get().getGjeldendePerioder().getPerioder().isEmpty()) {
            return Optional.empty();
        }
        Set<PeriodeResultatÅrsak> opphørsAvslagÅrsaker = IkkeOppfyltÅrsak.opphørsAvslagÅrsaker();
        List<UttakResultatPeriodeEntitet> perioder = uttakResultat.get().getGjeldendePerioder().getPerioder()
            .stream()
            .sorted(Comparator.comparing(UttakResultatPeriodeEntitet::getFom).reversed())
            .collect(Collectors.toList());
        // Sjekker om siste periode er avslått med en opphørsårsak
        UttakResultatPeriodeEntitet sistePeriode = perioder.remove(0);
        if (!opphørsAvslagÅrsaker.contains(sistePeriode.getResultatÅrsak())) {
            return Optional.empty();
        }
        LocalDate opphørsdato = sistePeriode.getFom();
        for (UttakResultatPeriodeEntitet periode : perioder) {
            if (opphørsAvslagÅrsaker.contains(periode.getResultatÅrsak()) && periode.getFom().isBefore(opphørsdato)) {
                opphørsdato = periode.getFom();
            } else {
                return Optional.ofNullable(opphørsdato);
            }
        }
        return Optional.of(opphørsdato);
    }

    private boolean getSøkerErMor(Behandling behandling) {
        return RelasjonsRolleType.MORA.equals(behandling.getFagsak().getRelasjonsRolleType());
    }

    List<BeregningsresultatPeriodeDto> lagPerioder(long behandlingId, BeregningsresultatEntitet beregningsresultat, Optional<UttakResultatEntitet> uttakResultat) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        List<BeregningsresultatPeriode> beregningsresultatPerioder = beregningsresultat.getBeregningsresultatPerioder();
        Map<Tuple<AktivitetStatus, Optional<String>>, Optional<LocalDate>> andelTilSisteUtbetalingsdatoMap = finnSisteUtbetalingdatoForAlleAndeler(beregningsresultatPerioder);
        return beregningsresultatPerioder.stream()
            .sorted(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(beregningsresultatPeriode -> BeregningsresultatPeriodeDto.build()
                .medFom(beregningsresultatPeriode.getBeregningsresultatPeriodeFom())
                .medTom(beregningsresultatPeriode.getBeregningsresultatPeriodeTom())
                .medDagsats(beregningsresultatPeriode.getDagsats())
                .medAndeler(lagAndeler(beregningsresultatPeriode, uttakResultat, andelTilSisteUtbetalingsdatoMap, iayGrunnlag))
                .create())
            .collect(Collectors.toList());
    }

    List<BeregningsresultatPeriodeAndelDto> lagAndeler(BeregningsresultatPeriode beregningsresultatPeriode, Optional<UttakResultatEntitet> uttakResultat, Map<Tuple<AktivitetStatus,
        Optional<String>>, Optional<LocalDate>> andelTilSisteUtbetalingsdatoMap, Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag) {

        List<BeregningsresultatAndel> beregningsresultatAndelList = beregningsresultatPeriode.getBeregningsresultatAndelList();

        // grupper alle andeler som har samme aktivitetstatus og arbeidsforholdId og legg dem i en tuple med hendholdsvis brukers og arbeidsgivers andel
        List<Tuple<BeregningsresultatAndel, Optional<BeregningsresultatAndel>>> andelListe = genererAndelListe(beregningsresultatAndelList);
        return andelListe.stream()
            .map(andelPar -> {
                BeregningsresultatAndel brukersAndel = andelPar.getElement1();
                Optional<BeregningsresultatAndel> arbeidsgiversAndel = andelPar.getElement2();
                Optional<Arbeidsgiver> arbeidsgiver = brukersAndel.getArbeidsgiver();
                BeregningsresultatPeriodeAndelDto.Builder dtoBuilder = BeregningsresultatPeriodeAndelDto.build()
                    .medRefusjon(arbeidsgiversAndel.map(BeregningsresultatAndel::getDagsats).orElse(0))
                    .medTilSøker(brukersAndel.getDagsats())
                    .medUtbetalingsgrad(brukersAndel.getUtbetalingsgrad())
                    .medSisteUtbetalingsdato(andelTilSisteUtbetalingsdatoMap.getOrDefault(genererAndelKey(brukersAndel), Optional.empty()).orElse(null))
                    .medAktivitetstatus(brukersAndel.getAktivitetStatus())
                    .medArbeidsforholdId(brukersAndel.getArbeidsforholdRef() != null
                        ? brukersAndel.getArbeidsforholdRef().getReferanse() : null)
                    .medAktørId(arbeidsgiver.filter(Arbeidsgiver::erAktørId).map(Arbeidsgiver::getAktørId).map(AktørId::getId).orElse(null))
                    .medArbeidsforholdType(brukersAndel.getArbeidsforholdType())
                    .medUttak(lagUttak(uttakResultat, beregningsresultatPeriode, brukersAndel))
                    .medStillingsprosent(brukersAndel.getStillingsprosent());
                var internArbeidsforholdId = brukersAndel.getArbeidsforholdRef() != null ? brukersAndel.getArbeidsforholdRef().getReferanse() : null;
                dtoBuilder.medArbeidsforholdId(internArbeidsforholdId);
                iayGrunnlag.ifPresent(iay -> iay.getArbeidsforholdInformasjon().ifPresent(arbeidsforholdInformasjon -> {
                    if (internArbeidsforholdId != null && arbeidsgiver.isPresent()) {
                        var eksternArbeidsforholdRef = arbeidsforholdInformasjon.finnEkstern(arbeidsgiver.get(), brukersAndel.getArbeidsforholdRef());
                        dtoBuilder.medEksternArbeidsforholdId(eksternArbeidsforholdRef.getReferanse());
                    }
                }));
                arbeidsgiver.ifPresent(arb -> settArbeidsgiverfelter(arb, dtoBuilder));
                return dtoBuilder
                    .create();
            })
            .collect(Collectors.toList());
    }

    private void settArbeidsgiverfelter(Arbeidsgiver arb, BeregningsresultatPeriodeAndelDto.Builder dtoBuilder) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arb);
        if (opplysninger != null) {
            dtoBuilder.medArbeidsgiverNavn(opplysninger.getNavn());
            dtoBuilder.medArbeidsgiverOrgnr(opplysninger.getIdentifikator());
        } else {
            throw new IllegalStateException("Finner ikke arbeidsgivers identifikator");
        }
    }

    private Map<Tuple<AktivitetStatus, Optional<String>>, Optional<LocalDate>> finnSisteUtbetalingdatoForAlleAndeler(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        Collector<BeregningsresultatAndel, ?, Optional<LocalDate>> maxTomDatoCollector = Collectors.mapping(andel -> andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeTom(),
            Collectors.maxBy(Comparator.naturalOrder()));
        return beregningsresultatPerioder.stream()
            .flatMap(brp -> brp.getBeregningsresultatAndelList().stream())
            .filter(andel -> andel.getDagsats() > 0)
            .collect(Collectors.groupingBy(this::genererAndelKey, maxTomDatoCollector));
    }

    private Tuple<AktivitetStatus, Optional<String>> genererAndelKey(BeregningsresultatAndel andel) {
        return new Tuple<>(andel.getAktivitetStatus(), finnSekundærIdentifikator(andel));
    }

    private List<Tuple<BeregningsresultatAndel, Optional<BeregningsresultatAndel>>> genererAndelListe(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        Map<Tuple<AktivitetStatus, Optional<String>>, List<BeregningsresultatAndel>> collect = beregningsresultatAndelList.stream()
            .collect(Collectors.groupingBy(this::genererAndelKey));

        return collect.values().stream().map(andeler -> {
            BeregningsresultatAndel brukerAndel = andeler.stream()
                .filter(BeregningsresultatAndel::erBrukerMottaker)
                .reduce(this::slåSammenAndeler)
                .orElseThrow(() -> new IllegalStateException("Utvilkerfeil: Mangler andel for bruker, men skal alltid ha andel for bruker her."));

            Optional<BeregningsresultatAndel> arbeidsgiverAndel = andeler.stream()
                .filter(a -> !a.erBrukerMottaker())
                .reduce(this::slåSammenAndeler);

            return new Tuple<>(brukerAndel, arbeidsgiverAndel);
        })
            .collect(Collectors.toList());
    }

    private Optional<String> finnSekundærIdentifikator(BeregningsresultatAndel andel) {
        // Denne metoden finner sekundæridentifikator for andelen, etter aktivitetstatus.
        // Mulige identifikatorer i prioritert rekkefølge:
        // 1. arbeidsforholdId
        // 2. orgNr
        if (andel.getArbeidsforholdRef() != null && andel.getArbeidsforholdRef().getReferanse() != null) {
            return Optional.of(andel.getArbeidsforholdRef().getReferanse());
        } else return andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator);
    }

    private UttakDto lagUttak(Optional<UttakResultatEntitet> uttakResultat,
                              BeregningsresultatPeriode beregningsresultatPeriode,
                              BeregningsresultatAndel brukersAndel) {

        if (!uttakResultat.isPresent()) {
            return UttakDto.build().create();
        }

        List<UttakResultatPeriodeEntitet> perioder = uttakResultat.get().getGjeldendePerioder().getPerioder();

        return perioder.stream()
            .findAny()
            .map(uttakResultatPerArbeidsforhold -> finnTilhørendeUttakPeriodeAktivitet(perioder, beregningsresultatPeriode))
            .map(uttakResultatPeriode -> lagUttakDto(uttakResultatPeriode, brukersAndel))
            .orElseThrow(() -> new IllegalArgumentException("UttakResultatEntitet inneholder ikke resultater for gitt arbeidsforholdId."));
    }

    private UttakDto lagUttakDto(UttakResultatPeriodeEntitet uttakResultatPeriode, BeregningsresultatAndel brukersAndel) {
        List<UttakResultatPeriodeAktivitetEntitet> aktiviteter = uttakResultatPeriode.getAktiviteter();
        UttakResultatPeriodeAktivitetEntitet korrektUttakAndel = finnKorrektUttaksAndel(brukersAndel, aktiviteter);
        return UttakDto.build()
            .medStønadskontoType(korrektUttakAndel.getTrekkonto())
            .medPeriodeResultatType(uttakResultatPeriode.getResultatType())
            .medGradering(korrektUttakAndel.isGraderingInnvilget())
            .create();
    }

    private UttakResultatPeriodeAktivitetEntitet finnKorrektUttaksAndel(BeregningsresultatAndel brukersAndel, List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        if (brukersAndel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER)) {
            return førsteAvType(aktiviteter, UttakArbeidType.FRILANS);
        } else if (brukersAndel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return førsteAvType(aktiviteter, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
        } else if (brukersAndel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)) {
            return finnKorrektArbeidstakerAndel(brukersAndel, aktiviteter);
        } else {
            return førsteAvType(aktiviteter, UttakArbeidType.ANNET);
        }
    }

    private UttakResultatPeriodeAktivitetEntitet førsteAvType(List<UttakResultatPeriodeAktivitetEntitet> aktiviteter, UttakArbeidType type) {
        return aktiviteter.stream()
            .filter(a -> a.getUttakArbeidType().equals(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke periodeaktivitet fra uttak for uttak arbeid type " + type));
    }

    private UttakResultatPeriodeAktivitetEntitet finnKorrektArbeidstakerAndel(BeregningsresultatAndel brukersAndel, List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        List<UttakResultatPeriodeAktivitetEntitet> korrekteAktiviteter = finnKorrekteAktiviteter(brukersAndel, aktiviteter);
        if (korrekteAktiviteter.size() != 1) {
            throw new IllegalArgumentException("Forventet akkurat 1 uttakaktivitet for beregningsresultat andel " + brukersAndel.getAktivitetStatus() + " "
                + brukersAndel.getArbeidsforholdIdentifikator() + " " + brukersAndel.getArbeidsforholdRef() + ". Antall matchende aktiviteter var " + korrekteAktiviteter.size());

        }
        return korrekteAktiviteter.get(0);
    }

    private List<UttakResultatPeriodeAktivitetEntitet> finnKorrekteAktiviteter(BeregningsresultatAndel brukersAndel, List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        return aktiviteter.stream()
            .filter(aktivitet -> Objects.equals(brukersAndel.getArbeidsgiver().orElse(null), aktivitet.getArbeidsgiver()))
            .filter(aktivitet -> Objects.equals(brukersAndel.getArbeidsforholdRef(), aktivitet.getArbeidsforholdRef()))
            .filter(aktivitet -> Objects.equals(UttakArbeidType.ORDINÆRT_ARBEID, aktivitet.getUttakArbeidType()))
            .collect(Collectors.toList());
    }

    private UttakResultatPeriodeEntitet finnTilhørendeUttakPeriodeAktivitet(Collection<UttakResultatPeriodeEntitet> uttakResultatPerioder,
                                                                            BeregningsresultatPeriode beregningsresultatPeriode) {
        return uttakResultatPerioder.stream()
            .filter(uttakResultatPeriode -> !uttakResultatPeriode.getFom().isAfter(beregningsresultatPeriode.getBeregningsresultatPeriodeFom()))
            .filter(uttakResultatPeriode -> !uttakResultatPeriode.getTom().isBefore(beregningsresultatPeriode.getBeregningsresultatPeriodeTom()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("BeregningsresultatPeriode tilhører ikke noen periode fra UttakResultatEntitet"));
    }

    private BeregningsresultatAndel slåSammenAndeler(BeregningsresultatAndel a, BeregningsresultatAndel b) {
        InternArbeidsforholdRef førsteArbeidsforholdId = a.getArbeidsforholdRef();
        InternArbeidsforholdRef andreArbeidsforholdId = b.getArbeidsforholdRef();
        boolean harUlikeArbeidsforholdIder = false;
        if (førsteArbeidsforholdId != null && andreArbeidsforholdId != null) {
            harUlikeArbeidsforholdIder = !Objects.equals(førsteArbeidsforholdId.getReferanse(), andreArbeidsforholdId.getReferanse());
        }
        if (harUlikeArbeidsforholdIder
            || a.getUtbetalingsgrad().compareTo(b.getUtbetalingsgrad()) != 0
            || a.getStillingsprosent().compareTo(b.getStillingsprosent()) != 0
            || !a.getBeregningsresultatPeriode().equals(b.getBeregningsresultatPeriode())) {
            throw new IllegalStateException("Utviklerfeil: Andeler som slås sammen skal ikke ha ulikt arbeidsforhold, periode, stillingsprosent eller utbetalingsgrad");
        }
        BeregningsresultatAndel ny = Kopimaskin.deepCopy(a, a.getBeregningsresultatPeriode());
        BeregningsresultatAndel.builder(ny)
            .medDagsats(a.getDagsats() + b.getDagsats())
            .medDagsatsFraBg(a.getDagsatsFraBg() + b.getDagsatsFraBg());
        return ny;
    }
}
