package no.nav.foreldrepenger.mottak.vedtak.overlapp;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

/*
 * Tjeneste for å opprette samshandlingsoppgaver dersom nylig vedtak i fpsak overlapper andre ytelser
 * - Sykepenger i SPeil
 * - Pleiepenger i K9-sak
 * - Pleiepenger i Infotrygd
 * - Omsorgspenger i K9-sak (hovedsaklig om høsten)
 * - Opplæringspenger i K9-sak (behandles i Infotrygd inntil videre)
 * - FRISINN i K9-sak (venter ikke mye aktivitet)
 *
 * TBD
 * - Sykepenger i Infotrygd
 * - Ta inn ting fra VurderOmArenaYtelseSkalOpphøre (flytt fra vedtak til mottak)
 */
@ApplicationScoped
public class OverlappOppgaveTjeneste {

    private OppgaveTjeneste oppgaveTjeneste;


    @Inject
    public OverlappOppgaveTjeneste(OppgaveTjeneste oppgaveTjeneste) {
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    OverlappOppgaveTjeneste() {
        // for CDI
    }

    public void håndterOverlapp(List<OverlappVedtak> overlappListe, Behandling behandling) {
        var grupperteOverlapp = overlappListe.stream()
            .collect(Collectors.groupingBy(Gruppering::new));
        grupperteOverlapp.entrySet().stream()
            .filter(e -> OverlappVedtak.OverlappYtelseType.SP.equals(e.getKey().ytelseType()))
            .forEach(e -> håndterOverlappSykepenger(e.getKey(), e.getValue(), behandling));
        grupperteOverlapp.entrySet().stream()
            .filter(e -> OverlappVedtak.OverlappYtelseType.BS.equals(e.getKey().ytelseType()) || Fagsystem.K9SAK.equals(e.getKey().fagsystem()))
            .forEach(e -> håndterOverlappPleieOmsorg(e.getKey(), e.getValue(), behandling));
    }

    private void håndterOverlappSykepenger(Gruppering gruppering, List<OverlappVedtak> overlappListe, Behandling behandling) {
        if (overlappListe.isEmpty()) { // Utelat Infotrygd inntil det er avtalt
            return;
        }
        var system = Fagsystem.INFOTRYGD.equals(gruppering.fagsystem()) ? "Infotrygd" : "Speil";
        var minFom = overlappListe.stream()
            .map(periode -> periode.getPeriode().getFomDato())
            .min(Comparator.naturalOrder()).orElseThrow();
        var maxTom = overlappListe.stream()
            .map(periode -> periode.getPeriode().getTomDato())
            .max(Comparator.naturalOrder()).orElseThrow();
        var foreldrepengerYtelse = behandling.getFagsakYtelseType().getNavn().toLowerCase();
        var maxUtbetalingsprosent = overlappListe.stream()
            .map(OverlappVedtak::getFpsakUtbetalingsprosent)
            .max(Comparator.naturalOrder()).orElse(100L);

        // Beskrivelse må tilpasses dersom / når det skal opprettes oppgaver ved overlapp mot Infotrygd
        var beskrivelse = String.format("Det er innvilget %s (%s%%) som overlapper med sykepenger i periode %s - %s i %s. Vurder konsekvens for ytelse.",
            foreldrepengerYtelse, maxUtbetalingsprosent, minFom, maxTom, system );
        oppgaveTjeneste.opprettVurderKonsekvensHosSykepenger(beskrivelse, behandling.getAktørId());

    }

    private void håndterOverlappPleieOmsorg(Gruppering gruppering, List<OverlappVedtak> overlappListe, Behandling behandling) {
        if (overlappListe.isEmpty()) {
            return;
        }
        var minFom = overlappListe.stream()
            .map(periode -> periode.getPeriode().getFomDato())
            .min(Comparator.naturalOrder()).orElseThrow();
        var maxTom = overlappListe.stream()
            .map(periode -> periode.getPeriode().getTomDato())
            .max(Comparator.naturalOrder()).orElseThrow();
        var foreldrepengerYtelse = behandling.getFagsakYtelseType().getNavn().toLowerCase();
        var maxUtbetalingsprosent = overlappListe.stream()
            .map(OverlappVedtak::getFpsakUtbetalingsprosent)
            .max(Comparator.naturalOrder()).orElse(100L);
        var omsorgspengerYtelse = omsorgspengerYtelse(gruppering);

        if (Fagsystem.K9SAK.equals(gruppering.fagsystem())) {
            var beskrivelse = String.format("Det er innvilget %s (%s%%) som overlapper med %s sak %s i periode %s - %s i K9-sak. Vurder konsekvens for ytelse.",
                    foreldrepengerYtelse, maxUtbetalingsprosent, omsorgspengerYtelse, gruppering.saksnummer(), minFom, maxTom );
            oppgaveTjeneste.opprettVurderKonsekvensHosPleiepenger(beskrivelse, behandling.getAktørId());
        } else {
            var beskrivelse = String.format("Det er innvilget %s (%s%%) som overlapper med pleiepenger i periode %s - %s i Infotrygd. Vurder konsekvens for ytelse.",
                    foreldrepengerYtelse, maxUtbetalingsprosent, minFom, maxTom );
            oppgaveTjeneste.opprettVurderKonsekvensHosPleiepenger(beskrivelse, behandling.getAktørId());
        }
    }

    private static String omsorgspengerYtelse(Gruppering gruppering) {
        return switch (gruppering.ytelseType()) {
            case SP -> "sykepenger";
            case BS -> "pleiepenger";
            case PLEIEPENGER, OMSORGSPENGER, OPPLÆRINGSPENGER, FRISINN -> gruppering.ytelseType().name().toLowerCase();
        };
    }

    private record Gruppering(Fagsystem fagsystem, OverlappVedtak.OverlappYtelseType ytelseType, String saksnummer) {
        Gruppering(OverlappVedtak vedtak) {
            this(vedtak.getFagsystem(), vedtak.getYtelse(), Fagsystem.K9SAK.equals(vedtak.getFagsystem()) ? vedtak.getReferanse() : "");
        }

    }

}
