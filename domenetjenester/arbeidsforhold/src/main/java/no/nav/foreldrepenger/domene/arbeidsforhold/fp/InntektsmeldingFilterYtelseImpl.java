package no.nav.foreldrepenger.domene.arbeidsforhold.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingFilterYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class InntektsmeldingFilterYtelseImpl implements InntektsmeldingFilterYtelse {

    private static final Period SJEKK_INNTEKT_PERIODE = Period.parse("P10M");

    @Inject
    public InntektsmeldingFilterYtelseImpl() {
        //
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse,
            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
            Map<Arbeidsgiver, Set<V>> påkrevde) {
        return påkrevde;
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse,
            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
            Map<Arbeidsgiver, Set<V>> påkrevde) {
        if (!inntektArbeidYtelseGrunnlag.isPresent()) {
            return påkrevde;
        }
        Map<Arbeidsgiver, Set<V>> filtrert = new HashMap<>();
        var inntekterPrArbgiver = hentInntekterForUtledningAvInntektsmeldinger(referanse,
                inntektArbeidYtelseGrunnlag.get());
        påkrevde.forEach((key, value) -> {
            if ((inntekterPrArbgiver.get(key) != null) && !inntekterPrArbgiver.get(key).isEmpty()) {
                filtrert.put(key, value);
            }
        });
        // Ligg til annen logikk, som fx utelate arbeidsforhold med stillingsprosent 0.
        return filtrert;
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForKompletthetAktive(BehandlingReferanse referanse,
            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
            Map<Arbeidsgiver, Set<V>> påkrevde) {
        if (!inntektArbeidYtelseGrunnlag.isPresent()) {
            return påkrevde;
        }
        Map<Arbeidsgiver, Set<V>> filtrert = new HashMap<>();
        var inntekterPrArbgiver = hentInntekterForUtledningAvInntektsmeldinger(referanse,
                inntektArbeidYtelseGrunnlag.get());
        var aktiveArbeidsgivere = inntekterSisteFireMåneder(referanse, inntekterPrArbgiver);
        påkrevde.forEach((key, value) -> {
            if (aktiveArbeidsgivere.contains(key)) {
                filtrert.put(key, value);
            }
        });
        return filtrert;
    }

    private Map<Arbeidsgiver, Set<Inntektspost>> hentInntekterForUtledningAvInntektsmeldinger(BehandlingReferanse referanse,
            InntektArbeidYtelseGrunnlag grunnlag) {
        var inntektsPeriodeFom = referanse.getUtledetSkjæringstidspunkt().minus(SJEKK_INNTEKT_PERIODE);
        Map<Arbeidsgiver, Set<Inntektspost>> inntekterPrArbgiver = new HashMap<>();

        var filter = grunnlag.getAktørInntektFraRegister(referanse.getAktørId())
                .map(ai -> new InntektFilter(ai).før(referanse.getUtledetSkjæringstidspunkt())).orElse(InntektFilter.EMPTY);

        filter.getAlleInntektPensjonsgivende()
                .forEach(inntekt -> {
                    var poster = filter.filtrer(inntekt, inntekt.getAlleInntektsposter()).stream()
                            .filter(ip -> !InntektspostType.YTELSE.equals(ip.getInntektspostType()))
                            .filter(ip -> ip.getPeriode().getFomDato().isAfter(inntektsPeriodeFom))
                            .filter(it -> it.getBeløp().getVerdi().compareTo(BigDecimal.ZERO) > 0)
                            .collect(Collectors.toSet());
                    if (inntekterPrArbgiver.get(inntekt.getArbeidsgiver()) != null) {
                        inntekterPrArbgiver.get(inntekt.getArbeidsgiver()).addAll(poster);
                    } else {
                        inntekterPrArbgiver.put(inntekt.getArbeidsgiver(), poster);
                    }
                });
        return inntekterPrArbgiver;
    }

    private List<Arbeidsgiver> inntekterSisteFireMåneder(BehandlingReferanse referanse,
            Map<Arbeidsgiver, Set<Inntektspost>> inntektOpptjeningsperiode) {
        List<Arbeidsgiver> aktiveArbeidsgivere = new ArrayList<>();
        var stp = referanse.getUtledetSkjæringstidspunkt();
        var tidligstedato = LocalDate.now().isBefore(stp) ? LocalDate.now() : stp;
        var inntekterFom = tidligstedato.getDayOfMonth() > 5 ? tidligstedato.minusMonths(4).withDayOfMonth(1)
                : tidligstedato.minusMonths(5).withDayOfMonth(1);
        inntektOpptjeningsperiode.forEach((key, value) -> {
            var poster = value.stream()
                    .filter(i -> i.getPeriode().getTomDato().isAfter(inntekterFom) && i.getPeriode().getFomDato().isBefore(tidligstedato))
                    .map(Inntektspost::getPeriode).map(DatoIntervallEntitet::getFomDato).map(LocalDate::getMonthValue)
                    .collect(Collectors.toSet());
            if (poster.size() >= 2) {
                aktiveArbeidsgivere.add(key);
            }
        });
        return aktiveArbeidsgivere;
    }
}
