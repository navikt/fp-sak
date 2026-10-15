package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

public interface InntektsmeldingFilterYtelse {

    /**
     * Returnerer arbeidsgivere vi krever inntektsmelding fra etter ytelsesspesifikk vurdering og
     * filtrering
     */
    Set<Arbeidsgiver> søknadsFilter(BehandlingReferanse referanse, Set<Arbeidsgiver> påkrevde);

    Set<Arbeidsgiver> aktiveArbeidsforholdFilter(BehandlingReferanse referanse,
                                                 Skjæringstidspunkt stp,
                                                 Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                 Set<Arbeidsgiver> påkrevde);
}
