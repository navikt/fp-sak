package no.nav.foreldrepenger.domene.vedtak.observer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.AnvistAndel;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.ArbeidsgiverIdent;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class VedtattYtelseMapper {

    private final Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser;

    private VedtattYtelseMapper(Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser) {
        this.arbeidsforholdReferanser = arbeidsforholdReferanser;
    }

    static VedtattYtelseMapper medArbeidsforhold(Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser) {
        return new VedtattYtelseMapper(arbeidsforholdReferanser);
    }

    List<Anvisning> mapTilkjent(BeregningsresultatEntitet tilkjent) {
        return tilkjent.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(this::mapPeriode)
            .toList();
    }

    private Anvisning mapPeriode(BeregningsresultatPeriode periode) {
        var anvisning = new Anvisning();
        var p = new Periode();
        p.setFom(periode.getBeregningsresultatPeriodeFom());
        p.setTom(periode.getBeregningsresultatPeriodeTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(BigDecimal.valueOf(periode.getDagsats())));
        anvisning.setUtbetalingsgrad(new Desimaltall(periode.getKalkulertUtbetalingsgrad()));
        anvisning.setAndeler(mapAndeler(periode.getBeregningsresultatAndelList()));
        return anvisning;
    }

    private List<AnvistAndel> mapAndeler(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        var resultatPrNøkkkel = beregningsresultatAndelList.stream()
            .collect(Collectors.groupingBy(a -> new AnvistAndelNøkkel(a.getArbeidsgiver().orElse(null), a.getArbeidsforholdRef(), a.getInntektskategori())));
        return resultatPrNøkkkel.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(this::mapTilAnvistAndel)
            .toList();
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
        var utbetalingsgrad = resultatAndeler.stream().map(BeregningsresultatAndel::getUtbetalingsgrad)
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

    private static ArbeidsgiverIdent mapAktør(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver != null ? new ArbeidsgiverIdent(arbeidsgiver.getIdentifikator()) : null;
    }

    private record AnvistAndelNøkkel(Arbeidsgiver arbeidsgiver,
                                     InternArbeidsforholdRef arbeidsforholdRef,
                                     Inntektskategori inntektskategori) implements Comparable<AnvistAndelNøkkel> {
        @Override
        public int compareTo(AnvistAndelNøkkel o) {
            if (this.equals(o)) {
                return 0;
            }
            var arbeidsgiverErLik = Objects.equals(this.arbeidsgiver, o.arbeidsgiver());
            if (arbeidsgiverErLik) {
                var arbeidsforholdRefErLik = Objects.equals(this.arbeidsforholdRef, o.arbeidsforholdRef());
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
}
