package no.nav.foreldrepenger.domene.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;

public interface OpptjeningAktivitetVurdering {

    VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                  BehandlingReferanse behandlingReferanse,
                                  Yrkesaktivitet overstyrtAktivitet,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  boolean harVærtSaksbehandlet);

    VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                  BehandlingReferanse behandlingReferanse,
                                  Yrkesaktivitet registerAktivitet,
                                  Yrkesaktivitet overstyrtAktivitet,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  boolean harVærtSaksbehandlet);
}
