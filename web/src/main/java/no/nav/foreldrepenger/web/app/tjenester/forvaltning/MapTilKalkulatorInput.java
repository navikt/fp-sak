package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.SvangerskapspengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktørArbeidDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktørInntektDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktørYtelseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektspostDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.NaturalYtelseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.RefusjonDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Beløp;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Stillingsprosent;
import no.nav.folketrygdloven.kalkulator.modell.virksomhet.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetGraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AndelGraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.GraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.RefusjonskravDatoDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.BeløpDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdInformasjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdOverstyringDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntekterDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingerDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingsPostDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelserDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.RelatertYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.TemaUnderkategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittOpptjeningDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningPeriodeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;

/**
 * Mapper beregningsgrunnlagInput til KalkulatorInput for bruk til feilsøking av saker
 */
class MapTilKalkulatorInput {

    public static KalkulatorInputDto map(BeregningsgrunnlagInput beregningsgrunnlagInput, AktørId aktørId) {
        if (beregningsgrunnlagInput == null) {
            return null;
        }
        KalkulatorInputDto kalkulatorInputDto = new KalkulatorInputDto(
            mapIayGrunnlag(beregningsgrunnlagInput, aktørId),
            mapOpptjeningAktiviteter(beregningsgrunnlagInput.getOpptjeningAktiviteterForBeregning()),
            beregningsgrunnlagInput.getSkjæringstidspunktOpptjening()
        );
        kalkulatorInputDto.medYtelsespesifiktGrunnlag(mapYtelsesSpesifiktGrunnlag(beregningsgrunnlagInput.getYtelsespesifiktGrunnlag()));
        kalkulatorInputDto.medRefusjonskravDatoer(mapRefusjonskravDatoer(beregningsgrunnlagInput.getRefusjonskravDatoer()));
        kalkulatorInputDto.medAktivitetGradering(mapAktivitetGradering(beregningsgrunnlagInput.getAktivitetGradering()));
        return kalkulatorInputDto;
    }

    private static AktivitetGraderingDto mapAktivitetGradering(AktivitetGradering aktivitetGradering) {
        if (aktivitetGradering == null) {
            return null;
        }
        AktivitetGraderingDto aktivitetGraderingDto = new AktivitetGraderingDto(mapAndelGraderinger(aktivitetGradering.getAndelGradering()));
        return aktivitetGraderingDto.getAndelGraderingDto() == null || aktivitetGraderingDto.getAndelGraderingDto().isEmpty() ? null : aktivitetGraderingDto;
    }

    private static List<AndelGraderingDto> mapAndelGraderinger(Set<AndelGradering> andelGradering) {
        return andelGradering == null ? null : andelGradering.stream().map(MapTilKalkulatorInput::mapAndelGradering).collect(Collectors.toList());
    }

    private static AndelGraderingDto mapAndelGradering(AndelGradering andelGradering) {
        return andelGradering == null ? null : new AndelGraderingDto(
            andelGradering.getAktivitetStatus() == null ? null : new AktivitetStatus(andelGradering.getAktivitetStatus().getKode()),
            mapArbeidsgiver(andelGradering.getArbeidsgiver()),
            mapAbakusReferanse(andelGradering.getArbeidsforholdRef()),
            mapGraderinger(andelGradering.getGraderinger())
        );
    }

    private static List<GraderingDto> mapGraderinger(List<AndelGradering.Gradering> graderinger) {
        return graderinger == null ? null : graderinger.stream().map(MapTilKalkulatorInput::mapGradering).collect(Collectors.toList());
    }

    private static GraderingDto mapGradering(AndelGradering.Gradering gradering) {
        return gradering == null ? null : new GraderingDto(mapPeriode(gradering.getPeriode()), gradering.getArbeidstidProsent());
    }

    private static List<RefusjonskravDatoDto> mapRefusjonskravDatoer(List<no.nav.folketrygdloven.kalkulator.modell.iay.RefusjonskravDatoDto> refusjonskravDatoer) {
        return refusjonskravDatoer == null ? null : refusjonskravDatoer.stream().map(MapTilKalkulatorInput::mapRefusjonskravDato).collect(Collectors.toList());
    }

    private static RefusjonskravDatoDto mapRefusjonskravDato(no.nav.folketrygdloven.kalkulator.modell.iay.RefusjonskravDatoDto refusjonskravDatoDto) {
        return refusjonskravDatoDto == null ? null : new RefusjonskravDatoDto(
            mapArbeidsgiver(refusjonskravDatoDto.getArbeidsgiver()),
            refusjonskravDatoDto.getFørsteDagMedRefusjonskrav().orElse(null),
            refusjonskravDatoDto.getFørsteInnsendingAvRefusjonskrav(),
            refusjonskravDatoDto.harRefusjonFraStart()
        );
    }

    private static YtelsespesifiktGrunnlagDto mapYtelsesSpesifiktGrunnlag(YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag) {
        if (ytelsespesifiktGrunnlag == null) {
            return null;
        } else if (ytelsespesifiktGrunnlag instanceof SvangerskapspengerGrunnlag) {
            SvangerskapspengerGrunnlag svpGrunnlag = (SvangerskapspengerGrunnlag) ytelsespesifiktGrunnlag;
            return new no.nav.folketrygdloven.kalkulus.beregning.v1.SvangerskapspengerGrunnlag(mapUtbetalingsgradPrAktivitet(svpGrunnlag.getUtbetalingsgradPrAktivitet()));
        } else if (ytelsespesifiktGrunnlag instanceof no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.ForeldrepengerGrunnlag) {
            no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.ForeldrepengerGrunnlag fpGrunnlag = (no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.ForeldrepengerGrunnlag) ytelsespesifiktGrunnlag;
            return new ForeldrepengerGrunnlag(BigDecimal.valueOf(fpGrunnlag.getDekningsgrad()), fpGrunnlag.isKvalifisererTilBesteberegning());
        }
        return null;
    }

    private static List<UtbetalingsgradPrAktivitetDto> mapUtbetalingsgradPrAktivitet(List<no.nav.folketrygdloven.kalkulator.modell.svp.UtbetalingsgradPrAktivitetDto> utbetalingsgradPrAktivitet) {
        return utbetalingsgradPrAktivitet == null ? null : utbetalingsgradPrAktivitet.stream().map(MapTilKalkulatorInput::mapUtbetalingsgradForAktivitet).collect(Collectors.toList());
    }

    private static UtbetalingsgradPrAktivitetDto mapUtbetalingsgradForAktivitet(no.nav.folketrygdloven.kalkulator.modell.svp.UtbetalingsgradPrAktivitetDto utbetalingsgradPrAktivitetDto) {
        return utbetalingsgradPrAktivitetDto == null ? null : new UtbetalingsgradPrAktivitetDto(
            mapArbeidsforholdDto(utbetalingsgradPrAktivitetDto.getUtbetalingsgradArbeidsforhold()),
            mapPerioderMedUtbetalingsgrad(utbetalingsgradPrAktivitetDto.getPeriodeMedUtbetalingsgrad())
        );
    }

    private static List<PeriodeMedUtbetalingsgradDto> mapPerioderMedUtbetalingsgrad(List<no.nav.folketrygdloven.kalkulator.modell.svp.PeriodeMedUtbetalingsgradDto> periodeMedUtbetalingsgrad) {
        return periodeMedUtbetalingsgrad == null ? null : periodeMedUtbetalingsgrad.stream().map(MapTilKalkulatorInput::mapPeriodeMedUtbetalingsgrad).collect(Collectors.toList());
    }

    private static PeriodeMedUtbetalingsgradDto mapPeriodeMedUtbetalingsgrad(no.nav.folketrygdloven.kalkulator.modell.svp.PeriodeMedUtbetalingsgradDto periodeMedUtbetalingsgradDto) {
        return periodeMedUtbetalingsgradDto == null ? null :
            new PeriodeMedUtbetalingsgradDto(
                mapPeriode(periodeMedUtbetalingsgradDto.getPeriode()),
                periodeMedUtbetalingsgradDto.getUtbetalingsgrad()
            );
    }

    private static UtbetalingsgradArbeidsforholdDto mapArbeidsforholdDto(no.nav.folketrygdloven.kalkulator.modell.svp.UtbetalingsgradArbeidsforholdDto utbetalingsgradArbeidsforhold) {
        return utbetalingsgradArbeidsforhold == null ? null :
            new UtbetalingsgradArbeidsforholdDto(
                utbetalingsgradArbeidsforhold.getArbeidsgiver().map(MapTilKalkulatorInput::mapArbeidsgiver).orElse(null),
                mapAbakusReferanse(utbetalingsgradArbeidsforhold.getInternArbeidsforholdRef()),
                utbetalingsgradArbeidsforhold.getUttakArbeidType() == null ? null : new UttakArbeidType(utbetalingsgradArbeidsforhold.getUttakArbeidType().getKode())
            );
    }

    private static OpptjeningAktiviteterDto mapOpptjeningAktiviteter(Collection<no.nav.folketrygdloven.kalkulator.opptjening.OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningAktiviteterForBeregning) {
        return opptjeningAktiviteterForBeregning == null ? null : new OpptjeningAktiviteterDto(mapOpptjeningPerioder(opptjeningAktiviteterForBeregning));
    }

    private static List<OpptjeningPeriodeDto> mapOpptjeningPerioder(Collection<no.nav.folketrygdloven.kalkulator.opptjening.OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningAktiviteterForBeregning) {
        return opptjeningAktiviteterForBeregning.stream().map(MapTilKalkulatorInput::mapOpptjeningPeriode).collect(Collectors.toList());
    }

    private static OpptjeningPeriodeDto mapOpptjeningPeriode(no.nav.folketrygdloven.kalkulator.opptjening.OpptjeningAktiviteterDto.OpptjeningPeriodeDto opptjeningPeriodeDto) {
        return new OpptjeningPeriodeDto(
            new OpptjeningAktivitetType(opptjeningPeriodeDto.getOpptjeningAktivitetType().getKode()),
            new Periode(opptjeningPeriodeDto.getPeriode().getFom(), opptjeningPeriodeDto.getPeriode().getTom()),
            mapArbeidsgiver(opptjeningPeriodeDto.getArbeidsgiverOrgNummer(), opptjeningPeriodeDto.getArbeidsgiverAktørId()),
            mapAbakusReferanse(opptjeningPeriodeDto.getArbeidsforholdId())
        );
    }

    private static Aktør mapArbeidsgiver(String arbeidsgiverOrgNummer, String arbeidsgiverAktørId) {
        if (arbeidsgiverOrgNummer == null && arbeidsgiverAktørId == null) {
            return null;
        }
        return arbeidsgiverOrgNummer == null ? new AktørIdPersonident(arbeidsgiverAktørId) : new Organisasjon(arbeidsgiverOrgNummer);
    }

    private static InntektArbeidYtelseGrunnlagDto mapIayGrunnlag(BeregningsgrunnlagInput beregningsgrunnlagInput, AktørId aktørId) {
        if (beregningsgrunnlagInput.getIayGrunnlag() == null) {
            return null;
        }
        no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto iayGrunnlag = beregningsgrunnlagInput.getIayGrunnlag();
        InntektArbeidYtelseGrunnlagDto inntektArbeidYtelseGrunnlagDto = new InntektArbeidYtelseGrunnlagDto();
        no.nav.folketrygdloven.kalkulator.modell.typer.AktørId mappedAktørId = new no.nav.folketrygdloven.kalkulator.modell.typer.AktørId(aktørId.getId());
        inntektArbeidYtelseGrunnlagDto.medArbeidDto(mapArbeidDto(iayGrunnlag.getAktørArbeidFraRegister(mappedAktørId)));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapArbeidsforholdInformasjon(iayGrunnlag.getArbeidsforholdInformasjon()));
        inntektArbeidYtelseGrunnlagDto.medInntekterDto(mapInntekter(iayGrunnlag.getAktørInntektFraRegister(mappedAktørId)));
        inntektArbeidYtelseGrunnlagDto.medInntektsmeldingerDto(mapInntektsmeldingerDto(iayGrunnlag.getInntektsmeldinger()));
        inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapOppgittOpptjening(iayGrunnlag.getOppgittOpptjening()));
        inntektArbeidYtelseGrunnlagDto.medYtelserDto(mapYtelserDto(iayGrunnlag.getAktørYtelseFraRegister(mappedAktørId)));
        return inntektArbeidYtelseGrunnlagDto;
    }

    private static YtelserDto mapYtelserDto(Optional<AktørYtelseDto> aktørYtelseFraRegister) {
        return aktørYtelseFraRegister.map(aktørYtelseDto -> new YtelserDto(mapYtelser(aktørYtelseDto.getAlleYtelser()))).orElse(null);
    }

    private static List<YtelseDto> mapYtelser(Collection<no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDto> alleYtelser) {
        return alleYtelser == null ? null : alleYtelser.stream().map(MapTilKalkulatorInput::mapYtelse).collect(Collectors.toList());
    }

    private static YtelseDto mapYtelse(no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDto ytelseDto) {
        return ytelseDto == null ? null : new YtelseDto(
            ytelseDto.getVedtaksDagsats().map(Beløp::getVerdi).map(BeløpDto::new).orElse(null),
            mapYtelseAnvistSet(ytelseDto.getYtelseAnvist()),
            ytelseDto.getRelatertYtelseType() == null ? null : new RelatertYtelseType(ytelseDto.getRelatertYtelseType().getKode()),
            mapPeriode(ytelseDto.getPeriode()),
            ytelseDto.getBehandlingsTema() == null ||
                ytelseDto.getBehandlingsTema().equals(no.nav.folketrygdloven.kalkulator.modell.ytelse.TemaUnderkategori.UDEFINERT) ?
                null : new TemaUnderkategori(ytelseDto.getBehandlingsTema().getKode())
        );
    }

    private static Set<no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto> mapYtelseAnvistSet(Collection<YtelseAnvistDto> ytelseAnvist) {
        return ytelseAnvist == null ? null : ytelseAnvist.stream().map(MapTilKalkulatorInput::mapYtelseAnvist).collect(Collectors.toSet());
    }

    private static no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto mapYtelseAnvist(YtelseAnvistDto ytelseAnvistDto) {
        return ytelseAnvistDto == null ? null : new no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto(
            new Periode(ytelseAnvistDto.getAnvistFOM(), ytelseAnvistDto.getAnvistTOM()),
            ytelseAnvistDto.getBeløp().map(Beløp::getVerdi).map(BeløpDto::new).orElse(null),
            ytelseAnvistDto.getDagsats().map(Beløp::getVerdi).map(BeløpDto::new).orElse(null),
            ytelseAnvistDto.getUtbetalingsgradProsent().map(Stillingsprosent::getVerdi).orElse(null)
        );
    }

    private static OppgittOpptjeningDto mapOppgittOpptjening(Optional<no.nav.folketrygdloven.kalkulator.modell.iay.OppgittOpptjeningDto> oppgittOpptjening) {
        return oppgittOpptjening.map(oppgittOpptjeningDto -> new OppgittOpptjeningDto(
            mapFrilans(oppgittOpptjeningDto.getFrilans()),
            mapNæringer(oppgittOpptjeningDto.getEgenNæring()),
            null
        )).orElse(null);
    }

    private static List<OppgittEgenNæringDto> mapNæringer(List<no.nav.folketrygdloven.kalkulator.modell.iay.OppgittEgenNæringDto> egenNæring) {
        return egenNæring == null ? null : egenNæring.stream().map(MapTilKalkulatorInput::mapNæring).collect(Collectors.toList());
    }

    private static OppgittEgenNæringDto mapNæring(no.nav.folketrygdloven.kalkulator.modell.iay.OppgittEgenNæringDto oppgittEgenNæringDto) {
        return oppgittEgenNæringDto == null ? null : new OppgittEgenNæringDto(
            mapPeriode(oppgittEgenNæringDto.getPeriode()),
            oppgittEgenNæringDto.getOrgnr() == null ? null : new Organisasjon(oppgittEgenNæringDto.getOrgnr()),
            oppgittEgenNæringDto.getVirksomhetType() == null ? null : new VirksomhetType(oppgittEgenNæringDto.getVirksomhetType().getKode()),
            oppgittEgenNæringDto.getNyoppstartet(),
            oppgittEgenNæringDto.getVarigEndring(),
            oppgittEgenNæringDto.getEndringDato(),
            oppgittEgenNæringDto.getNærRelasjon(),
            oppgittEgenNæringDto.getNyIArbeidslivet(),
            oppgittEgenNæringDto.getBruttoInntekt()
        );
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
        return alleInntektsmeldinger == null ? null : alleInntektsmeldinger.stream().map(MapTilKalkulatorInput::mapInntektsmelding).collect(Collectors.toList());
    }

    private static InntektsmeldingDto mapInntektsmelding(no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDto inntektsmeldingDto) {
        return inntektsmeldingDto == null ? null : new InntektsmeldingDto(
            mapArbeidsgiver(inntektsmeldingDto.getArbeidsgiver()),
            mapBeløp(inntektsmeldingDto.getInntektBeløp()),
            mapNaturalYtelser(inntektsmeldingDto.getNaturalYtelser()),
            mapRefusjonEndringer(inntektsmeldingDto.getEndringerRefusjon()),
            mapAbakusReferanse(inntektsmeldingDto.getArbeidsforholdRef()),
            inntektsmeldingDto.getStartDatoPermisjon().orElse(null),
            inntektsmeldingDto.getRefusjonOpphører(),
            mapBeløp(inntektsmeldingDto.getRefusjonBeløpPerMnd())
        );
    }

    private static BeløpDto mapBeløp(Beløp beløp) {
        return beløp == null ? null : new BeløpDto(beløp.getVerdi());
    }

    private static List<no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto> mapRefusjonEndringer(List<RefusjonDto> endringerRefusjon) {
        return endringerRefusjon == null ? null : endringerRefusjon.stream().map(MapTilKalkulatorInput::mapRefusjonEndring).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto mapRefusjonEndring(RefusjonDto refusjonDto) {
        return refusjonDto == null ? null : new no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto(
            mapBeløp(refusjonDto.getRefusjonsbeløp()),
            refusjonDto.getFom()
        );
    }

    private static List<no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto> mapNaturalYtelser(List<NaturalYtelseDto> naturalYtelser) {
        return naturalYtelser == null ? null : naturalYtelser.stream().map(MapTilKalkulatorInput::mapNaturalYtelse).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto mapNaturalYtelse(NaturalYtelseDto naturalYtelseDto) {
        return naturalYtelseDto == null ? null : new no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto(
            mapPeriode(naturalYtelseDto.getPeriode()),
            mapBeløp(naturalYtelseDto.getBeloepPerMnd()),
            naturalYtelseDto.getType() == null ? null : new NaturalYtelseType(naturalYtelseDto.getType().getKode())
        );
    }

    private static InntekterDto mapInntekter(Optional<AktørInntektDto> aktørInntektFraRegister) {
        return aktørInntektFraRegister.map(aktørInntektDto -> new InntekterDto(mapUtbetalinger(aktørInntektDto.getInntekt()))).orElse(null);
    }

    private static List<UtbetalingDto> mapUtbetalinger(Collection<InntektDto> inntekt) {
        return inntekt == null ? null : inntekt.stream().map(MapTilKalkulatorInput::mapUtbetaling).collect(Collectors.toList());
    }

    private static UtbetalingDto mapUtbetaling(InntektDto inntektDto) {
        if (inntektDto == null) {
            return null;
        }
        UtbetalingDto utbetalingDto = new UtbetalingDto(
            inntektDto.getInntektsKilde() == null ? null : new InntektskildeType(inntektDto.getInntektsKilde().getKode()),
            mapPoster(inntektDto.getAlleInntektsposter())
        );
        utbetalingDto.setArbeidsgiver(mapArbeidsgiver(inntektDto.getArbeidsgiver()));
        return utbetalingDto;
    }

    private static List<UtbetalingsPostDto> mapPoster(Collection<InntektspostDto> alleInntektsposter) {
        return alleInntektsposter == null ? null : alleInntektsposter.stream().map(MapTilKalkulatorInput::mapPost).collect(Collectors.toList());
    }

    private static UtbetalingsPostDto mapPost(InntektspostDto inntektspostDto) {
        return inntektspostDto == null ? null : new UtbetalingsPostDto(
            mapPeriode(inntektspostDto.getPeriode()),
            inntektspostDto.getInntektspostType() == null ?
                new InntektspostType(no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType.UDEFINERT.getKode()) :
                new InntektspostType(inntektspostDto.getInntektspostType().getKode()),
            inntektspostDto.getBeløp() == null ? null : inntektspostDto.getBeløp().getVerdi()
        );
    }

    private static Periode mapPeriode(Intervall periode) {
        return periode == null ? null : new Periode(periode.getFomDato(), periode.getTomDato());
    }

    private static ArbeidsforholdInformasjonDto mapArbeidsforholdInformasjon(Optional<no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdInformasjonDto> arbeidsforholdInformasjon) {
        ArbeidsforholdInformasjonDto arbeidsforholdInformasjonDtoMapped = arbeidsforholdInformasjon.map(arbeidsforholdInformasjonDto ->
            new ArbeidsforholdInformasjonDto(mapOverstyringer(arbeidsforholdInformasjonDto.getOverstyringer()))
        ).orElse(null);
        if (arbeidsforholdInformasjonDtoMapped == null || arbeidsforholdInformasjonDtoMapped.getOverstyringer() == null || arbeidsforholdInformasjonDtoMapped.getOverstyringer().isEmpty()) {
            return null;
        }
        return arbeidsforholdInformasjonDtoMapped;
    }

    private static List<ArbeidsforholdOverstyringDto> mapOverstyringer(List<no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdOverstyringDto> overstyringer) {
        return overstyringer == null ? null : overstyringer.stream().map(MapTilKalkulatorInput::mapOverstyring).collect(Collectors.toList());
    }

    private static ArbeidsforholdOverstyringDto mapOverstyring(no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdOverstyringDto arbeidsforholdOverstyringDto) {
        return arbeidsforholdOverstyringDto == null ? null : new ArbeidsforholdOverstyringDto(
            mapArbeidsgiver(arbeidsforholdOverstyringDto.getArbeidsgiver()),
            mapAbakusReferanse(arbeidsforholdOverstyringDto.getArbeidsforholdRef()),
            arbeidsforholdOverstyringDto.getHandling() == null ? null : new ArbeidsforholdHandlingType(arbeidsforholdOverstyringDto.getHandling().getKode())
        );
    }

    private static ArbeidDto mapArbeidDto(Optional<AktørArbeidDto> aktørArbeidFraRegister) {
        return aktørArbeidFraRegister.map(aktørArbeidDto -> new ArbeidDto(aktørArbeidDto.hentAlleYrkesaktiviteter()
            .stream().map(MapTilKalkulatorInput::mapYrkesaktivitet).collect(Collectors.toList())))
            .orElse(null);
    }

    private static no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto mapYrkesaktivitet(YrkesaktivitetDto yrkesaktivitetDto) {
        return yrkesaktivitetDto == null ? null : new no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto(
            mapArbeidsgiver(yrkesaktivitetDto.getArbeidsgiver()),
            mapAbakusReferanse(yrkesaktivitetDto.getArbeidsforholdRef()),
            yrkesaktivitetDto.getArbeidType() == null ? null : new no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType(yrkesaktivitetDto.getArbeidType().getKode()),
            mapAktivitetsAvtaler(yrkesaktivitetDto.getAlleAktivitetsAvtaler())
        );
    }

    private static List<AktivitetsAvtaleDto> mapAktivitetsAvtaler(Collection<no.nav.folketrygdloven.kalkulator.modell.iay.AktivitetsAvtaleDto> alleAktivitetsAvtaler) {
        return alleAktivitetsAvtaler == null ? null : alleAktivitetsAvtaler.stream().map(MapTilKalkulatorInput::mapAktivitetsAvtale).collect(Collectors.toList());
    }

    private static AktivitetsAvtaleDto mapAktivitetsAvtale(no.nav.folketrygdloven.kalkulator.modell.iay.AktivitetsAvtaleDto aktivitetsAvtaleDto) {
        return aktivitetsAvtaleDto == null ? null : new AktivitetsAvtaleDto(
            mapPeriode(aktivitetsAvtaleDto.getPeriode()),
            aktivitetsAvtaleDto.getSisteLønnsendringsdato(),
            aktivitetsAvtaleDto.erAnsettelsesPeriode() ? null : BigDecimal.valueOf(100)
        );
    }

    private static no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto mapAbakusReferanse(InternArbeidsforholdRefDto arbeidsforholdRef) {
        if (arbeidsforholdRef == null || arbeidsforholdRef.getReferanse() == null) {
            return null;
        }
        return new no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto(arbeidsforholdRef.getReferanse());
    }

    private static Aktør mapArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getIdentifikator()) : new AktørIdPersonident(arbeidsgiver.getIdentifikator());
    }

}
