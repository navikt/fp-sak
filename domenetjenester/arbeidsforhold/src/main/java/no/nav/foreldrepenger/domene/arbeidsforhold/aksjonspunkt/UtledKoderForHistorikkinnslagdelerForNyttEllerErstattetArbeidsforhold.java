package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

final class UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold {

    private UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold() {
        // Skjul default constructor
    }

    static Optional<VurderArbeidsforholdHistorikkinnslag> utled(ArbeidsforholdDto arbeidsforholdDto) {
        if (!StringUtils.isEmpty(arbeidsforholdDto.getErstatterArbeidsforholdId())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.SLÅTT_SAMMEN_MED_ANNET);
        }
        if (Boolean.TRUE.equals(arbeidsforholdDto.getErNyttArbeidsforhold())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.NYTT_ARBEIDSFORHOLD);
        }
        return Optional.empty();
    }

}
