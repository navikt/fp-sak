package no.nav.foreldrepenger.økonomistøtte;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class FastsettOppdragskontroll {
    private FastsettOppdragskontroll() {
        // skjul default constructor
    }

    public static Oppdragskontroll finnEllerOpprett(List<Oppdragskontroll> tidligereOppdragListe, Long behandlingId,
                                             Long prosessTaskId, Saksnummer saksnummer) {
        Optional<Oppdragskontroll> oppdragskontrollForBehandlingOpt = tidligereOppdragListe.stream()
            .filter(oppdragskontroll -> behandlingId.equals(oppdragskontroll.getBehandlingId()))
            .findFirst();
        if (oppdragskontrollForBehandlingOpt.isPresent()) {
            Oppdragskontroll oppdragskontroll = oppdragskontrollForBehandlingOpt.get();
            oppdragskontroll.setVenterKvittering(Boolean.TRUE);
            return oppdragskontroll;
        }
        return opprettOppdragskontroll(prosessTaskId, behandlingId, saksnummer);
    }

    private static Oppdragskontroll opprettOppdragskontroll(Long prosessTaskId, Long behandlingId, Saksnummer saksnummer) {
        return Oppdragskontroll.builder()
            .medSaksnummer(saksnummer)
            .medBehandlingId(behandlingId)
            .medVenterKvittering(Boolean.TRUE)
            .medProsessTaskId(prosessTaskId)
            .build();
    }
}
