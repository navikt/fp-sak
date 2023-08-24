package no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.*;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.MottakerOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.util.SetUtil;

import java.time.LocalDate;
import java.util.Map;

public class OppdragForMottakerTjeneste {

    private final KodeFagområde økonomiFagområde;
    private final FagsystemId fagsystemId;
    private final Betalingsmottaker betalingsmottaker;
    private final LocalDate fellesEndringstidspunkt;

    public OppdragForMottakerTjeneste(KodeFagområde økonomiFagområde, FagsystemId fagsystemId, Betalingsmottaker betalingsmottaker, LocalDate fellesEndringstidspunkt) {
        this.økonomiFagområde = økonomiFagområde;
        this.fagsystemId = fagsystemId;
        this.betalingsmottaker = betalingsmottaker;
        this.fellesEndringstidspunkt = fellesEndringstidspunkt;
    }

    public Oppdrag lagOppdrag(MottakerOppdragKjedeOversikt eksisterendeOppdrag, Map<KjedeNøkkel, Ytelse> nyTilkjentYtelse) {
        var factory = lagOppdragskjedeFactory(eksisterendeOppdrag);
        var builder = Oppdrag.builder(økonomiFagområde, fagsystemId, betalingsmottaker);
        for (var nøkkel : SetUtil.sortertUnionOfKeys(eksisterendeOppdrag.getKjeder(), nyTilkjentYtelse)) {
            var kjede = fellesEndringstidspunkt == null
                ? factory.lagOppdragskjede(eksisterendeOppdrag.getKjede(nøkkel), nyTilkjentYtelse.getOrDefault(nøkkel, Ytelse.EMPTY), nøkkel.gjelderEngangsutbetaling())
                : factory.lagOppdragskjedeFraFellesEndringsdato(eksisterendeOppdrag.getKjede(nøkkel), nyTilkjentYtelse.getOrDefault(nøkkel, Ytelse.EMPTY), nøkkel.gjelderEngangsutbetaling(), fellesEndringstidspunkt);
            if (kjede != null) {
                builder.leggTil(nøkkel, kjede);
            }
        }
        return builder.build();
    }

    private OppdragKjedeFactory lagOppdragskjedeFactory(MottakerOppdragKjedeOversikt eksisterendeOppdrag) {
        return eksisterendeOppdrag.isEmpty()
            ? OppdragKjedeFactory.lagForNyMottaker(fagsystemId)
            : OppdragKjedeFactory.lagForEksisterendeMottaker(eksisterendeOppdrag.finnHøyesteDelytelseId());
    }

}
