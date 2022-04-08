package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Behandling;

@ApplicationScoped
public class BehandlingGrunnlagBygger {

    public Behandling.Builder byggGrunnlag(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        return new Behandling.Builder()
            .berørtBehandling(fpGrunnlag.isBerørtBehandling())
            .søkerErMor(søkerErMor(ref))
            .kreverSammenhengendeUttak(ref.getSkjæringstidspunkt().kreverSammenhengendeUttak());
    }

    public static boolean søkerErMor(BehandlingReferanse ref) {
        return RelasjonsRolleType.erMor(ref.getRelasjonsRolleType());
    }
}
