package no.nav.foreldrepenger.domene.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;

public interface OpptjeningAktivitetVurdering {

    VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                  BehandlingReferanse behandlingReferanse,
                                  Skjæringstidspunkt skjæringstidspunkt,
                                  Yrkesaktivitet overstyrtAktivitet,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  boolean harVærtSaksbehandlet);

    VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                  BehandlingReferanse behandlingReferanse,
                                  Skjæringstidspunkt skjæringstidspunkt,
                                  Yrkesaktivitet overstyrtAktivitet,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  boolean harVærtSaksbehandlet,
                                  Yrkesaktivitet registerAktivitet,
                                  AktivitetsAvtale ansettelsesPeriode);

    default boolean skalInkludereAkkumulertFrilans() {
        return false;
    }

    default boolean skalInkludereDetaljertFrilansOppdrag() {
        return false;
    }
}
