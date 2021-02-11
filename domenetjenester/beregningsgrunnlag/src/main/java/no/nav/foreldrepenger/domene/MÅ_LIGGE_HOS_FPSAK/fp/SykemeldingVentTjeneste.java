package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
public class SykemeldingVentTjeneste {
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;

    public SykemeldingVentTjeneste() {
        // CDI
    }

    @Inject
    public SykemeldingVentTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
    }

    public Optional<LocalDate> skalVentePåSykemelding(BehandlingReferanse referanse) {
        boolean erFødendeKvinneSomSøkerForeldrepenger = besteberegningFødendeKvinneTjeneste.erFødendeKvinneSomSøkerForeldrepenger(referanse);

        if (!erFødendeKvinneSomSøkerForeldrepenger) {
            return Optional.empty();
        }

        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingUuid());
        YtelseFilter filter = new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(referanse.getAktørId()));
        return VentPåSykemelding.utledVenteFrist(filter, referanse.getSkjæringstidspunktOpptjening(), LocalDate.now());
    }
}
