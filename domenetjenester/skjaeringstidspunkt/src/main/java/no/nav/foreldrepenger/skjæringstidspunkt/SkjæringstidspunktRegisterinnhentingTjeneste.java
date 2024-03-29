package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;

public interface SkjæringstidspunktRegisterinnhentingTjeneste {

    /**
     * Skjæringstidspunkt som benyttes for registerinnhenting
     *
     * @param behandlingId behandling ID
     * @return datoen
     */
    LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId);

    default SimpleLocalDateInterval vurderOverstyrtStartdatoForRegisterInnhenting(Long behandlingId, SimpleLocalDateInterval intervall) {
        return intervall;
    }

}
