package no.nav.foreldrepenger.domene.arbeidsforhold.fp;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InaktiveArbeidsforholdUtleder;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingFilterYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class InntektsmeldingFilterYtelseImpl implements InntektsmeldingFilterYtelse {

    @Inject
    public InntektsmeldingFilterYtelseImpl() {
        //
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> søknadsFilter(BehandlingReferanse referanse, Map<Arbeidsgiver, Set<V>> påkrevde) {
        return påkrevde;
    }

    @Override
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> aktiveArbeidsforholdFilter(BehandlingReferanse referanse,
                                                                                      Skjæringstidspunkt skjæringstidspunkt,
                                                                                      Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                                      Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevde) {
        return InaktiveArbeidsforholdUtleder.finnKunAktive(påkrevde, inntektArbeidYtelseGrunnlag, referanse, skjæringstidspunkt);
    }
}
