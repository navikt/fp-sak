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
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
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
        var skjæringstidspunkt = input.getSkjæringstidspunkt().orElseThrow().getUtledetSkjæringstidspunkt();

        var aktørId = ref.aktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
            grunnlag.getAktørArbeidFraRegister(aktørId)).etter(skjæringstidspunkt);

        return filter.getYrkesaktiviteter()
            .stream()
            .filter(yrkesaktivitet -> skalYrkesaktivitetTasMed(yrkesaktivitet, bgStatuser))
            .toList();
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
        return finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbeidsforholdRef, yrkesAktiviteter, dato,
            input.getSkjæringstidspunkt().orElseThrow());
    }

    public Set<AktivitetIdentifikator> tilAktivitetIdentifikatorer() {
        return input.getBeregningsgrunnlagStatuser()
            .stream()
            .map(BeregningsgrunnlagStatus::toUttakAktivitetIdentifikator)
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

        return filter.getAlleYrkesaktiviteter().stream()
            .filter(ya -> skalYrkesaktivitetTellesMhpProsent(filter, ya, dato))
            .map(ya -> finnStillingsprosent(filter.getAktivitetsAvtalerForArbeid(ya), dato))
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }

    private boolean skalYrkesaktivitetTellesMhpProsent(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet, LocalDate dato) {
        var aktivitetsAvtaler = filter.getAktivitetsAvtalerForArbeid(yrkesaktivitet);
        if (aktivitetsAvtaler.isEmpty()) {
            if (ArbeidType.FORENKLET_OPPGJØRSORDNING.equals(yrkesaktivitet.getArbeidType())) {
                // Forekommer i halvparten av tilfellene pga rapportertingsmåte. Antar 0% i disse tilfellene.
                return false;
            } else {
                var melding = "Forventer minst en aktivitetsavtale ved dato " + dato.toString() + " i yrkesaktivitet"
                    + yrkesaktivitet + " med ansettelsesperioder " + filter.getAnsettelsesPerioder(yrkesaktivitet).toString()
                    + " og alle aktivitetsavtaler " + aktivitetsAvtaler;
                throw new IllegalStateException(melding);
            }
        }
        return true;
    }

    public BigDecimal summerStillingsprosentAlleYrkesaktiviteter(LocalDate dato) {
        var grunnlag = input.getIayGrunnlag();
        if (grunnlag == null) {
            return BigDecimal.ZERO;
        }
        var ref = input.getBehandlingReferanse();
        var skjæringstidspunkt = input.getSkjæringstidspunkt().orElseThrow().getUtledetSkjæringstidspunkt();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
            grunnlag.getAktørArbeidFraRegister(ref.aktørId())).etter(skjæringstidspunkt);

        return filter.getYrkesaktiviteter().stream()
            .filter(ya -> !filter.getAktivitetsAvtalerForArbeid(ya).isEmpty() || !ArbeidType.FORENKLET_OPPGJØRSORDNING.equals(ya.getArbeidType()))
            .map(y -> finnStillingsprosent(filter.getAktivitetsAvtalerForArbeid(y), dato))
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
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
            .toList();
    }

    private BigDecimal finnStillingsprosent(Collection<AktivitetsAvtale> aktivitetsAvtaler, LocalDate dato) {
        return finnAktivitetPåDato(aktivitetsAvtaler, dato)
            .map(AktivitetsAvtale::getProsentsats)
            .map(Stillingsprosent::getVerdi)
            .orElse(BigDecimal.ZERO);
    }

    private Optional<AktivitetsAvtale> finnAktivitetPåDato(Collection<AktivitetsAvtale> aktivitetsAvtaler, LocalDate dato) {
        var aktuelleAvtaler = aktivitetsAvtaler.stream().filter(aa -> aa.getProsentsats() != null).toList();
        var overlapper = aktuelleAvtaler.stream()
            .filter(aa -> riktigDato(dato, aa))
            .max(Comparator.comparing(aa -> aa.getPeriode().getFomDato()));
        if (overlapper.isPresent()) {
            return overlapper;
        }

        var førDato = aktuelleAvtaler.stream().filter(aa -> aa.getPeriode().getFomDato().isBefore(dato)).toList();
        if (førDato.isEmpty()) {
            return aktuelleAvtaler.stream().min(Comparator.comparing(aa -> aa.getPeriode().getFomDato()));
        } else {
            return førDato.stream().max(Comparator.comparing(aa -> aa.getPeriode().getFomDato()));
        }
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
        return avtale.getPeriode().inkluderer(dato);
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
