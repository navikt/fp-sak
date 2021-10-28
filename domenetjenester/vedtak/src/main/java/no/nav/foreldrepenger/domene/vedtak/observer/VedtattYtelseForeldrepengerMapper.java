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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class VedtattYtelseForeldrepengerMapper {

    private List<ArbeidsforholdReferanse> arbeidsforholdReferanser = new ArrayList<>();
    private final boolean skalMappeArbeidsforhold;

    private VedtattYtelseForeldrepengerMapper(List<ArbeidsforholdReferanse> arbeidsforholdReferanser) {
        this.arbeidsforholdReferanser = arbeidsforholdReferanser;
        this.skalMappeArbeidsforhold = true;
    }

    private VedtattYtelseForeldrepengerMapper(boolean skalMappeArbeidsforhold) {
        this.skalMappeArbeidsforhold = skalMappeArbeidsforhold;
    }

    static VedtattYtelseForeldrepengerMapper medArbeidsforhold(List<ArbeidsforholdReferanse> arbeidsforholdReferanser) {
        return new VedtattYtelseForeldrepengerMapper(arbeidsforholdReferanser);
    }

    static VedtattYtelseForeldrepengerMapper utenArbeidsforhold() {
        return new VedtattYtelseForeldrepengerMapper(false);
    }

    List<Anvisning> mapForeldrepenger(BeregningsresultatEntitet tilkjent) {
        return tilkjent.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(p -> mapForeldrepengerPeriode(p))
            .collect(Collectors.toList());
    }

    private Anvisning mapForeldrepengerPeriode(BeregningsresultatPeriode periode) {
        final var anvisning = new Anvisning();
        final var p = new Periode();
        p.setFom(periode.getBeregningsresultatPeriodeFom());
        p.setTom(periode.getBeregningsresultatPeriodeTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(periode.getDagsatsFraBg())));
        anvisning.setUtbetalingsgrad(new Desimaltall(periode.getKalkulertUtbetalingsgrad()));
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
            mapAktør(e.getKey().getArbeidsgiver()),
            finnEksternReferanse(e.getKey().getArbeidsforholdRef()),
            new Desimaltall(finnTotalBeløp(e.getValue())),
            finnUtbetalingsgrad(e.getValue()),
            new Desimaltall(finnRefusjonsgrad(e.getValue())),
            no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori.fraKode(e.getKey().getInntektskategori().getKode())
        );
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

    private static class AnvistAndelNøkkel implements Comparable<AnvistAndelNøkkel> {
        private final Arbeidsgiver arbeidsgiver;
        private final InternArbeidsforholdRef arbeidsforholdRef;
        private final Inntektskategori inntektskategori;

        public AnvistAndelNøkkel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Inntektskategori inntektskategori) {
            this.arbeidsgiver = arbeidsgiver;
            this.arbeidsforholdRef = arbeidsforholdRef;
            this.inntektskategori = inntektskategori;
        }

        public Arbeidsgiver getArbeidsgiver() {
            return arbeidsgiver;
        }

        public InternArbeidsforholdRef getArbeidsforholdRef() {
            return arbeidsforholdRef;
        }

        public Inntektskategori getInntektskategori() {
            return inntektskategori;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnvistAndelNøkkel that = (AnvistAndelNøkkel) o;
            return Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef) && inntektskategori == that.inntektskategori;
        }

        @Override
        public int hashCode() {
            return Objects.hash(arbeidsgiver, arbeidsforholdRef, inntektskategori);
        }

        @Override
        public int compareTo(AnvistAndelNøkkel o) {
            if (this.equals(o)) {
                return 0;
            }
            boolean arbeidsgiverErLik = Objects.equals(this.arbeidsgiver, o.getArbeidsgiver());
            if (arbeidsgiverErLik) {
                boolean arbeidsforholdRefErLik = Objects.equals(this.arbeidsforholdRef, o.getArbeidsforholdRef());
                if (arbeidsforholdRefErLik) {
                    return this.getInntektskategori().compareTo(o.getInntektskategori());
                }
                if (this.arbeidsforholdRef.getReferanse() != null && o.getArbeidsforholdRef().getReferanse() != null) {
                    return this.arbeidsforholdRef.getReferanse().compareTo(o.getArbeidsforholdRef().getReferanse());
                }
                return this.arbeidsforholdRef.getReferanse() != null ? 1 : -1;
            }
            if (this.getArbeidsgiver() != null && o.getArbeidsgiver() != null) {
                return this.getArbeidsgiver().getIdentifikator().compareTo(o.getArbeidsgiver().getIdentifikator());
            }
            return this.arbeidsgiver != null ? 1 : -1;
        }
    }


}
