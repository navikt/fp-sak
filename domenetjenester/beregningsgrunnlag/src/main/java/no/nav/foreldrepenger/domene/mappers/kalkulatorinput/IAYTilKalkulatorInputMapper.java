package no.nav.foreldrepenger.domene.mappers.kalkulatorinput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.BeløpDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.JournalpostId;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdInformasjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdOverstyringDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.PermisjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntekterDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingerDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingsPostDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseFordelingDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelserDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Arbeidskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektPeriodeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.PermisjonsbeskrivelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.RelatertYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.TemaUnderkategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansInntekt;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittOpptjeningDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningPeriodeDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class IAYTilKalkulatorInputMapper {

    private static String KODEVERDI_UNDEFINED = "-";

    public static List<Inntekt> finnRelevanteInntekter(InntektFilter inntektFilter) {
        return new ArrayList<>() {
            {
                addAll(inntektFilter.getAlleInntektSammenligningsgrunnlag());
                addAll(inntektFilter.getAlleInntektBeregningsgrunnlag());
                addAll(inntektFilter.getAlleInntektBeregnetSkatt());
            }
        };
    }

    public static ArbeidsforholdInformasjonDto mapTilArbeidsforholdInformasjonDto(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        List<ArbeidsforholdOverstyringDto> resultat = arbeidsforholdInformasjon.getOverstyringer().stream()
            .map(arbeidsforholdOverstyring -> new ArbeidsforholdOverstyringDto(mapTilAktør(arbeidsforholdOverstyring.getArbeidsgiver()),
                arbeidsforholdOverstyring.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold() ? new InternArbeidsforholdRefDto(arbeidsforholdOverstyring.getArbeidsforholdRef().getReferanse())
                    : null,
                ArbeidsforholdHandlingType.fraKode(arbeidsforholdOverstyring.getHandling().getKode())))
            .collect(Collectors.toList());

        if (!resultat.isEmpty()) {
            return new ArbeidsforholdInformasjonDto(resultat);
        }
        return null;
    }

    public static OppgittEgenNæringDto mapOppgittEgenNæring(OppgittEgenNæring oppgittEgenNæring) {
        return new OppgittEgenNæringDto(
            mapPeriode(oppgittEgenNæring.getPeriode()),
            oppgittEgenNæring.getOrgnr() == null ? null : new Organisasjon(oppgittEgenNæring.getOrgnr()),
            VirksomhetType.fraKode(oppgittEgenNæring.getVirksomhetType().getKode()),
            oppgittEgenNæring.getNyoppstartet(),
            oppgittEgenNæring.getVarigEndring(),
            oppgittEgenNæring.getEndringDato(),
            oppgittEgenNæring.getNyIArbeidslivet(),
            oppgittEgenNæring.getBegrunnelse(),
            oppgittEgenNæring.getBruttoInntekt());
    }

    public static OppgittArbeidsforholdDto mapArbeidsforhold(OppgittArbeidsforhold arb) {
        return new OppgittArbeidsforholdDto(mapPeriode(arb.getPeriode()), null);
    }

    public static Function<OppgittFrilansoppdrag, OppgittFrilansInntekt> mapFrilansOppdrag() {
        return frilansoppdrag -> new OppgittFrilansInntekt(mapPeriode(frilansoppdrag.getPeriode()), null);
    }

    private static InntektsmeldingerDto mapTilDto(Collection<Inntektsmelding> inntektsmeldinger) {

        List<InntektsmeldingDto> inntektsmeldingDtoer = inntektsmeldinger.stream().map(inntektsmelding -> {
            Aktør aktør = mapTilAktør(inntektsmelding.getArbeidsgiver());
            var beløpDto = new BeløpDto(inntektsmelding.getInntektBeløp().getVerdi());
            var naturalYtelseDtos = inntektsmelding.getNaturalYtelser().stream().map(naturalYtelse -> new NaturalYtelseDto(
                mapPeriode(naturalYtelse.getPeriode()),
                new BeløpDto(naturalYtelse.getBeloepPerMnd().getVerdi()),
                NaturalYtelseType.fraKode(naturalYtelse.getType().getKode()))).collect(Collectors.toList());

            var refusjonDtos = inntektsmelding.getEndringerRefusjon().stream().map(refusjon -> new RefusjonDto(
                new BeløpDto(refusjon.getRefusjonsbeløp().getVerdi()),
                refusjon.getFom())).collect(Collectors.toList());

            var internArbeidsforholdRefDto = inntektsmelding.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()
                ? new InternArbeidsforholdRefDto(inntektsmelding.getArbeidsforholdRef().getReferanse())
                : null;
            var startDato = inntektsmelding.getStartDatoPermisjon().isPresent() ? inntektsmelding.getStartDatoPermisjon().get() : null;
            var refusjon = inntektsmelding.getRefusjonOpphører();
            var beløpDto1 = inntektsmelding.getRefusjonBeløpPerMnd() != null ? new BeløpDto(inntektsmelding.getRefusjonBeløpPerMnd().getVerdi()) : null;

            var journalpostId = new JournalpostId(inntektsmelding.getJournalpostId().getVerdi());
            return new InntektsmeldingDto(aktør, beløpDto,
                naturalYtelseDtos,
                refusjonDtos,
                internArbeidsforholdRefDto,
                startDato,
                refusjon,
                beløpDto1,
                journalpostId,
                inntektsmelding.getKanalreferanse());
        }).collect(Collectors.toList());

        return inntektsmeldingDtoer.isEmpty() ? null : new InntektsmeldingerDto(inntektsmeldingDtoer);
    }

    private static Periode mapPeriode(DatoIntervallEntitet periode) {
        return new Periode(periode.getFomDato(), periode.getTomDato());
    }

    public static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getOrgnr()) : new AktørIdPersonident(arbeidsgiver.getAktørId().getId());
    }

    public static YtelserDto mapYtelseDto(List<Ytelse> alleYtelser) {
        List<YtelseDto> ytelserDto = alleYtelser.stream().map(ytelse -> new YtelseDto(
                mapBeløp(ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getVedtaksDagsats)),
                mapYtelseAnvist(ytelse.getYtelseAnvist()),
                new RelatertYtelseType(ytelse.getRelatertYtelseType().getKode()),
                mapPeriode(ytelse.getPeriode()),
                mapTemaUnderkategori(ytelse),
                mapYtelseGrunnlag(ytelse.getYtelseGrunnlag())))
            .collect(Collectors.toList());

        if (!ytelserDto.isEmpty()) {
            return new YtelserDto(ytelserDto);
        }
        return null;
    }

    private static YtelseGrunnlagDto mapYtelseGrunnlag(Optional<YtelseGrunnlag> ytelseGrunnlag) {
        return ytelseGrunnlag.map(yg -> new YtelseGrunnlagDto(
            Arbeidskategori.fraKode(yg.getArbeidskategori().map(no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori::getKode).orElse(null)), mapYtelseFordeling(yg.getYtelseStørrelse()))).orElse(null);
    }

    private static List<YtelseFordelingDto> mapYtelseFordeling(List<YtelseStørrelse> ytelseStørrelse) {
        return ytelseStørrelse.stream()
            .map(ys -> new YtelseFordelingDto(mapVirksomhet(ys), InntektPeriodeType.fraKode(ys.getHyppighet().getKode()), ys.getBeløp().getVerdi(), ys.getErRefusjon()))
            .collect(Collectors.toList());
    }

    private static Organisasjon mapVirksomhet(YtelseStørrelse ys) {
        return ys.getVirksomhet().map(orgNummer -> new Organisasjon(orgNummer.getId())).orElse(null);
    }

    // TODO (OJR): Skal vi mappe dette slik eller tåler Kalkulus UNDEFINED("-")
    private static TemaUnderkategori mapTemaUnderkategori(Ytelse ytelse) {
        if (KODEVERDI_UNDEFINED.equals(ytelse.getBehandlingsTema().getKode())) {
            return null;
        }
        return TemaUnderkategori.fraKode(ytelse.getBehandlingsTema().getKode());
    }

    private static BeløpDto mapBeløp(Optional<Beløp> beløp) {
        return beløp.map(value -> new BeløpDto(value.getVerdi())).orElse(null);
    }

    private static Set<YtelseAnvistDto> mapYtelseAnvist(Collection<YtelseAnvist> ytelseAnvist) {
        return ytelseAnvist.stream().map(ya -> {
            BeløpDto beløpDto = mapBeløp(ya.getBeløp());
            BeløpDto dagsatsDto = mapBeløp(ya.getDagsats());
            BigDecimal bigDecimal = ya.getUtbetalingsgradProsent().isPresent() ? ya.getUtbetalingsgradProsent().get().getVerdi() : null;
            return new YtelseAnvistDto(new Periode(
                ya.getAnvistFOM(), ya.getAnvistTOM()),
                beløpDto,
                dagsatsDto,
                bigDecimal);
        }).collect(Collectors.toSet());
    }

    public static InntekterDto mapInntektDto(List<Inntekt> alleInntektBeregningsgrunnlag) {
        List<UtbetalingDto> utbetalingDtoer = alleInntektBeregningsgrunnlag.stream().map(IAYTilKalkulatorInputMapper::mapTilDto).collect(Collectors.toList());
        if (!utbetalingDtoer.isEmpty()) {
            return new InntekterDto(utbetalingDtoer);
        }
        return null;
    }

    private static UtbetalingDto mapTilDto(Inntekt inntekt) {
        UtbetalingDto utbetalingDto = new UtbetalingDto(InntektskildeType.fraKode(inntekt.getInntektsKilde().getKode()),
            inntekt.getAlleInntektsposter().stream().map(IAYTilKalkulatorInputMapper::mapTilDto).collect(Collectors.toList()));
        if (inntekt.getArbeidsgiver() != null) {
            return utbetalingDto.medArbeidsgiver(mapTilAktør(inntekt.getArbeidsgiver()));
        }
        return utbetalingDto;
    }

    private static UtbetalingsPostDto mapTilDto(Inntektspost inntektspost) {
        return new UtbetalingsPostDto(
            mapPeriode(inntektspost.getPeriode()),
            InntektspostType.fraKode(inntektspost.getInntektspostType().getKode()),
            inntektspost.getBeløp().getVerdi());
    }

    public static ArbeidDto mapArbeidDto(Collection<Yrkesaktivitet> yrkesaktiviteterForBeregning) {
        List<YrkesaktivitetDto> yrkesaktivitetDtoer = yrkesaktiviteterForBeregning.stream().map(IAYTilKalkulatorInputMapper::mapTilDto).collect(Collectors.toList());
        if (!yrkesaktivitetDtoer.isEmpty()) {
            return new ArbeidDto(yrkesaktivitetDtoer);
        }
        return null;
    }

    private static YrkesaktivitetDto mapTilDto(Yrkesaktivitet yrkesaktivitet) {
        List<AktivitetsAvtaleDto> aktivitetsAvtaleDtos = yrkesaktivitet.getAlleAktivitetsAvtaler().stream().map(aktivitetsAvtale -> new AktivitetsAvtaleDto(mapPeriode(aktivitetsAvtale.getPeriode()),
            aktivitetsAvtale.getSisteLønnsendringsdato(),
            aktivitetsAvtale.getProsentsats() != null ? aktivitetsAvtale.getProsentsats().getVerdi() : null)
        ).collect(Collectors.toList());

        String arbeidsforholdRef = yrkesaktivitet.getArbeidsforholdRef().getReferanse();
        List<PermisjonDto> permisjoner = yrkesaktivitet.getPermisjon().stream()
            .map(IAYTilKalkulatorInputMapper::mapTilPermisjonDto)
            .collect(Collectors.toList());
        return new YrkesaktivitetDto(
            mapTilAktør(yrkesaktivitet.getArbeidsgiver()),
            arbeidsforholdRef != null ? new InternArbeidsforholdRefDto(arbeidsforholdRef) : null,
            ArbeidType.fraKode(yrkesaktivitet.getArbeidType().getKode()),
            aktivitetsAvtaleDtos,
            permisjoner);
    }

    public static OpptjeningAktiviteterDto mapTilDto(OpptjeningAktiviteter opptjeningAktiviteter) {
        return new OpptjeningAktiviteterDto(opptjeningAktiviteter.getOpptjeningPerioder()
            .stream()
            .map(opptjeningPeriode -> new OpptjeningPeriodeDto(
                OpptjeningAktivitetType.fraKode(opptjeningPeriode.opptjeningAktivitetType().getKode()),
                new Periode(opptjeningPeriode.periode().getFom(), opptjeningPeriode.periode().getTom()),
                mapTilDto(opptjeningPeriode),
                opptjeningPeriode.arbeidsforholdId() != null && opptjeningPeriode.arbeidsforholdId().getReferanse() != null
                    ? new InternArbeidsforholdRefDto(opptjeningPeriode.arbeidsforholdId().getReferanse())
                    : null))
            .collect(Collectors.toList())
            , null);
    }


    private static PermisjonDto mapTilPermisjonDto(Permisjon permisjon) {
        return new PermisjonDto(
            new Periode(permisjon.getFraOgMed(), permisjon.getTilOgMed()),
            permisjon.getProsentsats().getVerdi(),
            PermisjonsbeskrivelseType.fraKode(permisjon.getPermisjonsbeskrivelseType().getKode())
        );
    }

    private static Aktør mapTilDto(OpptjeningAktiviteter.OpptjeningPeriode periode) {
        var orgNummer = periode.arbeidsgiverOrgNummer() != null ? new Organisasjon(periode.arbeidsgiverOrgNummer()) : null;
        if (orgNummer != null) {
            return orgNummer;
        }
        return periode.arbeidsgiverAktørId() != null ? new AktørIdPersonident(periode.arbeidsgiverAktørId()) : null;
    }

    public static InntektArbeidYtelseGrunnlagDto mapTilDto(InntektArbeidYtelseGrunnlag grunnlag,
                                                           AktørId aktørId, LocalDate skjæringstidspunktOpptjening) {

        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunktOpptjening);
        var ytelseFilter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));

        var yrkesaktiviteterForBeregning = new ArrayList<>(yrkesaktivitetFilter.getYrkesaktiviteter());
        yrkesaktiviteterForBeregning.addAll(yrkesaktivitetFilter.getFrilansOppdrag());
        var alleRelevanteInntekter = finnRelevanteInntekter(inntektFilter);
        var inntektArbeidYtelseGrunnlagDto = new InntektArbeidYtelseGrunnlagDto();

        inntektArbeidYtelseGrunnlagDto.medArbeidDto(mapArbeidDto(yrkesaktiviteterForBeregning));
        inntektArbeidYtelseGrunnlagDto.medInntekterDto(mapInntektDto(alleRelevanteInntekter));
        inntektArbeidYtelseGrunnlagDto.medYtelserDto(mapYtelseDto(ytelseFilter.getAlleYtelser()));
        var inntektsmeldinger = grunnlag.getInntektsmeldinger().stream()
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .flatMap(Collection::stream).toList();
        inntektArbeidYtelseGrunnlagDto.medInntektsmeldingerDto(mapTilDto(inntektsmeldinger));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon().map(IAYTilKalkulatorInputMapper::mapTilArbeidsforholdInformasjonDto).orElse(null));
        inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapTilOppgittOpptjeningDto(grunnlag.getOppgittOpptjening().orElse(null)));

        return inntektArbeidYtelseGrunnlagDto;
    }

    public static OppgittOpptjeningDto mapTilOppgittOpptjeningDto(OppgittOpptjening oppgittOpptjening) {
        if (oppgittOpptjening != null) {
            return new OppgittOpptjeningDto(
                oppgittOpptjening.getFrilans().map(IAYTilKalkulatorInputMapper::mapOppgittFrilansOppdragListe).orElse(null),
                mapOppgittEgenNæringListe(oppgittOpptjening.getEgenNæring()),
                mapOppgittArbeidsforholdDto(oppgittOpptjening.getOppgittArbeidsforhold()));
        }
        return null;
    }

    public static List<OppgittEgenNæringDto> mapOppgittEgenNæringListe(List<OppgittEgenNæring> egenNæring) {
        return egenNæring == null ? null : egenNæring.stream().map(IAYTilKalkulatorInputMapper::mapOppgittEgenNæring).collect(Collectors.toList());
    }

    private static List<OppgittArbeidsforholdDto> mapOppgittArbeidsforholdDto(List<OppgittArbeidsforhold> arbeidsforhold) {
        if (arbeidsforhold == null) {
            return null;
        }
        return arbeidsforhold.stream().map(IAYTilKalkulatorInputMapper::mapArbeidsforhold).collect(Collectors.toList());
    }

    private static OppgittFrilansDto mapOppgittFrilansOppdragListe(OppgittFrilans oppgittFrilans) {
        return new OppgittFrilansDto(Boolean.TRUE.equals(oppgittFrilans.getErNyoppstartet()), Collections.emptyList());
    }

}
