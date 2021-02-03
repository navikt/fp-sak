package no.nav.foreldrepenger.ny.tjeneste;

import java.time.LocalDate;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.ny.domene.FagsystemId;
import no.nav.foreldrepenger.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.ny.domene.Oppdrag;
import no.nav.foreldrepenger.ny.domene.OppdragKjedeFortsettelse;
import no.nav.foreldrepenger.ny.domene.Ytelse;
import no.nav.foreldrepenger.ny.domene.samlinger.MottakerOppdragKjedeOversikt;
import no.nav.foreldrepenger.ny.util.SetUtil;

public class OppdragForMottakerTjeneste {

    private final ØkonomiKodeFagområde økonomiFagområde;
    private final FagsystemId fagsystemId;
    private final Betalingsmottaker betalingsmottaker;
    private final LocalDate fellesEndringstidspunkt;

    public OppdragForMottakerTjeneste(ØkonomiKodeFagområde økonomiFagområde, FagsystemId fagsystemId, Betalingsmottaker betalingsmottaker, LocalDate fellesEndringstidspunkt) {
        this.økonomiFagområde = økonomiFagområde;
        this.fagsystemId = fagsystemId;
        this.betalingsmottaker = betalingsmottaker;
        this.fellesEndringstidspunkt = fellesEndringstidspunkt;
    }

    public Oppdrag lagOppdrag(MottakerOppdragKjedeOversikt eksisterendeOppdrag, Map<KjedeNøkkel, Ytelse> nyTilkjentYtelse) {
        OppdragKjedeFactory factory = lagOppdragskjedeFactory(eksisterendeOppdrag);
        Oppdrag.Builder builder = Oppdrag.builder(økonomiFagområde, fagsystemId, betalingsmottaker);
        for (KjedeNøkkel nøkkel : SetUtil.sortertUnionOfKeys(eksisterendeOppdrag.getKjeder(), nyTilkjentYtelse)) {
            OppdragKjedeFortsettelse kjede = fellesEndringstidspunkt == null
                ? factory.lagOppdragskjede(eksisterendeOppdrag.getKjede(nøkkel), nyTilkjentYtelse.getOrDefault(nøkkel, Ytelse.EMPTY), nøkkel.gjelderFeriepenger())
                : factory.lagOppdragskjedeFraFellesEndringsdato(eksisterendeOppdrag.getKjede(nøkkel), nyTilkjentYtelse.getOrDefault(nøkkel, Ytelse.EMPTY), nøkkel.gjelderFeriepenger(), fellesEndringstidspunkt);
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
