package no.nav.foreldrepenger.domene.medlem;

import java.time.LocalDate;
import java.time.Period;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
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


    public LocalDateInterval bosattVurderingsintervall(BehandlingReferanse ref) {
        var referansedato = getReferansedato(ref);
        var maxdato = switch (ref.fagsakYtelseType()) {
            case ENGANGSTØNAD -> referansedato;
            case FORELDREPENGER, SVANGERSKAPSPENGER -> ref.getUttaksintervall().map(LocalDateInterval::getTomDato).orElse(referansedato);
            case null, default -> throw new IllegalArgumentException("Mangler ytelse");
        };
        var startdato = minDato(referansedato, LocalDate.now());
        return new LocalDateInterval(startdato.minus(BOSATT_TILBAKE_TID), maxdato);
    }

    public LocalDateInterval fortsattBosattVurderingsintervall(BehandlingReferanse ref) {
        var referansedato = getReferansedato(ref);
        var maxdato = switch (ref.fagsakYtelseType()) {
            case FORELDREPENGER, SVANGERSKAPSPENGER -> ref.getUttaksintervall().map(LocalDateInterval::getTomDato).orElse(referansedato);
            case null, default -> throw new IllegalArgumentException("Mangler ytelse");
        };
        return new LocalDateInterval(referansedato, maxdato);
    }

    public LocalDateInterval lovligOppholdVurderingsintervall(BehandlingReferanse ref) {
        var referansedato = getReferansedato(ref);
        var maxdato = switch (ref.fagsakYtelseType()) {
            case ENGANGSTØNAD -> referansedato;
            case FORELDREPENGER, SVANGERSKAPSPENGER -> ref.getUttaksintervall().map(LocalDateInterval::getTomDato).orElse(referansedato);
            case null, default -> throw new IllegalArgumentException("Mangler ytelse");
        };
        var startdato = FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType()) ? startLovligOppholdES(ref, referansedato) : referansedato;
        return new LocalDateInterval(startdato, maxdato);
    }

    private LocalDate getReferansedato(BehandlingReferanse ref) {
        return switch (ref.fagsakYtelseType()) {
            case ENGANGSTØNAD -> referansedatoES(ref);
            case FORELDREPENGER, SVANGERSKAPSPENGER -> ref.getUtledetSkjæringstidspunkt();
            case null, default -> throw new IllegalArgumentException("Mangler ytelse");
        };
    }

    private LocalDate referansedatoES(BehandlingReferanse ref) {
        if (botidCore2024.ikkeBotidskrav(ref.skjæringstidspunkt().getFamilieHendelseDato().orElse(null))) {
            return ref.getUtledetSkjæringstidspunkt();
        } else { // Default etter overgansperiode (8/2-25)
            return ref.skjæringstidspunkt().getFamilieHendelseDato().map(FamilieHendelseDato::termindato)
                .orElseGet(ref::getUtledetSkjæringstidspunkt);
        }
    }

    private LocalDate startLovligOppholdES(BehandlingReferanse ref, LocalDate referansedato) {
        if (botidCore2024.ikkeBotidskrav(ref.skjæringstidspunkt().getFamilieHendelseDato().orElse(null))) {
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
