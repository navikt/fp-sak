package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

class LagOppdragskontrollTjeneste {

    private LagOppdragskontrollTjeneste() {
    }

    static Oppdragskontroll hentEllerOpprettOppdragskontroll(OppdragInput input, Oppdragskontroll oppdragskontrollFraFør) {
        if (oppdragskontrollFraFør != null) {
            oppdragskontrollFraFør.setVenterKvittering(true);
            oppdragskontrollFraFør.setProsessTaskId(input.getProsessTaskId());
            return oppdragskontrollFraFør;
        }
        return Oppdragskontroll.builder()
            .medSaksnummer(input.getSaksnummer())
            .medBehandlingId(input.getBehandlingId())
            .medVenterKvittering(true)
            .medProsessTaskId(input.getProsessTaskId())
            .build();
    }
}
