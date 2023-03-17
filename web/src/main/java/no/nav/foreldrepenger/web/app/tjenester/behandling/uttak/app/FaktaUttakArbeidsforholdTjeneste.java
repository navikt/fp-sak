package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

public class FaktaUttakArbeidsforholdTjeneste {

    FaktaUttakArbeidsforholdTjeneste() {
        // CDI
    }

    public static List<ArbeidsforholdDto> hentArbeidsforhold(UttakInput input) {
        return input.getBeregningsgrunnlagStatuser().stream()
            .map(FaktaUttakArbeidsforholdTjeneste::map)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .toList();
    }

    private static Optional<ArbeidsforholdDto> map(BeregningsgrunnlagStatus statusPeriode) {

        if (statusPeriode.erFrilanser()) {
            return Optional.of(ArbeidsforholdDto.frilans());
        }
        if (statusPeriode.erSelvstendigNæringsdrivende()) {
            return Optional.of(ArbeidsforholdDto.selvstendigNæringsdrivende());
        }
        if (statusPeriode.erArbeidstaker()) {
            return mapArbeidstaker(statusPeriode);
        }
        return Optional.empty();
    }

    private static Optional<ArbeidsforholdDto> mapArbeidstaker(BeregningsgrunnlagStatus andel) {
        return andel.getArbeidsgiver().map(a -> ArbeidsforholdDto.ordinært(a.getIdentifikator()));
    }
}
