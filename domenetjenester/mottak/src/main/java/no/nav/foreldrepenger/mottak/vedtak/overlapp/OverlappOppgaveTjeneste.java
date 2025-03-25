package no.nav.foreldrepenger.mottak.vedtak.overlapp;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

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

    private static final String SYKEPENGER = "sykepenger";

    private OppgaveTjeneste oppgaveTjeneste;


    @Inject
    public OverlappOppgaveTjeneste(OppgaveTjeneste oppgaveTjeneste) {
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    OverlappOppgaveTjeneste() {
        // for CDI
    }

    public void håndterOverlapp(List<OverlappVedtak> overlappListe, BehandlingReferanse ref, LocalDateTimeline<BigDecimal> tidslineFpsak) {
        if (overlappListe.isEmpty()) {
            return;
        }
        var fpsakIntervall = new LocalDateInterval(tidslineFpsak.getMinLocalDate(), tidslineFpsak.getMaxLocalDate());
        var grupperteOverlapp = overlappListe.stream()
            .collect(Collectors.groupingBy(Gruppering::new));
        grupperteOverlapp.entrySet().stream()
            .filter(e -> OverlappVedtak.OverlappYtelseType.SP.equals(e.getKey().ytelseType()))
            .forEach(e -> håndterOverlappSykepenger(e.getKey(), e.getValue(), ref, fpsakIntervall));
        grupperteOverlapp.entrySet().stream()
            .filter(e -> OverlappVedtak.OverlappYtelseType.BS.equals(e.getKey().ytelseType()) || Fagsystem.K9SAK.equals(e.getKey().fagsystem()))
            .forEach(e -> håndterOverlappPleieOmsorg(e.getKey(), e.getValue(), ref, fpsakIntervall));
    }

    private void håndterOverlappSykepenger(Gruppering gruppering, List<OverlappVedtak> overlappListe, BehandlingReferanse ref, LocalDateInterval fpsakIntervall) {
        var system = Fagsystem.INFOTRYGD.equals(gruppering.fagsystem()) ? "Infotrygd" : "Speil";
        // Beskrivelse må tilpasses dersom / når det skal opprettes oppgaver ved overlapp mot Infotrygd
        var beskrivelse2 = lagBeskrivelseAnnenYtelse(ref, SYKEPENGER, system, overlappListe);
        var beskrivelse = lagSamletBeskrivelse(ref, fpsakIntervall, beskrivelse2, SYKEPENGER);
        oppgaveTjeneste.opprettVurderKonsekvensHosSykepenger(beskrivelse, ref.aktørId());

    }

    private void håndterOverlappPleieOmsorg(Gruppering gruppering, List<OverlappVedtak> overlappListe, BehandlingReferanse ref, LocalDateInterval fpsakIntervall) {
        var omsorgspengerYtelse = omsorgspengerYtelse(gruppering);

        var beskrivelse2 = Fagsystem.K9SAK.equals(gruppering.fagsystem())
            ? lagBeskrivelseAnnenYtelse(ref, omsorgspengerYtelse + " sak " + gruppering.saksnummer(), "K9-sak", overlappListe)
            : lagBeskrivelseAnnenYtelse(ref, omsorgspengerYtelse, "Infotrygd", overlappListe);

        var beskrivelse = lagSamletBeskrivelse(ref, fpsakIntervall, beskrivelse2, omsorgspengerYtelse);
        oppgaveTjeneste.opprettVurderKonsekvensHosPleiepenger(beskrivelse, ref.aktørId());
    }

    private static String lagSamletBeskrivelse(BehandlingReferanse ref, LocalDateInterval fpsakIntervall,
                                               String beskrivelse2, String ytelse) {
        var beskrivelse1 = lagBeskrivelseVedtakForeldrepenger(ref, fpsakIntervall);
        var beskrivelse3 = lagBeskrivelseVeiledning(ytelse);
        return beskrivelse1 + System.lineSeparator() + beskrivelse2 + System.lineSeparator() + beskrivelse3;
    }


    private static String lagBeskrivelseVedtakForeldrepenger(BehandlingReferanse ref, LocalDateInterval fpsakIntervall) {
        var foreldrepengerYtelse = ref.fagsakYtelseType().getNavn().toLowerCase();
        return String.format("Denne oppgaven kommer fordi det er innvilget %s fra %s til %s med saksnummer %s.",
            foreldrepengerYtelse, d2(fpsakIntervall.getFomDato()), d2(fpsakIntervall.getTomDato()), ref.saksnummer().getVerdi());
    }

    private static String lagBeskrivelseAnnenYtelse(BehandlingReferanse ref, String ytelse, String system, List<OverlappVedtak> overlappListe) {
        var foreldrepengerYtelse = ref.fagsakYtelseType().getNavn().toLowerCase();
        if (overlappListe.size() == 1) {
            var tekst = tekstForEnkeltPeriode(overlappListe.getFirst(), foreldrepengerYtelse);
            return String.format("Dette vedtaket overlapper antagelig med %s i %s i perioden %s.", ytelse, system, tekst);
        } else if (overlappListe.size() == 2 || overlappListe.size() == 3) {
            var perioder = overlappListe.stream()
                .map(p -> tekstForEnkeltPeriode(p, foreldrepengerYtelse))
                .toList();
            return String.format("Dette vedtaket overlapper antagelig med %s i %s i periodene %s.", ytelse, system, String.join(", ", perioder));
        } else {
            var minFom = overlappListe.stream().map(p -> p.getPeriode().getFomDato()).min(Comparator.naturalOrder()).orElseThrow();
            var maxTom = overlappListe.stream().map(p -> p.getPeriode().getTomDato()).max(Comparator.naturalOrder()).orElseThrow();

            return String.format("Dette vedtaket overlapper antagelig med %s i %s i perioden fra %s til %s.", ytelse, system, d2(minFom), d2(maxTom));
        }
    }

    private static String tekstForEnkeltPeriode (OverlappVedtak overlapp, String foreldrepengerYtelse) {
        return String.format("%s til %s med %s%% %s", d2(overlapp.getPeriode().getFomDato()), d2(overlapp.getPeriode().getTomDato()),
            overlapp.getFpsakUtbetalingsprosent(), foreldrepengerYtelse);
    }

    private static String d2(LocalDate dato) {
        return dato.format(DateTimeFormatter.ofPattern("d.M.yy"));
    }

    private static String lagBeskrivelseVeiledning(String ytelse) {
        return String.format("Det må undersøkes om det er overlapp mellom ytelsene og om %s skal endres. Er du i tvil må du ta kontakt med NFP.", ytelse);
    }



    private static String omsorgspengerYtelse(Gruppering gruppering) {
        return switch (gruppering.ytelseType()) {
            case SP -> SYKEPENGER;
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
