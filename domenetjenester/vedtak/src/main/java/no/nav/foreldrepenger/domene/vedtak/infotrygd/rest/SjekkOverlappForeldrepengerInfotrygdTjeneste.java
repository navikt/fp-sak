package no.nav.foreldrepenger.domene.vedtak.infotrygd.rest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.StatusKode;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class SjekkOverlappForeldrepengerInfotrygdTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SjekkOverlappForeldrepengerInfotrygdTjeneste.class);


    private InfotrygdFPGrunnlag tjeneste ;
    private AktørConsumer aktørConsumer;

    SjekkOverlappForeldrepengerInfotrygdTjeneste() {
        // CDI
    }

    @Inject
    public SjekkOverlappForeldrepengerInfotrygdTjeneste(InfotrygdFPGrunnlag tjeneste, AktørConsumer aktørConsumer) {
        this.tjeneste = tjeneste;
        this.aktørConsumer = aktørConsumer;
    }

    public boolean harForeldrepengerInfotrygdSomOverlapper(AktørId aktørId, LocalDate vedtakDato) {
        var ident = getFnrFraAktørId(aktørId);
        List<Grunnlag> rest = tjeneste.hentGrunnlag(ident.getIdent(), vedtakDato.minusWeeks(1), vedtakDato.plusYears(3));

        var mappedGrunnlag = rest.stream()
            .filter(g -> overlapper(g, vedtakDato))
            .collect(Collectors.toList());

        return !mappedGrunnlag.isEmpty();
    }

    private boolean overlapper(Grunnlag grunnlag, LocalDate vedtakDato) {
        LocalDate maxDato = finnMaxDato(grunnlag, vedtakDato);
        if (!maxDato.isBefore(vedtakDato)) {
            LOG.info("Overlapp INFOTRYGD: fødselsdato barn: {} opphørsdato fra INFOTRYGD: {} Startdato ny sak: {}", grunnlag.getFødselsdatoBarn(), maxDato, vedtakDato);
        } else {
            LOG.info("Uten Overlapp INFOTRYGD: fødselsdato barn: {} opphørsdato fra INFOTRYGD: {} Startdato ny sak: {}", grunnlag.getFødselsdatoBarn(), maxDato, vedtakDato);
        }
        return !maxDato.isBefore(vedtakDato);
    }

    private LocalDate finnMaxDato(Grunnlag grunnlag, LocalDate vedtakDato) {
        if (grunnlag.getOpphørFom() != null) {
            return localDateMinus1Virkedag(grunnlag.getOpphørFom());
        }
        // Ignoreres og gir ikke overlapp
        if (grunnlag.getStatus() == null || StatusKode.UKJENT.equals(grunnlag.getStatus().getKode())) {
            LOG.info("Sjekk overlapp INFOTRYGD: ukjent status");
            return Tid.TIDENES_BEGYNNELSE;
        }
        // Ikke startet eller Løpende - vet ikke om det kan komme flere vedtak - kan ikke se på utbetalt til nå.
        if (Set.of(StatusKode.I, StatusKode.L).contains(grunnlag.getStatus().getKode())) {
            LOG.info("Overlapp INFOTRYGD: status i IT {} gjør at vi ikke vet om det kommer flere utbetalinger ", grunnlag.getStatus());
            return Tid.TIDENES_ENDE;
        }
        // Status Avsluttet, opphørFom ikke satt
        return grunnlag.getPeriode() == null || grunnlag.getPeriode().getTom() == null ? Tid.TIDENES_BEGYNNELSE : grunnlag.getPeriode().getTom();
    }

    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new).orElseThrow();
    }

    private LocalDate localDateMinus1Virkedag(LocalDate opphoerFomDato) {
        LocalDate dato = opphoerFomDato.minusDays(1);
        if (dato.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue()) {
            dato = opphoerFomDato.minusDays(1L + dato.getDayOfWeek().getValue() - DayOfWeek.FRIDAY.getValue());
        }
        return dato;
    }

}
