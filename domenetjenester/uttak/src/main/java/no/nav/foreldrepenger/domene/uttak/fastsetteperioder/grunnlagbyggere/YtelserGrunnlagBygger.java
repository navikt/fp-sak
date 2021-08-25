package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.PeriodeMedBarnInnlagt;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ytelser.PleiepengerMedInnleggelse;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ytelser.Ytelser;

@ApplicationScoped
public class YtelserGrunnlagBygger {

    public Ytelser byggGrunnlag(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var pleiepengerMedInnleggelse = getPleiepengerMedInnleggelse(fpGrunnlag);
        return new Ytelser(pleiepengerMedInnleggelse.orElse(null));
    }

    private Optional<PleiepengerMedInnleggelse> getPleiepengerMedInnleggelse(ForeldrepengerGrunnlag fpGrunnlag) {
        var pleiepengerGrunnlag = fpGrunnlag.getPleiepengerGrunnlag();
        if (pleiepengerGrunnlag.isPresent()) {
            var perioderMedInnleggelse = pleiepengerGrunnlag.get().getPerioderMedInnleggelse();
            if (perioderMedInnleggelse.isPresent()) {
                var perioder = perioderMedInnleggelse.get()
                    .getInnleggelser()
                    .stream()
                    .map(p -> {
                        var periode = p.getPeriode();
                        return new PeriodeMedBarnInnlagt(periode.getFomDato(), periode.getTomDato());
                    })
                    .toList();
                return Optional.of(new PleiepengerMedInnleggelse(perioder));
            }
        }
        return Optional.empty();
    }
}
