package no.nav.foreldrepenger.domene.vedtak.observer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.AnvistAndel;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateSegment;

class VedtattYtelseMapper {

    private List<ArbeidsforholdReferanse> arbeidsforholdReferanser = new ArrayList<>();
    private final boolean skalMappeArbeidsforhold;

    private VedtattYtelseMapper(List<ArbeidsforholdReferanse> arbeidsforholdReferanser) {
        this.arbeidsforholdReferanser = arbeidsforholdReferanser;
        this.skalMappeArbeidsforhold = true;
    }

    private VedtattYtelseMapper(boolean skalMappeArbeidsforhold) {
        this.skalMappeArbeidsforhold = skalMappeArbeidsforhold;
    }

    static VedtattYtelseMapper medArbeidsforhold(List<ArbeidsforholdReferanse> arbeidsforholdReferanser) {
        return new VedtattYtelseMapper(arbeidsforholdReferanser);
    }

    static VedtattYtelseMapper utenArbeidsforhold() {
        return new VedtattYtelseMapper(false);
    }

    List<Anvisning> mapForeldrepenger(BeregningsresultatEntitet tilkjent) {
        return tilkjent.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(this::mapForeldrepengerPeriode)
            .collect(Collectors.toList());
    }

    private Anvisning mapForeldrepengerPeriode(BeregningsresultatPeriode periode) {
        return mapPeriode(periode, periode.getDagsatsFraBg(), periode.getKalkulertUtbetalingsgrad());
    }

    List<Anvisning> mapSvangerskapspenger(BeregningsresultatEntitet tilkjent, BeregningsgrunnlagEntitet beregningsgrunnlag) {
        var grunnlagSatsUtbetGrad = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsgrunnlagPeriodeFom(), p.getBeregningsgrunnlagPeriodeTom(), beregnGrunnlagSatsUtbetGradSvp(p, beregningsgrunnlag.getGrunnbeløp().getVerdi())))
            .collect(Collectors.toList());
        return tilkjent.getBeregningsresultatPerioder().stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> mapSvangerskapspengerPeriode(p, grunnlagSatsUtbetGrad))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Anvisning mapSvangerskapspengerPeriode(BeregningsresultatPeriode periode, List<LocalDateSegment<DagsatsUtbgradSVP>> dagsatsGrader) {
        var utledetDagsatsGrad = finnKombinertDagsatsUtbetaling(periode, dagsatsGrader);
        var dagsats = utledetDagsatsGrad != null ? utledetDagsatsGrad.dagsats() : periode.getDagsatsFraBg();
        var utbetalingsgrad = utledetDagsatsGrad != null ? new BigDecimal(utledetDagsatsGrad.utbetalingsgrad()) : periode.getKalkulertUtbetalingsgrad();
        return mapPeriode(periode, dagsats, utbetalingsgrad);
    }

    private static DagsatsUtbgradSVP finnKombinertDagsatsUtbetaling(BeregningsresultatPeriode periode, List<LocalDateSegment<DagsatsUtbgradSVP>> dagsatsGrader) {
        return dagsatsGrader.stream()
            .filter(d -> d.getLocalDateInterval().encloses(periode.getBeregningsresultatPeriodeFom())) // Antar at BR-perioder ikke krysser BG-perioder
            .findFirst()
            .map(LocalDateSegment::getValue)
            .orElse(null);
    }

    private static DagsatsUtbgradSVP beregnGrunnlagSatsUtbetGradSvp(BeregningsgrunnlagPeriode bgPeriode, BigDecimal grunnbeløp) {
        var seksG = new BigDecimal(6).multiply(grunnbeløp);
        var avkortet = bgPeriode.getBruttoPrÅr().compareTo(seksG) > 0 ? seksG : bgPeriode.getBruttoPrÅr();
        var grad = BigDecimal.ZERO.compareTo(avkortet) == 0 ? 0 :
            BigDecimal.TEN.multiply(BigDecimal.TEN).multiply(bgPeriode.getRedusertPrÅr()).divide(avkortet, RoundingMode.HALF_EVEN).longValue();
        var dagsats = BigDecimal.ZERO.compareTo(bgPeriode.getRedusertPrÅr()) == 0 ? 0 :
            new BigDecimal(bgPeriode.getDagsats()).multiply(avkortet).divide(bgPeriode.getRedusertPrÅr(), RoundingMode.HALF_EVEN).longValue();
        return new DagsatsUtbgradSVP(dagsats, grad);
    }

    private Anvisning mapPeriode(BeregningsresultatPeriode periode, long dagsats, BigDecimal utbetalingsgrad) {
        final var anvisning = new Anvisning();
        final var p = new Periode();
        p.setFom(periode.getBeregningsresultatPeriodeFom());
        p.setTom(periode.getBeregningsresultatPeriodeTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(dagsats)));
        anvisning.setUtbetalingsgrad(new Desimaltall(utbetalingsgrad));
        anvisning.setAndeler(skalMappeArbeidsforhold ? mapAndeler(periode.getBeregningsresultatAndelList()) : Collections.emptyList());
        return anvisning;
    }

    private List<AnvistAndel> mapAndeler(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        Map<AnvistAndelNøkkel, List<BeregningsresultatAndel>> resultatPrNøkkkel = beregningsresultatAndelList.stream()
            .collect(Collectors.groupingBy(a -> new AnvistAndelNøkkel(a.getArbeidsgiver().orElse(null), a.getArbeidsforholdRef(), a.getInntektskategori())));
        return resultatPrNøkkkel.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(this::mapTilAnvistAndel)
            .collect(Collectors.toList());
    }

    private AnvistAndel mapTilAnvistAndel(Map.Entry<AnvistAndelNøkkel, List<BeregningsresultatAndel>> e) {
        return new AnvistAndel(
            mapAktør(e.getKey().arbeidsgiver()),
            finnEksternReferanse(e.getKey().arbeidsforholdRef()),
            new Desimaltall(finnTotalBeløp(e.getValue())),
            finnUtbetalingsgrad(e.getValue()),
            new Desimaltall(finnRefusjonsgrad(e.getValue())),
            mapInntektklasse(e.getKey().inntektskategori())
        );
    }

    private static Inntektklasse mapInntektklasse(Inntektskategori inntektskategori) {
        return switch(inntektskategori) {
            case ARBEIDSTAKER -> Inntektklasse.ARBEIDSTAKER;
            case ARBEIDSTAKER_UTEN_FERIEPENGER -> Inntektklasse.ARBEIDSTAKER_UTEN_FERIEPENGER;
            case FRILANSER -> Inntektklasse.FRILANSER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Inntektklasse.SELVSTENDIG_NÆRINGSDRIVENDE;
            case DAGPENGER -> Inntektklasse.DAGPENGER;
            case ARBEIDSAVKLARINGSPENGER -> Inntektklasse.ARBEIDSAVKLARINGSPENGER;
            case SJØMANN -> Inntektklasse.MARITIM;
            case DAGMAMMA -> Inntektklasse.DAGMAMMA;
            case JORDBRUKER -> Inntektklasse.JORDBRUKER;
            case FISKER -> Inntektklasse.FISKER;
            default -> Inntektklasse.INGEN;
        };
    }

    private static BigDecimal finnRefusjonsgrad(List<BeregningsresultatAndel> resultatAndeler) {
        var refusjon = resultatAndeler.stream()
            .filter(a -> !a.erBrukerMottaker())
            .map(BeregningsresultatAndel::getDagsats)
            .reduce(Integer::sum)
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO);

        var total = finnTotalBeløp(resultatAndeler);

        return total.compareTo(BigDecimal.ZERO) > 0 ?
            refusjon.multiply(BigDecimal.valueOf(100)).divide(total, 10, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
    }

    private static Desimaltall finnUtbetalingsgrad(List<BeregningsresultatAndel> resultatAndeler) {
        BigDecimal utbetalingsgrad = resultatAndeler.stream().map(BeregningsresultatAndel::getUtbetalingsgrad)
            .map(grad -> grad == null ? BigDecimal.valueOf(100) : grad)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO)
            .divide(BigDecimal.valueOf(resultatAndeler.size()), 10, RoundingMode.HALF_UP);
        return new Desimaltall(utbetalingsgrad);
    }

    private static BigDecimal finnTotalBeløp(List<BeregningsresultatAndel> resultatAndeler) {
        return resultatAndeler.stream().map(BeregningsresultatAndel::getDagsats)
            .reduce(Integer::sum)
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO);
    }

    private String finnEksternReferanse(InternArbeidsforholdRef internArbeidsforholdRef) {
        if (internArbeidsforholdRef == null || !internArbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            return null;
        }
        return this.arbeidsforholdReferanser.stream()
            .filter(r -> r.getInternReferanse().gjelderFor(internArbeidsforholdRef))
            .findFirst().map(ArbeidsforholdReferanse::getEksternReferanse)
            .map(EksternArbeidsforholdRef::getReferanse)
            .orElse(null);
    }

    private static no.nav.abakus.iaygrunnlag.Aktør mapAktør(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        if (arbeidsgiver.getErVirksomhet()) {
            return new Organisasjon(arbeidsgiver.getIdentifikator());
        }
        return new AktørIdPersonident(arbeidsgiver.getIdentifikator());
    }

    private static record AnvistAndelNøkkel(Arbeidsgiver arbeidsgiver,
                                            InternArbeidsforholdRef arbeidsforholdRef,
                                            Inntektskategori inntektskategori) implements Comparable<AnvistAndelNøkkel> {
        @Override
        public int compareTo(AnvistAndelNøkkel o) {
            if (this.equals(o)) {
                return 0;
            }
            boolean arbeidsgiverErLik = Objects.equals(this.arbeidsgiver, o.arbeidsgiver());
            if (arbeidsgiverErLik) {
                boolean arbeidsforholdRefErLik = Objects.equals(this.arbeidsforholdRef, o.arbeidsforholdRef());
                if (arbeidsforholdRefErLik) {
                    return this.inntektskategori().compareTo(o.inntektskategori());
                }
                if (this.arbeidsforholdRef.getReferanse() != null && o.arbeidsforholdRef().getReferanse() != null) {
                    return this.arbeidsforholdRef.getReferanse().compareTo(o.arbeidsforholdRef().getReferanse());
                }
                return this.arbeidsforholdRef.getReferanse() != null ? 1 : -1;
            }
            if (this.arbeidsgiver() != null && o.arbeidsgiver() != null) {
                return this.arbeidsgiver().getIdentifikator().compareTo(o.arbeidsgiver().getIdentifikator());
            }
            return this.arbeidsgiver != null ? 1 : -1;
        }
    }

    private static record DagsatsUtbgradSVP(long dagsats, long utbetalingsgrad) {
    }

}
