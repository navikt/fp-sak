package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.StønadsperiodeTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.konfig.Tid;

/**
 * Tjenesten sjekker om det finnes løpende saker for den personen det innvilges foreldrepenger eller svangerskapsper på.
 * Om det finnes løpende saker sjekkes det om ny sak overlapper med løpende sak. Det sjekkes både for mor, far og en
 * eventuell medforelder på foreldrepenger. Saker som overlapper annen parts sak på samme barn(saker som er koblet) håndteres ikke her.
 * Dersom det er overlapp opprettes en prosesstask for å håndtere overlappet videre.
 */
@ApplicationScoped
public class VurderOpphørAvYtelser {
    private static final Logger LOG = LoggerFactory.getLogger(VurderOpphørAvYtelser.class);

    private static final Set<FagsakYtelseType> VURDER_OVERLAPP = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);

    private static final Period MATCH_INTERVALL_HENDELSE = Period.parse("P6W");

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FagsakRepository fagsakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private StønadsperiodeTjeneste stønadsperiodeTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;

    private static final Period TO_TETTE_GRENSE = Period.ofWeeks(48);

    @Inject
    public VurderOpphørAvYtelser(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                 StønadsperiodeTjeneste stønadsperiodeTjeneste,
                                 ProsessTaskTjeneste taskTjeneste,
                                 FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                 FamilieHendelseRepository familieHendelseRepository,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = behandlingRepositoryProvider.getPersonopplysningRepository();
        this.taskTjeneste = taskTjeneste;
        this.stønadsperiodeTjeneste = stønadsperiodeTjeneste;
        this.familieHendelseRepository = familieHendelseRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    VurderOpphørAvYtelser() {
        // CDI
    }

    void vurderOpphørAvYtelser(Behandling behandling) {
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            vurderOppørAvYtelserForFP(behandling);
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            vurderOpphørAvYtelserForSVP(behandling);
        }
    }

    private void vurderOppørAvYtelserForFP(Behandling iverksattBehandling) {
        stønadsperiodeTjeneste.stønadsperiodeStartdato(iverksattBehandling).ifPresent(startdatoIVB -> {
            løpendeSakerSomOverlapperUttakPåNyIkkeKobletSak(iverksattBehandling.getAktørId(), iverksattBehandling.getFagsak(), startdatoIVB)
                .forEach( fagsakOpphør -> opprettTaskForÅHåndtereOpphørBrukersSaker(fagsakOpphør, iverksattBehandling));

            // sjekker om ny sak på mor overlapper med fars tidligere saker
            if (RelasjonsRolleType.erMor(iverksattBehandling.getRelasjonsRolleType())) {
                personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(iverksattBehandling.getId())
                    .map(OppgittAnnenPartEntitet::getAktørId)
                    .ifPresent(annenPart -> løpendeSakerSomOverlapperUttakPåNyIkkeKobletSak(annenPart, iverksattBehandling.getFagsak(), startdatoIVB)
                        .forEach(fagsakOpphørFar -> opprettTaskForÅHåndtereOpphørBrukersSaker(fagsakOpphørFar, iverksattBehandling)));
            }
        });
    }

    private LocalDate finnGjeldendeFamiliehendelseDato(long behandlingId) {
        return familieHendelseRepository.hentAggregat(behandlingId).getGjeldendeVersjon().getSkjæringstidspunkt();
    }

    private void vurderOpphørAvYtelserForSVP(Behandling behandling) {
        stønadsperiodeTjeneste.stønadsperiode(behandling).ifPresent(stønadsperiodeIVB ->
            løpendeSakerSomOverlapperUttakNySakSVP(behandling, stønadsperiodeIVB)
                .forEach(sakspar -> opprettTaskForÅHåndtereOpphør(sakspar.fagsakOpphør(), sakspar.opphørÅrsak())));
    }

    private void opprettTaskForÅHåndtereOpphør(Fagsak sakOpphør, Fagsak fersktVedtak) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterOpphørAvYtelserTask.class);
        prosessTaskData.setFagsak(sakOpphør.getSaksnummer().getVerdi(), sakOpphør.getId());
        prosessTaskData.setProperty(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY, String.format("Overlapp identifisert: Vurder saksnr %s vedtak i saksnr %s", sakOpphør.getSaksnummer(), fersktVedtak.getSaksnummer()));
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    private void opprettTaskForÅHåndtereOpphørBrukersSaker(Fagsak sakOpphør, Behandling iverksattBehandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterOpphørAvYtelserTask.class);

        //dersom to tette fødsler skal vi opprette VKY for at SB må ta stilling til eventuelt gjenstående minsterett ellers ikke
        if (toTetteFødsler(sakOpphør, iverksattBehandling) && overlappendeYtelse(sakOpphør, iverksattBehandling)) {
            prosessTaskData.setProperty(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY, String.format("Overlapp på sak med minsterett ved tette fødsler identifisert: Vurder om sak %s har brukt opp minsteretten, og skal opphøres pga ny sak %s", sakOpphør.getSaksnummer(), iverksattBehandling.getSaksnummer()));
        } else {
            prosessTaskData.setProperty(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY, null);
        }

        prosessTaskData.setFagsak(sakOpphør.getSaksnummer().getVerdi(), sakOpphør.getId());
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    private LocalDate hentFamilieHenseleDatoFraSisteYtelseBehandling(Fagsak sakOpphør) {
        return hentBehandling(sakOpphør)
            .map(sisteBehandlingPåsakOpphør -> finnGjeldendeFamiliehendelseDato(sisteBehandlingPåsakOpphør.getId()))
            .orElse(null);
    }

    private Optional<Behandling> hentBehandling(Fagsak sakOpphør) {
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(sakOpphør.getId());
    }

    private boolean toTetteFødsler(Fagsak sakOpphør, Behandling iverksattBehandling) {
        if (!beggeSakerErForeldrepenger(sakOpphør, iverksattBehandling)) {
            return false;
        }

        var fhDatoFraBehOpphør = hentFamilieHenseleDatoFraSisteYtelseBehandling(sakOpphør);
        var fhDatoNyBeh = finnGjeldendeFamiliehendelseDato(iverksattBehandling.getId());

        if (fhDatoFraBehOpphør == null || fhDatoNyBeh == null) {
            return false;
        }

        var tidligsteFH = fhDatoFraBehOpphør.isBefore(fhDatoNyBeh) ? fhDatoFraBehOpphør : fhDatoNyBeh;
        var senesteFH = fhDatoNyBeh.isAfter(fhDatoFraBehOpphør) ? fhDatoNyBeh : fhDatoFraBehOpphør;

        if (erFørsteBehandlingUtenMinsterett(sakOpphør, iverksattBehandling, fhDatoFraBehOpphør, fhDatoNyBeh) || fhDatoFraBehOpphør.equals(fhDatoNyBeh)) {
            return false;
        }
        var grenseToTette = tidligsteFH.plus(TO_TETTE_GRENSE).plusDays(1);
        return grenseToTette.isAfter(senesteFH);
    }

    private boolean erFørsteBehandlingUtenMinsterett(Fagsak sakOpphør, Behandling iverksattBehandling, LocalDate fhDatoFraBehOpphør, LocalDate fhDatoNyBeh) {
        if (fhDatoFraBehOpphør.isBefore(fhDatoNyBeh)) {
            var sisteYtelseBehandling = hentBehandling(sakOpphør).orElseThrow(() -> new IllegalStateException(String.format("Finner ikke siste ytelsesbehandling for sak %s.", sakOpphør.getSaksnummer().getVerdi())));
            return  skjæringstidspunktTjeneste.getSkjæringstidspunkter(sisteYtelseBehandling.getId()).utenMinsterett();
        } else {
            return skjæringstidspunktTjeneste.getSkjæringstidspunkter(iverksattBehandling.getId()).utenMinsterett();
        }
    }

    private boolean overlappendeYtelse(Fagsak sakOpphør, Behandling iverksattBehandling) {
        return !stønadsperiodeTjeneste.utbetalingsTidslinjeEnkeltSak(sakOpphør)
            .intersection(stønadsperiodeTjeneste.utbetalingsTidslinjeEnkeltSak(iverksattBehandling)).isEmpty();
    }

    private boolean beggeSakerErForeldrepenger(Fagsak sakOpphør, Behandling iverksattBehandling) {
        return FagsakYtelseType.FORELDREPENGER.equals(sakOpphør.getYtelseType()) && FagsakYtelseType.FORELDREPENGER.equals(iverksattBehandling.getFagsakYtelseType());
    }

    private List<Fagsak> løpendeSakerSomOverlapperUttakPåNyIkkeKobletSak(AktørId aktørId, Fagsak fagsakIVB, LocalDate startdatoIVB) {
        return fagsakRepository.hentForBruker(aktørId)
            .stream()
            .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
            .filter(f -> !erSammeEllerKobletSak(fagsakIVB, f))
            .filter(f -> erMaxDatoPåLøpendeSakEtterStartDatoNysak(f, startdatoIVB))
            .toList();
    }

    private boolean erSammeEllerKobletSak(Fagsak iverksatt, Fagsak sjekk) {
        var kobletTilIverksatt = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(sjekk)
            .flatMap(fr -> fr.getRelatertFagsak(sjekk))
            .filter(f -> f.getSaksnummer().equals(iverksatt.getSaksnummer()))
            .isPresent();
        return iverksatt.getSaksnummer().equals(sjekk.getSaksnummer()) || kobletTilIverksatt;
    }

    private record FagsakPar(Fagsak fagsakOpphør, Fagsak opphørÅrsak) {}

    private List<FagsakPar> løpendeSakerSomOverlapperUttakNySakSVP(Behandling behandlingIVB,
                                                                LocalDateInterval stønadsperiodeIVB) {
        return fagsakRepository.hentForBruker(behandlingIVB.getAktørId())
            .stream()
            .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
            .filter(f -> !behandlingIVB.getSaksnummer().equals(f.getSaksnummer()))
            .flatMap(f -> sjekkOverlappMotIverksattSvangerskapspenger(f, behandlingIVB, stønadsperiodeIVB).stream())
            .toList();
    }

    private Optional<FagsakPar> sjekkOverlappMotIverksattSvangerskapspenger(Fagsak sjekkFagsak, Behandling behandlingIVB,
                                                                          LocalDateInterval stønadsperiodeIVB) {
        var overlapp = stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(sjekkFagsak)
            .filter(utbetalingsperiode -> utbetalingsperiode.overlaps(stønadsperiodeIVB));
        if (overlapp.isEmpty()) return Optional.empty();
        var saksnummer = behandlingIVB.getSaksnummer();
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(sjekkFagsak.getYtelseType())) {
            LOG.info("Overlapp SVP oppdaget for sak {} med løpende SVP-sak {}. Ingen revurdering opprettet", saksnummer, sjekkFagsak.getSaksnummer());
            return Optional.empty();
        }
        return overlapp.flatMap(utbetalingsperiodeForeldrepenger -> {
            if (stønadsperiodeIVB.getFomDato().isBefore(utbetalingsperiodeForeldrepenger.getFomDato())) {
                // Overlapp med løpende foreldrepenger på samme barn - opprettes revurdering på innvilget svp behandling
                LOG.info("Overlapp SVP: SVP-sak {} overlapper med FP-sak på samme barn {}", saksnummer, sjekkFagsak.getSaksnummer());
                return Optional.of(new FagsakPar(behandlingIVB.getFagsak(), sjekkFagsak)); // Jepp her må SVP opphøres
            } else if (stønadsperiodeTjeneste.fullUtbetalingSisteUtbetalingsperiode(sjekkFagsak)) {
                // Overlapp med løpende foreldrepenger og svp for nytt barn - opprettes revurdering på løpende foreldrepenger-sak
                LOG.info("Overlapp SVP: SVP-sak {} overlapper med FP-sak {}", saksnummer, sjekkFagsak.getSaksnummer());
                return Optional.of(new FagsakPar(sjekkFagsak, behandlingIVB.getFagsak()));
            } else {
                // Overlapp med løpenge graderte foreldrepenger -  kan være tillatt så derfor logger vi foreløpig
                LOG.info("Overlapp SVP: SVP-sak {} overlapper med gradert FP-sak {}. Ingen revurdering opprettet", saksnummer, sjekkFagsak.getSaksnummer());
                return Optional.empty();
            }
        });
    }

    private boolean erMaxDatoPåLøpendeSakEtterStartDatoNysak(Fagsak fagsak, LocalDate startdatoIVB) {
        var startdato = stønadsperiodeTjeneste.stønadsperiodeStartdato(fagsak).orElse(Tid.TIDENES_ENDE);
        var sluttdato = stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(fagsak).orElse(Tid.TIDENES_BEGYNNELSE);
        return startdato.minus(MATCH_INTERVALL_HENDELSE).isBefore(startdatoIVB.plusWeeks(6)) && (sluttdato.equals(startdatoIVB) || sluttdato.isAfter(startdatoIVB));
    }

}
