package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;

public interface SkjæringstidspunktRegisterinnhentingTjeneste {

    /**
     * Skjæringstidspunkt som benyttes for registerinnhenting
     *
     * @param behandlingId behandling ID
     * @return datoen
     */
    LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId);

}
