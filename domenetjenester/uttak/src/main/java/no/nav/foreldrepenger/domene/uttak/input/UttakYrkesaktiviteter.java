package no.nav.foreldrepenger.domene.uttak.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;

public class UttakYrkesaktiviteter {
    private final UttakInput input;

    public UttakYrkesaktiviteter(UttakInput input) {
        this.input = input;
    }

    private List<Yrkesaktivitet> hentYrkesAktiviteterOrdinærtArbeidsforhold(UttakInput input) {
        var grunnlag = input.getIayGrunnlag();
        if (grunnlag == null) {
            return Collections.emptyList();
        }
        var bgStatuser = input.getBeregningsgrunnlagStatuser();
        var ref = input.getBehandlingReferanse();
        var skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();

        var aktørId = ref.aktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
            grunnlag.getAktørArbeidFraRegister(aktørId)).etter(skjæringstidspunkt);

        return filter.getYrkesaktiviteter()
            .stream()
            .filter(yrkesaktivitet -> skalYrkesaktivitetTasMed(yrkesaktivitet, bgStatuser))
            .collect(Collectors.toList());
    }

    private boolean skalYrkesaktivitetTasMed(Yrkesaktivitet yrkesaktivitet,
                                             Collection<BeregningsgrunnlagStatus> statuser) {
        return statuser.stream().anyMatch(bgStatus -> skalYrkesaktivitetTasMed(yrkesaktivitet, bgStatus));
    }

    private boolean skalYrkesaktivitetTasMed(Yrkesaktivitet yrkesaktivitet, BeregningsgrunnlagStatus bgStatus) {
        var arbeidsgiver = bgStatus.getArbeidsgiver().orElse(null);
        var arbeidsforhold = bgStatus.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef());
        var arbeidsgiver2 = yrkesaktivitet.getArbeidsgiver();
        var arbeidsforhold2 = Optional.ofNullable(yrkesaktivitet.getArbeidsforholdRef())
            .orElse(InternArbeidsforholdRef.nullRef());
        if (arbeidsgiver == null || arbeidsgiver2 == null) {
            return false;
        }

        return Objects.equals(arbeidsgiver, arbeidsgiver2) && arbeidsforhold.gjelderFor(arbeidsforhold2);
    }

    public BigDecimal finnStillingsprosentOrdinærtArbeid(Arbeidsgiver arbeidsgiver,
                                                         InternArbeidsforholdRef arbeidsforholdRef,
                                                         LocalDate dato) {
        var yrkesAktiviteter = hentYrkesAktiviteterOrdinærtArbeidsforhold(input);
        var ref = input.getBehandlingReferanse();
        return finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbeidsforholdRef, yrkesAktiviteter, dato,
            ref.getSkjæringstidspunkt());
    }

    public Set<AktivitetIdentifikator> tilAktivitetIdentifikatorer() {
        return input.getBeregningsgrunnlagStatuser()
            .stream()
            .map(statusPeriode -> statusPeriode.toUttakAktivitetIdentifikator())
            .collect(Collectors.toSet());
    }

    private BigDecimal finnStillingsprosentOrdinærtArbeid(Arbeidsgiver arbeidsgiver,
                                                          InternArbeidsforholdRef ref,
                                                          List<Yrkesaktivitet> yrkesaktivitetList,
                                                          LocalDate dato,
                                                          Skjæringstidspunkt skjæringstidspunkt) {

        var filter0 = new YrkesaktivitetFilter(null, yrkesaktivitetList);
        var yaMedAnsettelsesperiodePåDato = yaMedAnsettelsesperiodePåDato(filter0, arbeidsgiver, ref,
            yrkesaktivitetList, dato);

        var filter = new YrkesaktivitetFilter(null, yaMedAnsettelsesperiodePåDato).etter(
            skjæringstidspunkt.getUtledetSkjæringstidspunkt());

        var sum = BigDecimal.ZERO;
        for (var ya : filter.getAlleYrkesaktiviteter()) {
            var aktivitetsAvtaler = filter.getAktivitetsAvtalerForArbeid(ya);
            if (ArbeidType.FORENKLET_OPPGJØRSORDNING.equals(ya.getArbeidType()) && aktivitetsAvtaler.isEmpty()) {
                // Forekommer i halvparten av tilfellene pga rapportertingsmåte. Antar 0% i disse tilfellene.
                continue;
            }
            if (aktivitetsAvtaler.isEmpty()) {
                var melding = "Forventer minst en aktivitetsavtale ved dato " + dato.toString() + " i yrkesaktivitet"
                    + ya.toString() + " med ansettelsesperioder " + filter.getAnsettelsesPerioder(ya).toString()
                    + " og alle aktivitetsavtaler " + aktivitetsAvtaler.toString();
                throw new IllegalStateException(melding);
            }
            sum = sum.add(finnStillingsprosent(aktivitetsAvtaler, dato));
        }

        return sum;
    }

    private List<Yrkesaktivitet> yaMedAnsettelsesperiodePåDato(YrkesaktivitetFilter filter,
                                                               Arbeidsgiver arbeidsgiver,
                                                               InternArbeidsforholdRef ref,
                                                               List<Yrkesaktivitet> yrkesaktivitetList,
                                                               LocalDate dato) {
        return yrkesaktivitetList.stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .filter(ya -> riktigDato(dato, ansettelsePeriodeForYrkesaktivitet(filter, ya)))
            .filter(ya -> Objects.equals(ya.getArbeidsgiver(), arbeidsgiver))
            .filter(ya -> ya.getArbeidsforholdRef().gjelderFor(ref == null ? InternArbeidsforholdRef.nullRef() : ref))
            .collect(Collectors.toList());
    }

    private BigDecimal finnStillingsprosent(Collection<AktivitetsAvtale> aktivitetsAvtaler, LocalDate dato) {
        var aktivitetPåDato = finnAktivitetPåDato(aktivitetsAvtaler, dato);
        if (aktivitetPåDato.getProsentsats() == null) {
            return BigDecimal.ZERO;
        }
        return aktivitetPåDato.getProsentsats().getVerdi();
    }

    private AktivitetsAvtale finnAktivitetPåDato(Collection<AktivitetsAvtale> aktivitetsAvtaler, LocalDate dato) {
        var overlapper = aktivitetsAvtaler.stream()
            .filter(aa -> riktigDato(dato, aa))
            .max(Comparator.comparing(o -> {
                if (o.getProsentsats() == null) {
                    return BigDecimal.ZERO;
                }
                return o.getProsentsats().getVerdi();
            }));
        if (overlapper.isPresent()) {
            return overlapper.get();
        }

        // Ingen avtaler finnes på dato. Bruker nærmeste avtale. Kommer av dårlig datakvalitet i registerne
        var sortert = sortertPåDato(aktivitetsAvtaler);
        var førsteAktivitetsavtale = sortert.get(0);
        // Hull i starten av ansettelsesperioden
        if (dato.isBefore(førsteAktivitetsavtale.getPeriode().getFomDato())) {
            return førsteAktivitetsavtale;
        }
        // Hull på slutten av ansettelsesperioden
        return sortert.get(aktivitetsAvtaler.size() - 1);

    }

    private List<AktivitetsAvtale> sortertPåDato(Collection<AktivitetsAvtale> aktivitetsAvtaler) {
        return aktivitetsAvtaler.stream()
            .sorted(Comparator.comparing(aktivitetsAvtale -> aktivitetsAvtale.getPeriode().getFomDato()))
            .collect(Collectors.toList());
    }

    private List<AktivitetsAvtale> ansettelsePeriodeForYrkesaktivitet(YrkesaktivitetFilter filter, Yrkesaktivitet ya) {
        var ansettelsesPerioder = filter.getAnsettelsesPerioder(ya);
        if (ansettelsesPerioder.isEmpty()) {
            throw new IllegalStateException("Forventet at alle yrkesaktiviteter har en ansettelsesperiode");
        }
        return ansettelsesPerioder;
    }

    private boolean riktigDato(LocalDate dato, List<AktivitetsAvtale> avtaler) {
        return avtaler.stream().anyMatch(avtale -> riktigDato(dato, avtale));
    }

    private boolean riktigDato(LocalDate dato, AktivitetsAvtale avtale) {
        return (avtale.getPeriode().getFomDato().isEqual(dato) || avtale.getPeriode().getFomDato().isBefore(dato)) &&
            (avtale.getPeriode().getTomDato().isEqual(dato) || avtale.getPeriode().getTomDato().isAfter(dato));
    }

    public Optional<LocalDate> finnStartdato(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return avtalerForOrdinærtArbeidsforhold(arbeidsgiver, arbeidsforholdRef)
            .min(Comparator.comparing(o -> o.getPeriode().getFomDato()))
            .map(ansettelsesPeriode -> ansettelsesPeriode.getPeriode().getFomDato());
    }

    private Stream<AktivitetsAvtale> avtalerForOrdinærtArbeidsforhold(Arbeidsgiver arbeidsgiver,
                                                                      InternArbeidsforholdRef arbeidsforholdRef) {
        var yrkesaktiviteter = hentYrkesAktiviteterOrdinærtArbeidsforhold(input);
        var filter = new YrkesaktivitetFilter(yrkesaktiviteter);

        return yrkesaktiviteter.stream()
            .filter(yrkesaktivitet -> yrkesaktivitet.gjelderFor(arbeidsgiver, arbeidsforholdRef))
            .flatMap(yrkesaktivitet -> filter.getAnsettelsesPerioder(yrkesaktivitet).stream());
    }
}
