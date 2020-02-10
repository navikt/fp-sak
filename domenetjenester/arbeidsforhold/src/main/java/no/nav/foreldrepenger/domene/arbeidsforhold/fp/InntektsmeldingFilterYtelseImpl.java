package no.nav.foreldrepenger.domene.arbeidsforhold.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
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

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class InntektsmeldingFilterYtelseImpl implements InntektsmeldingFilterYtelse {

    private static final Period SJEKK_INNTEKT_PERIODE = Period.parse("P10M");

    @Inject
    public InntektsmeldingFilterYtelseImpl() {
        //
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                           Map<Arbeidsgiver, Set<V>> påkrevde) {
        return påkrevde;
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                                  Map<Arbeidsgiver, Set<V>> påkrevde) {
        if (!inntektArbeidYtelseGrunnlag.isPresent()) {
            return påkrevde;
        }
        Map<Arbeidsgiver, Set<V>> filtrert = new HashMap<>();
        Map<Arbeidsgiver, Set<Inntektspost>> inntekterPrArbgiver = hentInntekterForUtledningAvInntektsmeldinger(referanse, inntektArbeidYtelseGrunnlag.get());
        påkrevde.entrySet().forEach(entry -> {
            if (inntekterPrArbgiver.get(entry.getKey()) != null && !inntekterPrArbgiver.get(entry.getKey()).isEmpty()) {
                filtrert.put(entry.getKey(), entry.getValue());
            }
        });
        // Ligg til annen logikk, som fx utelate arbeidsforhold med stillingsprosent 0.
        return filtrert;
    }

    private Map<Arbeidsgiver, Set<Inntektspost>> hentInntekterForUtledningAvInntektsmeldinger(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag grunnlag) {
        LocalDate inntektsPeriodeFom = referanse.getUtledetSkjæringstidspunkt().minus(SJEKK_INNTEKT_PERIODE);
        Map<Arbeidsgiver, Set<Inntektspost>> inntekterPrArbgiver = new HashMap<>();
        
        var filter = grunnlag.getAktørInntektFraRegister(referanse.getAktørId()).map(ai -> new InntektFilter(ai).før(referanse.getUtledetSkjæringstidspunkt())).orElse(InntektFilter.EMPTY);

        filter.getAlleInntektPensjonsgivende()
            .forEach(inntekt -> {
            Set<Inntektspost> poster = filter.filtrer(inntekt, inntekt.getAlleInntektsposter()).stream()
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
}
