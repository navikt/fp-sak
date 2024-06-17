package no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.FagsystemId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragKjedeFortsettelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;

public class OppdragKjedeFactory {

    private DelytelseId nesteDelytelseId;

    public static OppdragKjedeFactory lagForEksisterendeMottaker(DelytelseId høyesteEksisterendeDelytelseId) {
        return new OppdragKjedeFactory(høyesteEksisterendeDelytelseId.neste());
    }

    public static OppdragKjedeFactory lagForNyMottaker(FagsystemId fagsystemId) {
        return new OppdragKjedeFactory(DelytelseId.førsteForFagsystemId(fagsystemId));
    }

    private OppdragKjedeFactory(DelytelseId nesteDelytelseId) {
        this.nesteDelytelseId = nesteDelytelseId;
    }

    OppdragKjedeFortsettelse lagOppdragskjedeForYtelse(OppdragKjede tidligereOppdrag, Ytelse vedtak) {
        return lagOppdragskjede(tidligereOppdrag, vedtak, false);
    }

    OppdragKjedeFortsettelse lagOppdragskjedeForFeriepenger(OppdragKjede tidligereOppdrag, Ytelse vedtak) {
        return lagOppdragskjede(tidligereOppdrag, vedtak, true);
    }

    public OppdragKjedeFortsettelse lagOppdragskjede(OppdragKjede tidligereOppdrag, Ytelse vedtak, boolean gjelderEngangsutbetaling) {
        Objects.requireNonNull(tidligereOppdrag);
        Objects.requireNonNull(vedtak);
        var iverksattYtelse = tidligereOppdrag.tilYtelse();
        var endringsdato = EndringsdatoTjeneste.normal().finnEndringsdato(iverksattYtelse, vedtak);
        return endringsdato == null ? null : lagOppdragskjede(endringsdato, tidligereOppdrag, vedtak, gjelderEngangsutbetaling);
    }

    public OppdragKjedeFortsettelse lagOppdragskjedeFraFellesEndringsdato(OppdragKjede tidligereOppdrag,
                                                                          Ytelse vedtak,
                                                                          boolean gjelderEngangsutbetaling,
                                                                          LocalDate tidligsteEndringsdato) {
        Objects.requireNonNull(tidligereOppdrag);
        Objects.requireNonNull(vedtak);
        Objects.requireNonNull(tidligsteEndringsdato);
        var iverksattYtelse = tidligereOppdrag.tilYtelse();
        var endringsdato = EndringsdatoTjeneste.normal().finnEndringsdato(iverksattYtelse, vedtak);
        if (endringsdato != null && endringsdato.isBefore(tidligsteEndringsdato)) {
            throw new IllegalArgumentException("Endringsdato for kjeden er før felles endringsdato");
        }
        if (vedtak.getPerioderFraOgMed(tidligsteEndringsdato).isEmpty() && !tidligereOppdrag.tilYtelse()
            .harVerdiPåEllerEtter(tidligsteEndringsdato)) {
            return null;
        }
        return lagOppdragskjede(tidligsteEndringsdato, tidligereOppdrag, vedtak, gjelderEngangsutbetaling);
    }

    private OppdragKjedeFortsettelse lagOppdragskjede(LocalDate endringsdato,
                                                      OppdragKjede tidligereOppdrag,
                                                      Ytelse vedtak,
                                                      boolean gjelderEngangsutbetaling) {
        if (gjelderEngangsutbetaling && vedtak.getPerioder().size() > 1) {
            throw new IllegalArgumentException(
                "For feriepenger/engangsstønad skal det være 0 eller 1 periode pr nøkkel (nøkkel inkl opptjeningsår), men fikk: "
                    + vedtak.getPerioder().size() + " perioder");
        }

        var builder = OppdragKjedeFortsettelse.builder(endringsdato);

        //for engangsutbetalinger sendes opphør kun når ytelsen skal tas bort.
        var erYtelseEllerOpphørAvFeriepenger = !gjelderEngangsutbetaling || vedtak.getPerioder().isEmpty();
        if (erYtelseEllerOpphørAvFeriepenger && endringsdato != null && tidligereOppdrag.tilYtelse().harVerdiPåEllerEtter(endringsdato)) {
            var opphørsdato = tidligereOppdrag.getFørsteDato().isAfter(endringsdato) ? tidligereOppdrag.getFørsteDato() : endringsdato;
            builder.medOppdragslinje(OppdragLinje.lagOpphørslinje(tidligereOppdrag.getSisteLinje(), opphørsdato));
        }
        var ref = !tidligereOppdrag.erTom() ? tidligereOppdrag.getSisteLinje().getDelytelseId() : null;
        for (var ytelsePeriode : vedtak.getPerioderFraOgMed(endringsdato)) {
            var delytelseId = lagDelytelseId();
            builder.medOppdragslinje(
                OppdragLinje.builder().medYtelsePeriode(ytelsePeriode).medDelytelseId(delytelseId).medRefDelytelseId(ref).build());
            ref = delytelseId;
        }

        return builder.build();
    }

    private DelytelseId lagDelytelseId() {
        try {
            return nesteDelytelseId;
        } finally {
            nesteDelytelseId = nesteDelytelseId.neste();
        }
    }
}
