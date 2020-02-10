package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

public interface InntektsmeldingFilterYtelse {

    /** Returnerer påkrevde inntektsmeldinger etter ytelsesspesifikke vurdering og filtrering */
    <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                    Map<Arbeidsgiver, Set<V>> påkrevde);

    <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                        Map<Arbeidsgiver, Set<V>> påkrevde);
}
