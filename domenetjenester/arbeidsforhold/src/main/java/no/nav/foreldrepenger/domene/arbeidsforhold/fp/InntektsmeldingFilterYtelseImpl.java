package no.nav.foreldrepenger.domene.arbeidsforhold.fp;

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

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class InntektsmeldingFilterYtelseImpl implements InntektsmeldingFilterYtelse {

    @Inject
    public InntektsmeldingFilterYtelseImpl() {
        //
    }

    @Override
    public Set<Arbeidsgiver> søknadsFilter(BehandlingReferanse referanse, Set<Arbeidsgiver> påkrevde) {
        return påkrevde;
    }

    @Override
    public Set<Arbeidsgiver> aktiveArbeidsforholdFilter(BehandlingReferanse referanse,
                                                        Skjæringstidspunkt skjæringstidspunkt,
                                                        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                        Set<Arbeidsgiver> påkrevde) {
        return InaktiveArbeidsforholdUtleder.finnKunAktive(påkrevde, inntektArbeidYtelseGrunnlag, referanse, skjæringstidspunkt);
    }
}
