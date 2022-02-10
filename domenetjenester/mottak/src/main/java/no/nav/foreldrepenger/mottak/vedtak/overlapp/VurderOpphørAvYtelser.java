package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.StønadsperiodeTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.konfig.Tid;

/**
 * Tjenesten sjekker om det finnes løpende saker for den personen det innvilges foreldrepenger eller svangerskapsper på.
 * Om det finnes løpende saker sjekkes det om ny sak overlapper med løpende sak. Det sjekkes både for mor, far og en
 * eventuell medforelder på foreldrepenger.
 * Dersom det er overlapp opprettes en prosesstask for å håndtere overlappet videre.
 */
@ApplicationScoped
public class VurderOpphørAvYtelser {
    private static final Logger LOG = LoggerFactory.getLogger(VurderOpphørAvYtelser.class);

    private static final Set<FagsakYtelseType> VURDER_OVERLAPP = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
    private static final BigDecimal HUNDRE = new BigDecimal(100);

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private StønadsperiodeTjeneste stønadsperiodeTjeneste;

    @Inject
    public VurderOpphørAvYtelser(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                 StønadsperiodeTjeneste stønadsperiodeTjeneste,
                                 ProsessTaskTjeneste taskTjeneste) {
        this.fagsakRelasjonRepository = behandlingRepositoryProvider.getFagsakRelasjonRepository();
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = behandlingRepositoryProvider.getPersonopplysningRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.taskTjeneste = taskTjeneste;
        this.stønadsperiodeTjeneste = stønadsperiodeTjeneste;
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

    private void vurderOppørAvYtelserForFP(Behandling behandling) {
        var startdatoIVB = stønadsperiodeTjeneste.stønadsperiodeStartdato(behandling).orElse(null);
        if (startdatoIVB != null) {
            løpendeSakerSomOverlapperUttakPåNySak(behandling.getAktørId(), behandling.getFagsak(), startdatoIVB)
                .forEach(s -> opprettTaskForÅHåndtereOpphør(s, behandling.getFagsak()));

            if (RelasjonsRolleType.erMor(behandling.getRelasjonsRolleType())) {
                personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandling.getId())
                    .map(OppgittAnnenPartEntitet::getAktørId)
                    .ifPresent(annenPart -> løpendeSakerSomOverlapperUttakPåNySak(annenPart, behandling.getFagsak(), startdatoIVB)
                        .forEach(s -> opprettTaskForÅHåndtereOpphør(s, behandling.getFagsak())));
            }
        }
    }

    private void vurderOpphørAvYtelserForSVP(Behandling behandling) {
        stønadsperiodeTjeneste.stønadsperiode(behandling).ifPresent(stønadsperiodeIVB ->
            løpendeSakerSomOverlapperUttakNySakSVP(behandling, stønadsperiodeIVB)
                .forEach(sakspar -> opprettTaskForÅHåndtereOpphør(sakspar.fagsakOpphør(), sakspar.opphørÅrsak())));
    }

    private void opprettTaskForÅHåndtereOpphør(Fagsak sakOpphør, Fagsak fersktVedtak) {
        var beskrivelse = String.format("Overlapp identifisert: Vurder saksnr %s vedtak i saksnr %s", sakOpphør.getSaksnummer(), fersktVedtak.getSaksnummer());
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterOpphørAvYtelserTask.class);
        prosessTaskData.setFagsakId(sakOpphør.getId());
        prosessTaskData.setProperty(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY, beskrivelse);
        prosessTaskData.setProperty(HåndterOpphørAvYtelserTask.BEHANDLING_ÅRSAK_KEY,
            BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN.getKode());
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    private List<Fagsak> løpendeSakerSomOverlapperUttakPåNySak(AktørId aktørId, Fagsak fagsakIVB, LocalDate startdatoIVB) {
        var saker = fagsakRepository.hentForBruker(aktørId)
            .stream()
            .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
            .filter(f -> !erSammeEllerKobletSak(fagsakIVB, f))
            .filter(f -> erMaxDatoPåLøpendeSakEtterStartDatoNysak(f, startdatoIVB))
            .collect(Collectors.toList());
        return saker;
    }

    private boolean erSammeEllerKobletSak(Fagsak iverksatt, Fagsak sjekk) {
        var kobletTilIverksatt = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(sjekk)
            .flatMap(fr -> fr.getRelatertFagsak(sjekk))
            .filter(f -> f.getSaksnummer().equals(iverksatt.getSaksnummer()))
            .isPresent();
        return iverksatt.getSaksnummer().equals(sjekk.getSaksnummer()) || kobletTilIverksatt;
    }

    private record FagsakPar(Fagsak fagsakOpphør, Fagsak opphørÅrsak) {}

    private List<FagsakPar> løpendeSakerSomOverlapperUttakNySakSVP(Behandling behandlingIVB,
                                                                LocalDateInterval stønadsperiodeIVB) {
        var saker = fagsakRepository.hentForBruker(behandlingIVB.getAktørId())
            .stream()
            .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
            .filter(f -> !behandlingIVB.getFagsak().getSaksnummer().equals(f.getSaksnummer()))
            .flatMap(f -> sjekkOverlappMotIverksattSvangerskapspenger(f, behandlingIVB, stønadsperiodeIVB).stream())
            .collect(Collectors.toList());
        return saker;
    }

    private Optional<FagsakPar> sjekkOverlappMotIverksattSvangerskapspenger(Fagsak sjekkFagsak, Behandling behandlingIVB,
                                                                          LocalDateInterval stønadsperiodeIVB) {
        var saksnummer = behandlingIVB.getFagsak().getSaksnummer();
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(sjekkFagsak.getYtelseType())) {
            if (erMaxDatoPåLøpendeSakEtterStartDatoNysak(sjekkFagsak, stønadsperiodeIVB.getFomDato())) {
                LOG.info("Overlapp SVP oppdaget for sak {} med løpende SVP-sak {}. Ingen revurdering opprettet", saksnummer, sjekkFagsak.getSaksnummer());
            }
            return Optional.empty();
        }
        var stønadsperiodeForeldrepenger = stønadsperiodeTjeneste.stønadsperiode(sjekkFagsak).orElse(null);
        if (stønadsperiodeForeldrepenger == null || !stønadsperiodeForeldrepenger.overlaps(stønadsperiodeIVB)) {
            return Optional.empty();
        }
        if (stønadsperiodeIVB.getFomDato().isBefore(stønadsperiodeForeldrepenger.getFomDato())) {
            // Overlapp med løpende foreldrepenger på samme barn - opprettes revurdering på innvilget svp behandling
            LOG.info("Overlapp SVP: SVP-sak {} overlapper med FP-sak på samme barn {}", saksnummer, sjekkFagsak.getSaksnummer());
            return Optional.of(new FagsakPar(behandlingIVB.getFagsak(), sjekkFagsak)); // Jepp her må SVP opphøres
        } else if (erFullUtbetalingSistePeriode(sjekkFagsak.getId())) {
            // Overlapp med løpende foreldrepenger og svp for nytt barn - opprettes revurdering på løpende foreldrepenger-sak
            LOG.info("Overlapp SVP: SVP-sak {} overlapper med FP-sak {}", saksnummer, sjekkFagsak.getSaksnummer());
            return Optional.of(new FagsakPar(sjekkFagsak, behandlingIVB.getFagsak()));
        } else {
            // Overlapp med løpenge graderte foreldrepenger -  kan være tillatt så derfor logger vi foreløpig
            LOG.info("Overlapp SVP: SVP-sak {} overlapper med gradert FP-sak {}. Ingen revurdering opprettet", saksnummer, sjekkFagsak.getSaksnummer());
            return Optional.empty();
        }
    }

    private boolean erMaxDatoPåLøpendeSakEtterStartDatoNysak(Fagsak fagsak, LocalDate startdatoIVB) {
        var sluttdato = stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(fagsak).orElse(Tid.TIDENES_BEGYNNELSE);
        return sluttdato.equals(startdatoIVB) || sluttdato.isAfter(startdatoIVB);
    }

    private boolean erFullUtbetalingSistePeriode(Long fagsakId) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId)
            .map(Behandling::getId)
            .flatMap(b -> beregningsresultatRepository.hentUtbetBeregningsresultat(b))
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .max(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom))
            .map(BeregningsresultatPeriode::getKalkulertUtbetalingsgrad)
            .filter(ug -> ug.compareTo(HUNDRE) >= 0)
            .isPresent();
    }
}
