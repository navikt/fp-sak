package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.SvangerskapspengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktørArbeidDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktørInntektDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktørYtelseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektspostDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.KravperioderPrArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.NaturalYtelseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.PerioderForKravDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.RefusjonDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.RefusjonsperiodeDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulator.modell.typer.Beløp;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Stillingsprosent;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetGraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AndelGraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.GraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.KravperioderPrArbeidsforhold;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PerioderForKrav;
import no.nav.folketrygdloven.kalkulus.beregning.v1.Refusjonsperiode;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelseandel;
import no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelsegrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelseperiode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktivitetsgrad;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Utbetalingsgrad;
import no.nav.folketrygdloven.kalkulus.iay.IayProsent;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdInformasjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdOverstyringDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.PermisjonDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntekterDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingerDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingsPostDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelserDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseKilde;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittOpptjeningDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningPeriodeDto;
import no.nav.vedtak.konfig.Tid;

/**
 * Mapper beregningsgrunnlagInput til KalkulatorInput for bruk til feilsøking av saker
 */
class MapTilKalkulatorInput {

    private MapTilKalkulatorInput() {}

    public static KalkulatorInputDto map(BeregningsgrunnlagInput beregningsgrunnlagInput) {
        if (beregningsgrunnlagInput == null) {
            return null;
        }
        var kalkulatorInputDto = new KalkulatorInputDto(
            mapIayGrunnlag(beregningsgrunnlagInput),
            mapOpptjeningAktiviteter(beregningsgrunnlagInput.getOpptjeningAktiviteterForBeregning()),
            beregningsgrunnlagInput.getSkjæringstidspunktOpptjening());
        kalkulatorInputDto.medYtelsespesifiktGrunnlag(mapYtelsesSpesifiktGrunnlag(beregningsgrunnlagInput.getYtelsespesifiktGrunnlag()));
        kalkulatorInputDto.medRefusjonsperioderPrInntektsmelding(mapKravperioder(beregningsgrunnlagInput.getKravPrArbeidsgiver()));
        return kalkulatorInputDto;
    }

    private static List<KravperioderPrArbeidsforhold> mapKravperioder(List<KravperioderPrArbeidsforholdDto> kravPrArbeidsgiver) {
        return kravPrArbeidsgiver.stream().map(MapTilKalkulatorInput::mapKrav).toList();
    }

    private static KravperioderPrArbeidsforhold mapKrav(KravperioderPrArbeidsforholdDto k) {
        var aktør = mapArbeidsgiverNullsafe(k.getArbeidsgiver());
        var internRef = mapAbakusReferanse(k.getArbeidsforholdRef());
        var kravperioder = k.getPerioder().stream().map(MapTilKalkulatorInput::mapKravperiode).toList();
        var sisteSøktePeriode = mapSisteSøktePeriode(k); // Denne blir ikke korrekt, bør se på ny mapping her
        return new KravperioderPrArbeidsforhold(aktør, internRef, kravperioder, sisteSøktePeriode);
    }

    private static PerioderForKrav mapSisteSøktePeriode(KravperioderPrArbeidsforholdDto k) {
        var refperiode = k.getSisteSøktePerioder().stream()
            .map(p -> new Refusjonsperiode(mapPeriodeNullsafe(p), no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.ZERO))
            .toList();
        var innsending = refperiode.stream().map(p -> p.getPeriode().getFom()).min(Comparator.naturalOrder()).orElse(LocalDate.now());
        return new PerioderForKrav(innsending, refperiode);
    }

    private static PerioderForKrav mapKravperiode(PerioderForKravDto kravperiode) {
        var refusjonsperioder = kravperiode.getPerioder().stream().map(MapTilKalkulatorInput::mapRefusjonsperiode).toList();
        return new PerioderForKrav(kravperiode.getInnsendingsdato(), refusjonsperioder);
    }

    private static Refusjonsperiode mapRefusjonsperiode(RefusjonsperiodeDto refusjonsperiode) {
        return new Refusjonsperiode(mapPeriodeNullsafe(refusjonsperiode.periode()), mapTilBeløp(refusjonsperiode.beløp()));
    }

    private static AktivitetGraderingDto mapAktivitetGradering(AktivitetGradering aktivitetGradering) {
        if (aktivitetGradering == null) {
            return null;
        }
        var aktivitetGraderingDto = new AktivitetGraderingDto(mapAndelGraderinger(aktivitetGradering.getAndelGradering()));
        return aktivitetGraderingDto.getAndelGraderingDto() == null || aktivitetGraderingDto.getAndelGraderingDto().isEmpty() ? null : aktivitetGraderingDto;
    }

    private static List<AndelGraderingDto> mapAndelGraderinger(Set<AndelGradering> andelGradering) {
        return andelGradering == null ? null : andelGradering.stream().map(MapTilKalkulatorInput::mapAndelGradering).toList();
    }

    private static AndelGraderingDto mapAndelGradering(AndelGradering andelGradering) {
        if (andelGradering == null) {
            return null;
        }
        var aktivitetStatus = AktivitetStatus.fraKode(andelGradering.getAktivitetStatus().getKode());
        var arbeidsgiver = mapArbeidsgiver(andelGradering.getArbeidsgiver());
        var arbeidsforholdRef = mapAbakusReferanse(andelGradering.getArbeidsforholdRef());
        var graderinger = mapGraderinger(andelGradering.getGraderinger());
        return new AndelGraderingDto(aktivitetStatus, arbeidsgiver, arbeidsforholdRef, graderinger);
    }

    private static List<GraderingDto> mapGraderinger(List<AndelGradering.Gradering> graderinger) {
        return graderinger.stream().map(MapTilKalkulatorInput::mapGradering).toList();
    }

    private static GraderingDto mapGradering(AndelGradering.Gradering gradering) {
        return gradering == null ? null : new GraderingDto(mapPeriode(gradering.getPeriode()), Aktivitetsgrad.fra(gradering.getArbeidstidProsent().verdi()));
    }

    private static YtelsespesifiktGrunnlagDto mapYtelsesSpesifiktGrunnlag(YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag) {
        if (ytelsespesifiktGrunnlag == null) {
            return null;
        }
        if (ytelsespesifiktGrunnlag instanceof SvangerskapspengerGrunnlag svpGrunnlag) {
            var utbetalingsgradPrAktivitet = mapUtbetalingsgradPrAktivitet(svpGrunnlag.getUtbetalingsgradPrAktivitet());
            return new no.nav.folketrygdloven.kalkulus.beregning.v1.SvangerskapspengerGrunnlag(utbetalingsgradPrAktivitet, Tid.TIDENES_ENDE); // Inntill vi er klare for tilkommet inntekt
        }
        if (ytelsespesifiktGrunnlag instanceof no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag fpGrunnlag) {
            var aktivitetGraderingDto = mapAktivitetGradering(fpGrunnlag.getAktivitetGradering());
            // Forventer prosent her, så må gange opp. Brukes kun i forvaltning swaggerkall
            return new ForeldrepengerGrunnlag(BigDecimal.valueOf(fpGrunnlag.getDekningsgrad().getVerdi()).multiply(BigDecimal.valueOf(100)), fpGrunnlag.isKvalifisererTilBesteberegning(), aktivitetGraderingDto,
                mapBesteberegningYtelsegrunnlag(fpGrunnlag.getBesteberegningYtelsegrunnlag()));
        }
        return null;
    }

    private static List<Ytelsegrunnlag> mapBesteberegningYtelsegrunnlag(List<no.nav.folketrygdloven.kalkulator.modell.besteberegning.Ytelsegrunnlag> ytelsegrunnlagBB) {
        return ytelsegrunnlagBB == null ? Collections.emptyList() : ytelsegrunnlagBB.stream().map(yg -> new Ytelsegrunnlag(yg.ytelse(),mapYgPerioder(yg.perioder()))).toList();
    }

    private static List<Ytelseperiode> mapYgPerioder(List<no.nav.folketrygdloven.kalkulator.modell.besteberegning.Ytelseperiode> perioder) {
        return perioder.stream().map(p -> new Ytelseperiode(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()), mapYgAndeler(p.getAndeler()))).toList();
    }

    private static List<Ytelseandel> mapYgAndeler(List<no.nav.folketrygdloven.kalkulator.modell.besteberegning.Ytelseandel> andeler) {
        return andeler.stream().map(a -> new Ytelseandel(a.getAktivitetStatus(), a.getInntektskategori(), a.getArbeidskategori(), a.getDagsats())).toList();
    }

    private static List<UtbetalingsgradPrAktivitetDto> mapUtbetalingsgradPrAktivitet(List<no.nav.folketrygdloven.kalkulator.modell.svp.UtbetalingsgradPrAktivitetDto> utbetalingsgradPrAktivitet) {
        return utbetalingsgradPrAktivitet.stream().map(MapTilKalkulatorInput::mapUtbetalingsgradForAktivitet).toList();
    }

    private static UtbetalingsgradPrAktivitetDto mapUtbetalingsgradForAktivitet(no.nav.folketrygdloven.kalkulator.modell.svp.UtbetalingsgradPrAktivitetDto utbetalingsgradPrAktivitetDto) {
        if (utbetalingsgradPrAktivitetDto == null) {
            return null;
        }
        var utbetalingsgradArbeidsforholdDto = mapArbeidsforholdDto(utbetalingsgradPrAktivitetDto.getUtbetalingsgradArbeidsforhold());
        var periodeMedUtbetalingsgrad = mapPerioderMedUtbetalingsgrad(utbetalingsgradPrAktivitetDto.getPeriodeMedUtbetalingsgrad());
        return new UtbetalingsgradPrAktivitetDto(utbetalingsgradArbeidsforholdDto, periodeMedUtbetalingsgrad);
    }

    private static List<PeriodeMedUtbetalingsgradDto> mapPerioderMedUtbetalingsgrad(List<no.nav.folketrygdloven.kalkulator.modell.svp.PeriodeMedUtbetalingsgradDto> periodeMedUtbetalingsgrad) {
        return periodeMedUtbetalingsgrad.stream().map(MapTilKalkulatorInput::mapPeriodeMedUtbetalingsgrad).toList();
    }

    private static PeriodeMedUtbetalingsgradDto mapPeriodeMedUtbetalingsgrad(no.nav.folketrygdloven.kalkulator.modell.svp.PeriodeMedUtbetalingsgradDto periodeMedUtbetalingsgradDto) {
        return periodeMedUtbetalingsgradDto == null ? null
            : new PeriodeMedUtbetalingsgradDto(
                mapPeriode(periodeMedUtbetalingsgradDto.getPeriode()),
                Utbetalingsgrad.fra(periodeMedUtbetalingsgradDto.getUtbetalingsgrad().verdi()));
    }

    private static AktivitetDto mapArbeidsforholdDto(no.nav.folketrygdloven.kalkulator.modell.svp.AktivitetDto utbetalingsgradArbeidsforhold) {
        var arbeidsgiver = utbetalingsgradArbeidsforhold.getArbeidsgiver().map(MapTilKalkulatorInput::mapArbeidsgiverNullsafe).orElse(null);
        var internArbeidsforholdRef = mapAbakusReferanse(utbetalingsgradArbeidsforhold.getInternArbeidsforholdRef());
        var uttakArbeidType = utbetalingsgradArbeidsforhold.getUttakArbeidType();
        return new AktivitetDto(arbeidsgiver, internArbeidsforholdRef, uttakArbeidType);
    }

    private static OpptjeningAktiviteterDto mapOpptjeningAktiviteter(Collection<no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningAktiviteterForBeregning) {
        return new OpptjeningAktiviteterDto(mapOpptjeningPerioder(opptjeningAktiviteterForBeregning));
    }

    private static List<OpptjeningPeriodeDto> mapOpptjeningPerioder(Collection<no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningAktiviteterForBeregning) {
        return opptjeningAktiviteterForBeregning.stream().map(MapTilKalkulatorInput::mapOpptjeningPeriode).toList();
    }

    private static OpptjeningPeriodeDto mapOpptjeningPeriode(no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto.OpptjeningPeriodeDto opptjeningPeriodeDto) {
        return new OpptjeningPeriodeDto(
            OpptjeningAktivitetType.fraKode(opptjeningPeriodeDto.getOpptjeningAktivitetType().getKode()),
            mapPeriode(opptjeningPeriodeDto.getPeriode()),
            mapArbeidsgiverNullsafe(opptjeningPeriodeDto.getArbeidsgiverOrgNummer(), opptjeningPeriodeDto.getArbeidsgiverAktørId()),
            mapAbakusReferanse(opptjeningPeriodeDto.getArbeidsforholdId()));
    }

    private static Aktør mapArbeidsgiverNullsafe(String arbeidsgiverOrgNummer, String arbeidsgiverAktørId) {
        if (arbeidsgiverOrgNummer == null && arbeidsgiverAktørId == null) {
            return null;
        }
        return arbeidsgiverOrgNummer == null ? new AktørIdPersonident(arbeidsgiverAktørId) : new Organisasjon(arbeidsgiverOrgNummer);
    }

    private static InntektArbeidYtelseGrunnlagDto mapIayGrunnlag(BeregningsgrunnlagInput beregningsgrunnlagInput) {
        var iayGrunnlag = beregningsgrunnlagInput.getIayGrunnlag();
        var inntektArbeidYtelseGrunnlagDto = new InntektArbeidYtelseGrunnlagDto();
        inntektArbeidYtelseGrunnlagDto.medArbeidDto(mapArbeidDto(iayGrunnlag.getAktørArbeidFraRegister()));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapArbeidsforholdInformasjon(iayGrunnlag.getArbeidsforholdInformasjon()));
        inntektArbeidYtelseGrunnlagDto.medInntekterDto(mapInntekter(iayGrunnlag.getAktørInntektFraRegister()));
        inntektArbeidYtelseGrunnlagDto.medInntektsmeldingerDto(mapInntektsmeldingerDto(iayGrunnlag.getInntektsmeldinger()));
        inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapOppgittOpptjening(iayGrunnlag.getOppgittOpptjening()));
        inntektArbeidYtelseGrunnlagDto.medYtelserDto(mapYtelserDto(iayGrunnlag.getAktørYtelseFraRegister()));
        return inntektArbeidYtelseGrunnlagDto;
    }

    private static YtelserDto mapYtelserDto(Optional<AktørYtelseDto> aktørYtelseFraRegister) {
        return aktørYtelseFraRegister.map(aktørYtelseDto -> new YtelserDto(mapYtelser(aktørYtelseDto.getAlleYtelser()))).orElse(null);
    }

    private static List<YtelseDto> mapYtelser(Collection<no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDto> alleYtelser) {
        return alleYtelser == null ? null : alleYtelser.stream().map(MapTilKalkulatorInput::mapYtelse).toList();
    }

    private static YtelseDto mapYtelse(no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDto ytelseDto) {
        if (ytelseDto == null) {
            return null;
        }
        var vedtaksDagsats = ytelseDto.getVedtaksDagsats().map(b -> no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(b.verdi())).orElse(null);
        var ytelseAnvist = mapYtelseAnvistSet(ytelseDto.getYtelseAnvist());
        var relatertYtelseType = ytelseDto.getYtelseType();
        var periode = mapPeriode(ytelseDto.getPeriode());
        return new YtelseDto(vedtaksDagsats, ytelseAnvist, relatertYtelseType, periode, ytelseDto.getYtelseKilde().orElse(YtelseKilde.UDEFINERT));
    }

    private static Set<no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto> mapYtelseAnvistSet(Collection<YtelseAnvistDto> ytelseAnvist) {
        return ytelseAnvist == null ? null : ytelseAnvist.stream().map(MapTilKalkulatorInput::mapYtelseAnvist).collect(Collectors.toSet());
    }

    private static no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto mapYtelseAnvist(YtelseAnvistDto ytelseAnvistDto) {
        return ytelseAnvistDto == null ? null
            : new no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto(
                new Periode(ytelseAnvistDto.getAnvistFOM(), ytelseAnvistDto.getAnvistTOM()),
                ytelseAnvistDto.getBeløp().map(Beløp::verdi).map(no.nav.folketrygdloven.kalkulus.felles.v1.Beløp::new).orElse(null),
                ytelseAnvistDto.getDagsats().map(Beløp::verdi).map(no.nav.folketrygdloven.kalkulus.felles.v1.Beløp::new).orElse(null),
                ytelseAnvistDto.getUtbetalingsgradProsent().map(Stillingsprosent::verdi).map(IayProsent::new).orElse(null),
                Collections.emptyList());
    }

    private static OppgittOpptjeningDto mapOppgittOpptjening(Optional<no.nav.folketrygdloven.kalkulator.modell.iay.OppgittOpptjeningDto> oppgittOpptjening) {
        return oppgittOpptjening.map(oppgittOpptjeningDto -> new OppgittOpptjeningDto(
            null,
            mapFrilans(oppgittOpptjeningDto.getFrilans()),
            mapNæringer(oppgittOpptjeningDto.getEgenNæring()),
            null)).orElse(null);
    }

    private static List<OppgittEgenNæringDto> mapNæringer(List<no.nav.folketrygdloven.kalkulator.modell.iay.OppgittEgenNæringDto> egenNæring) {
        return egenNæring == null ? null : egenNæring.stream().map(MapTilKalkulatorInput::mapNæring).toList();
    }

    private static OppgittEgenNæringDto mapNæring(no.nav.folketrygdloven.kalkulator.modell.iay.OppgittEgenNæringDto oppgittEgenNæringDto) {
        if (oppgittEgenNæringDto == null) {
            return null;
        }
        return new OppgittEgenNæringDto(mapPeriode(oppgittEgenNæringDto.getPeriode()),
            oppgittEgenNæringDto.getOrgnr() == null ? null : new Organisasjon(oppgittEgenNæringDto.getOrgnr()),
            oppgittEgenNæringDto.getVirksomhetType() == null ? null : VirksomhetType.fraKode(oppgittEgenNæringDto.getVirksomhetType().getKode()),
            oppgittEgenNæringDto.getNyoppstartet(), oppgittEgenNæringDto.getVarigEndring(), oppgittEgenNæringDto.getEndringDato(),
            oppgittEgenNæringDto.getNyIArbeidslivet(), oppgittEgenNæringDto.getBegrunnelse(), mapTilBeløp(oppgittEgenNæringDto.getBruttoInntekt()));
    }

    private static OppgittFrilansDto mapFrilans(Optional<no.nav.folketrygdloven.kalkulator.modell.iay.OppgittFrilansDto> frilans) {
        return frilans.map(oppgittFrilansDto -> new OppgittFrilansDto(oppgittFrilansDto.getErNyoppstartet())).orElse(null);
    }

    private static InntektsmeldingerDto mapInntektsmeldingerDto(Optional<InntektsmeldingAggregatDto> inntektsmeldinger) {
        return inntektsmeldinger.map(inntektsmeldingAggregatDto -> new InntektsmeldingerDto(
            mapInntektsmeldinger(inntektsmeldingAggregatDto.getAlleInntektsmeldinger())))
            .orElse(null);
    }

    private static List<InntektsmeldingDto> mapInntektsmeldinger(List<no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDto> alleInntektsmeldinger) {
        return alleInntektsmeldinger == null ? null : alleInntektsmeldinger.stream().map(MapTilKalkulatorInput::mapInntektsmelding).toList();
    }

    private static InntektsmeldingDto mapInntektsmelding(no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDto inntektsmeldingDto) {
        return inntektsmeldingDto == null ? null
            : new InntektsmeldingDto(
                mapArbeidsgiver(inntektsmeldingDto.getArbeidsgiver()),
                new no.nav.folketrygdloven.kalkulus.felles.v1.Beløp(inntektsmeldingDto.getInntektBeløp().verdi()),
                mapNaturalYtelser(inntektsmeldingDto.getNaturalYtelser()),
                mapRefusjonEndringer(inntektsmeldingDto.getEndringerRefusjon()),
                mapAbakusReferanse(inntektsmeldingDto.getArbeidsforholdRef()),
                inntektsmeldingDto.getStartDatoPermisjon().orElse(null),
                inntektsmeldingDto.getRefusjonOpphører(),
                mapTilBeløp(inntektsmeldingDto.getRefusjonBeløpPerMnd()),
                inntektsmeldingDto.getJournalpostId());
    }

    private static List<no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto> mapRefusjonEndringer(List<RefusjonDto> endringerRefusjon) {
        return endringerRefusjon == null ? null : endringerRefusjon.stream().map(MapTilKalkulatorInput::mapRefusjonEndring).toList();
    }

    private static no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto mapRefusjonEndring(RefusjonDto refusjonDto) {
        return refusjonDto == null ? null
            : new no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto(
                mapTilBeløp(refusjonDto.getRefusjonsbeløp()),
                refusjonDto.getFom());
    }

    private static List<no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto> mapNaturalYtelser(List<NaturalYtelseDto> naturalYtelser) {
        return naturalYtelser == null ? null : naturalYtelser.stream().map(MapTilKalkulatorInput::mapNaturalYtelse).toList();
    }

    private static no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto mapNaturalYtelse(NaturalYtelseDto naturalYtelseDto) {
        if (naturalYtelseDto == null) {
            return null;
        }
        return new no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto(mapPeriodeNullsafe(naturalYtelseDto.getPeriode()),
            mapTilBeløp(naturalYtelseDto.getBeloepPerMnd()),
            naturalYtelseDto.getType() == null ? null : NaturalYtelseType.fraKode(naturalYtelseDto.getType().getKode()));
    }

    private static InntekterDto mapInntekter(Optional<AktørInntektDto> aktørInntektFraRegister) {
        return aktørInntektFraRegister.map(aktørInntektDto -> new InntekterDto(mapUtbetalinger(aktørInntektDto.getInntekt()))).orElse(null);
    }

    private static List<UtbetalingDto> mapUtbetalinger(Collection<InntektDto> inntekt) {
        return inntekt == null ? null : inntekt.stream().map(MapTilKalkulatorInput::mapUtbetaling).toList();
    }

    private static UtbetalingDto mapUtbetaling(InntektDto inntektDto) {
        if (inntektDto == null) {
            return null;
        }
        var utbetalingDto = new UtbetalingDto(InntektskildeType.fraKode(inntektDto.getInntektsKilde().getKode()), mapPoster(inntektDto.getAlleInntektsposter()));
        utbetalingDto.setArbeidsgiver(mapArbeidsgiverNullsafe(inntektDto.getArbeidsgiver()));
        return utbetalingDto;
    }

    private static List<UtbetalingsPostDto> mapPoster(Collection<InntektspostDto> alleInntektsposter) {
        return alleInntektsposter.stream().map(MapTilKalkulatorInput::mapPost).toList();
    }

    private static UtbetalingsPostDto mapPost(InntektspostDto inntektspostDto) {
        if (inntektspostDto == null) {
            return null;
        }
        var utbetaling = new UtbetalingsPostDto(
            mapPeriodeNullsafe(inntektspostDto.getPeriode()),
            inntektspostDto.getInntektspostType() == null
                ? InntektspostType.fraKode(no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType.UDEFINERT.getKode())
                : InntektspostType.fraKode(inntektspostDto.getInntektspostType().getKode()),
            mapTilBeløp(inntektspostDto.getBeløp()));
        var skatteOgAvgiftsregelType = inntektspostDto.getSkatteOgAvgiftsregelType() != null
            ? SkatteOgAvgiftsregelType.fraKode(inntektspostDto.getSkatteOgAvgiftsregelType().getKode())
            : null;
        utbetaling.setSkattAvgiftType(skatteOgAvgiftsregelType);
        utbetaling.setInntektYtelseType(inntektspostDto.getInntektYtelseType());
        return utbetaling;
    }

    private static Periode mapPeriodeNullsafe(Intervall periode) {
        return periode == null ? null : mapPeriode(periode);
    }

    private static Periode mapPeriode(Intervall periode) {
        return new Periode(periode.getFomDato(), periode.getTomDato());
    }

    private static ArbeidsforholdInformasjonDto mapArbeidsforholdInformasjon(Optional<no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdInformasjonDto> arbeidsforholdInformasjon) {
        var arbeidsforholdInformasjonDtoMapped = arbeidsforholdInformasjon
            .map(arbeidsforholdInformasjonDto -> new ArbeidsforholdInformasjonDto(mapOverstyringer(arbeidsforholdInformasjonDto.getOverstyringer()))).orElse(null);
        if (arbeidsforholdInformasjonDtoMapped == null || arbeidsforholdInformasjonDtoMapped.getOverstyringer() == null || arbeidsforholdInformasjonDtoMapped.getOverstyringer().isEmpty()) {
            return null;
        }
        return arbeidsforholdInformasjonDtoMapped;
    }

    private static List<ArbeidsforholdOverstyringDto> mapOverstyringer(List<no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdOverstyringDto> overstyringer) {
        return overstyringer == null ? null : overstyringer.stream().map(MapTilKalkulatorInput::mapOverstyring).toList();
    }

    private static ArbeidsforholdOverstyringDto mapOverstyring(no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdOverstyringDto arbeidsforholdOverstyringDto) {
        if (arbeidsforholdOverstyringDto == null) {
            return null;
        }
        var arbeidsgiver = mapArbeidsgiver(arbeidsforholdOverstyringDto.getArbeidsgiver());
        var arbeidsforholdRefDto = mapAbakusReferanse(arbeidsforholdOverstyringDto.getArbeidsforholdRef());
        var handling = arbeidsforholdOverstyringDto.getHandling() == null ? null : ArbeidsforholdHandlingType.fraKode(
            arbeidsforholdOverstyringDto.getHandling().getKode());
        return new ArbeidsforholdOverstyringDto(arbeidsgiver, arbeidsforholdRefDto, handling);
    }

    private static ArbeidDto mapArbeidDto(Optional<AktørArbeidDto> aktørArbeidFraRegister) {
        return aktørArbeidFraRegister.map(aktørArbeidDto -> new ArbeidDto(aktørArbeidDto.hentAlleYrkesaktiviteter()
            .stream().map(MapTilKalkulatorInput::mapYrkesaktivitet).toList()))
            .orElse(null);
    }

    private static no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto mapYrkesaktivitet(YrkesaktivitetDto yrkesaktivitetDto) {
        if (yrkesaktivitetDto == null) {
            return null;
        }
        var arbeidsgiver = mapArbeidsgiverNullsafe(yrkesaktivitetDto.getArbeidsgiver());
        var abakusReferanse = mapAbakusReferanse(yrkesaktivitetDto.getArbeidsforholdRef());
        var arbeidType = ArbeidType.fraKode(yrkesaktivitetDto.getArbeidType().getKode());
        var aktivitetsAvtaler = mapAktivitetsAvtaler(yrkesaktivitetDto.getAlleAktivitetsAvtaler());
        var permisjoner = mapPermisjoner(yrkesaktivitetDto.getPermisjoner());
        return new no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto(arbeidsgiver, abakusReferanse, arbeidType, aktivitetsAvtaler,
            permisjoner);
    }

    private static List<PermisjonDto> mapPermisjoner(Set<no.nav.folketrygdloven.kalkulator.modell.iay.permisjon.PermisjonDto> permisjoner) {
        return permisjoner.stream()
            .map(MapTilKalkulatorInput::mapPermisjon).toList();
    }

    private static PermisjonDto mapPermisjon(no.nav.folketrygdloven.kalkulator.modell.iay.permisjon.PermisjonDto perm) {
        var periode = mapPeriode(perm.getPeriode());
        return new PermisjonDto(periode, mapTilIAYProsent(perm.getProsentsats()), perm.getPermisjonsbeskrivelseType());
    }

    private static List<AktivitetsAvtaleDto> mapAktivitetsAvtaler(Collection<no.nav.folketrygdloven.kalkulator.modell.iay.AktivitetsAvtaleDto> alleAktivitetsAvtaler) {
        return alleAktivitetsAvtaler == null ? null : alleAktivitetsAvtaler.stream().map(MapTilKalkulatorInput::mapAktivitetsAvtale).toList();
    }

    private static AktivitetsAvtaleDto mapAktivitetsAvtale(no.nav.folketrygdloven.kalkulator.modell.iay.AktivitetsAvtaleDto aktivitetsAvtaleDto) {
        if (aktivitetsAvtaleDto == null) {
            return null;
        }
        var periode = mapPeriode(aktivitetsAvtaleDto.getPeriode());
        var stillingsprosent = aktivitetsAvtaleDto.erAnsettelsesPeriode() ? null : BigDecimal.valueOf(100);
        return new AktivitetsAvtaleDto(periode, aktivitetsAvtaleDto.getSisteLønnsendringsdato(), mapTilIAYProsent(stillingsprosent));
    }

    private static no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto mapAbakusReferanse(InternArbeidsforholdRefDto arbeidsforholdRef) {
        if (arbeidsforholdRef == null || arbeidsforholdRef.getReferanse() == null) {
            return null;
        }
        return new no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto(arbeidsforholdRef.getReferanse());
    }

    private static Aktør mapArbeidsgiverNullsafe(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver == null ? null : mapArbeidsgiver(arbeidsgiver);
    }

    private static Aktør mapArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getIdentifikator()) : new AktørIdPersonident(
            arbeidsgiver.getIdentifikator());
    }

    private static no.nav.folketrygdloven.kalkulus.felles.v1.Beløp mapTilBeløp(Beløp beløp) {
        return beløp == null ? null : no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(beløp.verdi());
    }

    private static IayProsent mapTilIAYProsent(Stillingsprosent prosent) {
        return prosent == null ? null : IayProsent.fra(prosent.verdi());
    }

    private static IayProsent mapTilIAYProsent(BigDecimal prosent) {
        return IayProsent.fra(prosent);
    }
}
