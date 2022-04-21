package no.nav.foreldrepenger.domene.fp;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;

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
        var erFødendeKvinneSomSøkerForeldrepenger = besteberegningFødendeKvinneTjeneste.erFødendeKvinneSomSøkerForeldrepenger(referanse);

        if (!erFødendeKvinneSomSøkerForeldrepenger) {
            return Optional.empty();
        }

        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.behandlingUuid());
        var filter = new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(referanse.aktørId()));
        return VentPåSykemelding.utledVenteFrist(filter, referanse.getSkjæringstidspunkt().getSkjæringstidspunktOpptjening(), LocalDate.now());
    }
}
