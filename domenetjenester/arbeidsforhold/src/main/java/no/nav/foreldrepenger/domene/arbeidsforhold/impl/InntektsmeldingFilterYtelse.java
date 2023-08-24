package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface InntektsmeldingFilterYtelse {

    /**
     * Returnerer påkrevde inntektsmeldinger etter ytelsesspesifikke vurdering og
     * filtrering
     */
    <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse, Map<Arbeidsgiver, Set<V>> påkrevde);

    Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse,
                                                                                            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                                            Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevde);
}
