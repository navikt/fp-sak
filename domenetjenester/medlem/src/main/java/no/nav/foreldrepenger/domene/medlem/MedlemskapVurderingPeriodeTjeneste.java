package no.nav.foreldrepenger.domene.medlem;

import java.time.LocalDate;
import java.time.Period;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.es.BotidCore2024;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class MedlemskapVurderingPeriodeTjeneste {

    private static final Period BOSATT_TILBAKE_TID = Period.ofMonths(12);
    private static final Period MEDLEMSKAP_ES = Period.ofMonths(12);

    private BotidCore2024 botidCore2024;
    MedlemskapVurderingPeriodeTjeneste() {
        // CDI
    }

    @Inject
    public MedlemskapVurderingPeriodeTjeneste(BotidCore2024 botidCore2024) {
        this.botidCore2024 = botidCore2024;
    }


    public LocalDateInterval bosattVurderingsintervall(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var referansedato = getReferansedato(ref, stp);
        var maxdato = switch (ref.fagsakYtelseType()) {
            case ENGANGSTØNAD -> referansedato;
            case FORELDREPENGER, SVANGERSKAPSPENGER -> stp.getUttaksintervall().map(LocalDateInterval::getTomDato).orElse(referansedato);
            case null, default -> throw new IllegalArgumentException("Mangler ytelse");
        };
        var startdato = minDato(referansedato, LocalDate.now());
        return new LocalDateInterval(startdato.minus(BOSATT_TILBAKE_TID), maxdato);
    }

    public LocalDateInterval fortsattBosattVurderingsintervall(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var referansedato = getReferansedato(ref, stp);
        var maxdato = switch (ref.fagsakYtelseType()) {
            case FORELDREPENGER, SVANGERSKAPSPENGER -> stp.getUttaksintervall().map(LocalDateInterval::getTomDato).orElse(referansedato);
            case null, default -> throw new IllegalArgumentException("Mangler ytelse");
        };
        return new LocalDateInterval(referansedato, maxdato);
    }

    public LocalDateInterval lovligOppholdVurderingsintervall(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var referansedato = getReferansedato(ref, stp);
        var maxdato = switch (ref.fagsakYtelseType()) {
            case ENGANGSTØNAD -> referansedato;
            case FORELDREPENGER, SVANGERSKAPSPENGER -> stp.getUttaksintervall().map(LocalDateInterval::getTomDato).orElse(referansedato);
            case null, default -> throw new IllegalArgumentException("Mangler ytelse");
        };
        var startdato = FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType()) ? startLovligOppholdES(stp, referansedato) : referansedato;
        return new LocalDateInterval(startdato, maxdato);
    }

    private LocalDate getReferansedato(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return switch (ref.fagsakYtelseType()) {
            case ENGANGSTØNAD -> referansedatoES(stp);
            case FORELDREPENGER, SVANGERSKAPSPENGER -> stp.getUtledetSkjæringstidspunkt();
            case null, default -> throw new IllegalArgumentException("Mangler ytelse");
        };
    }

    private LocalDate referansedatoES(Skjæringstidspunkt stp) {
        if (botidCore2024.ikkeBotidskrav(stp.getFamilieHendelseDato().orElse(null))) {
            return stp.getUtledetSkjæringstidspunkt();
        } else { // Default etter overgansperiode (8/2-25)
            return stp.getFamilieHendelseDato().map(FamilieHendelseDato::termindato)
                .orElseGet(stp::getUtledetSkjæringstidspunkt);
        }
    }

    private LocalDate startLovligOppholdES(Skjæringstidspunkt stp, LocalDate referansedato) {
        if (botidCore2024.ikkeBotidskrav(stp.getFamilieHendelseDato().orElse(null))) {
            return referansedato;
        } else { // Default etter overgansperiode (8/2-25)
            return minDato(referansedato, LocalDate.now()).minus(MEDLEMSKAP_ES);
        }
    }

    private LocalDate minDato(LocalDate dato1, LocalDate dato2) {
        if (dato1 != null && dato2 != null) {
            return dato1.isBefore(dato2) ? dato1 : dato2;
        } else {
            return dato1 != null ? dato1 : dato2;
        }
    }

}
