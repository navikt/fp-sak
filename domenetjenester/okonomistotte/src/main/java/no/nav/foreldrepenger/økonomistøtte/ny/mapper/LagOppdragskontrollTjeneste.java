package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

class LagOppdragskontrollTjeneste {

    static Oppdragskontroll hentEllerOpprettOppdragskontroll(OppdragInput input, Oppdragskontroll oppdragskontrollFraFør) {
        if (oppdragskontrollFraFør != null) {
            oppdragskontrollFraFør.setVenterKvittering(Boolean.TRUE);
            return oppdragskontrollFraFør;
        }
        return Oppdragskontroll.builder()
            .medSaksnummer(input.getSaksnummer())
            .medBehandlingId(input.getBehandlingId())
            .medVenterKvittering(Boolean.TRUE)
            .medProsessTaskId(input.getProsessTaskId())
            .build();
    }
}
