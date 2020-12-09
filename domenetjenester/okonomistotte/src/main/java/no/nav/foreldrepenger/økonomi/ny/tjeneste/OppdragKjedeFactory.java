package no.nav.foreldrepenger.økonomi.ny.tjeneste;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.økonomi.ny.domene.DelytelseId;
import no.nav.foreldrepenger.økonomi.ny.domene.FagsystemId;
import no.nav.foreldrepenger.økonomi.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomi.ny.domene.OppdragKjedeFortsettelse;
import no.nav.foreldrepenger.økonomi.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelsePeriode;

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

    public OppdragKjedeFortsettelse lagOppdragskjede(OppdragKjede tidligereOppdrag, Ytelse vedtak, boolean gjelderFeriepenger) {
        Objects.requireNonNull(tidligereOppdrag);
        Objects.requireNonNull(vedtak);
        Ytelse iverksattYtelse = tidligereOppdrag.tilYtelse();
        boolean erNy = iverksattYtelse.getPerioder().isEmpty();
        LocalDate endringsdato = EndringsdatoTjeneste.finnEndringsdato(iverksattYtelse, vedtak);
        return endringsdato == null ? null : lagOppdragskjede(erNy, endringsdato, tidligereOppdrag, vedtak, gjelderFeriepenger);
    }

    private OppdragKjedeFortsettelse lagOppdragskjede(boolean erNy, LocalDate endringsdato, OppdragKjede tidligereOppdrag, Ytelse vedtak, boolean gjelderFeriepenger) {
        if (gjelderFeriepenger && vedtak.getPerioder().size() > 1) {
            throw new IllegalArgumentException("For feriepenger skal det være 0 eller 1 periode pr nøkkel (nøkkel inkl opptjeningsår), men fikk: " + vedtak.getPerioder().size() + " perioder");
        }

        OppdragKjedeFortsettelse.Builder builder = OppdragKjedeFortsettelse.builder(erNy, endringsdato);

        //for feriepenger sendes opphør kun når feriepengene skal tas bort.
        boolean erYtelseEllerOpphørAvFeriepenger = !gjelderFeriepenger || vedtak.getPerioder().isEmpty();
        if (erYtelseEllerOpphørAvFeriepenger && endringsdato != null && tidligereOppdrag.tilYtelse().harVerdiPåEllerEtter(endringsdato)) {
            LocalDate opphørsdato = tidligereOppdrag.getFørsteDato().isAfter(endringsdato) ? tidligereOppdrag.getFørsteDato() : endringsdato;
            builder.medOppdragslinje(OppdragLinje.lagOpphørslinje(tidligereOppdrag.getSisteLinje(), opphørsdato));
        }
        DelytelseId ref = !tidligereOppdrag.erTom() ? tidligereOppdrag.getSisteLinje().getDelytelseId() : null;
        for (YtelsePeriode ytelsePeriode : vedtak.getPerioderFraOgMed(endringsdato)) {
            DelytelseId delytelseId = lagDelytelseId();
            builder.medOppdragslinje(OppdragLinje.builder()
                .medYtelsePeriode(ytelsePeriode)
                .medDelytelseId(delytelseId)
                .medRefDelytelseId(ref)
                .build());
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
