package no.nav.foreldrepenger.økonomistøtte.dagytelse.førstegangsoppdrag;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110.OpprettOppdrag110Tjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragslinje150Tjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;

@ApplicationScoped
public class OppdragskontrollFørstegang implements OppdragskontrollManager {

    private static final long INITIAL_LØPENUMMER = 99L;

    public OppdragskontrollFørstegang() {}

    @Override
    public Oppdragskontroll opprettØkonomiOppdrag(OppdragInput input, Oppdragskontroll oppdragskontroll) {
        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> andelPrMottakerMap = OpprettMottakereMapFørstegangsoppdrag.finnMottakereMedDeresAndel(input);
        if (andelPrMottakerMap.isEmpty()) {
            throw new IllegalStateException("Finner ingen oppdragsmottakere i behandling " + input.getBehandlingId());
        }
        long initialLøpenummer = INITIAL_LØPENUMMER;
        for (Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry : andelPrMottakerMap.entrySet()) {
            Oppdragsmottaker mottaker = entry.getKey();
            long fagsystemId = OpprettOppdrag110Tjeneste.settFagsystemId(input.getSaksnummer(), initialLøpenummer, false);
            Oppdrag110 oppdrag110 = OpprettOppdrag110Tjeneste.opprettNyOppdrag110(input, oppdragskontroll, mottaker, fagsystemId);
            List<TilkjentYtelseAndel> andelListe = entry.getValue();
            OpprettOppdragslinje150Tjeneste.opprettOppdragslinje150(input, oppdrag110, andelListe, mottaker);
            initialLøpenummer++;
        }
        return oppdragskontroll;
    }
}
