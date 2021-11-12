package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdFPGrunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Periode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.StatusKode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Vedtak;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class OverlappFPInfotrygdTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OverlappFPInfotrygdTjeneste.class);

    private InfotrygdFPGrunnlag tjeneste;
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    public OverlappFPInfotrygdTjeneste(InfotrygdFPGrunnlag tjeneste, PersoninfoAdapter personinfoAdapter) {
        this.tjeneste = tjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    OverlappFPInfotrygdTjeneste() {
        // CDI
    }

    public boolean harForeldrepengerInfotrygdSomOverlapper(AktørId aktørId, LocalDate vedtakDato) {
        var ident = getFnrFraAktørId(aktørId);
        var rest = tjeneste.hentGrunnlag(ident.getIdent(), vedtakDato.minusWeeks(1), vedtakDato.plusYears(3));

        return rest.stream().anyMatch(g -> overlapper(g, vedtakDato));
    }

    private boolean overlapper(Grunnlag grunnlag, LocalDate vedtakDato) {
        var maxDato = VirkedagUtil.tomVirkedag(finnMaxDatoUtbetaling(grunnlag).orElse(finnMaxDato(grunnlag)));
        if (!maxDato.isBefore(vedtakDato)) {
            LOG.info("Overlapp INFOTRYGD: fødselsdato barn: {} opphørsdato fra INFOTRYGD: {} Startdato ny sak: {}",
                grunnlag.getFødselsdatoBarn(), maxDato, vedtakDato);
        } else {
            LOG.info("Uten Overlapp INFOTRYGD: fødselsdato barn: {} opphørsdato fra INFOTRYGD: {} Startdato ny sak: {}",
                grunnlag.getFødselsdatoBarn(), maxDato, vedtakDato);
        }
        return !maxDato.isBefore(vedtakDato);
    }

    private Optional<LocalDate> finnMaxDatoUtbetaling(Grunnlag grunnlag) {
        return grunnlag.getVedtak()
            .stream()
            .filter(this::harUtbetaling)
            .map(Vedtak::periode)
            .map(Periode::tom)
            .max(Comparator.naturalOrder())
            .map(VirkedagUtil::tomVirkedag);
    }

    private LocalDate finnMaxDato(Grunnlag grunnlag) {
        if (grunnlag.getOpphørFom() != null) {
            return localDateMinus1Virkedag(grunnlag.getOpphørFom());
        }
        // Ignoreres og gir ikke overlapp
        if (grunnlag.getStatus() == null || StatusKode.UKJENT.equals(grunnlag.getStatus().kode())) {
            LOG.info("Sjekk overlapp INFOTRYGD: ukjent status");
            return Tid.TIDENES_BEGYNNELSE;
        }
        // Ikke startet eller Løpende - vet ikke om det kan komme flere vedtak - kan ikke se på utbetalt til nå.
        if (Set.of(StatusKode.I, StatusKode.L).contains(grunnlag.getStatus().kode())) {
            LOG.info("Overlapp INFOTRYGD: status i IT {} gjør at vi ikke vet om det kommer flere utbetalinger ",
                grunnlag.getStatus());
            return Tid.TIDENES_ENDE;
        }
        // Status Avsluttet, opphørFom ikke satt
        return grunnlag.getPeriode() == null
            || grunnlag.getPeriode().tom() == null ? Tid.TIDENES_BEGYNNELSE : grunnlag.getPeriode().tom();
    }

    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return personinfoAdapter.hentFnr(aktørId).orElseThrow();
    }

    private boolean harUtbetaling(Vedtak v) {
        return v.utbetalingsgrad() > 0;
    }

    private LocalDate localDateMinus1Virkedag(LocalDate opphoerFomDato) {
        return VirkedagUtil.tomVirkedag(opphoerFomDato.minusDays(1));
    }
}
