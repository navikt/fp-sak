package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.util.Optional;

import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

final class UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon {

    private UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon() {
        // Skjul default constructor
    }

    static Optional<VurderArbeidsforholdHistorikkinnslag> utled(ArbeidsforholdDto arbeidsforholdDto) {
        if (Boolean.TRUE.equals(arbeidsforholdDto.getBrukPermisjon())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON);
        }
        if (Boolean.FALSE.equals(arbeidsforholdDto.getBrukPermisjon())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON);
        }
        return Optional.empty();
    }

}
