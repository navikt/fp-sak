package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public interface InntektsmeldingFilterYtelse {

    /**
     * Returnerer påkrevde inntektsmeldinger etter ytelsesspesifikke vurdering og
     * filtrering
     */
    Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> søknadsFilter(BehandlingReferanse referanse,
                                                                  Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevde);

    Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> aktiveArbeidsforholdFilter(BehandlingReferanse referanse,
                                                                               Skjæringstidspunkt stp,
                                                                               Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                               Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevde);
}
