package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;


@ApplicationScoped
public class SVPFeriepengekontrollTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(SVPFeriepengekontrollTjeneste.class);

    private static final int AKSEPTERT_FEILMARGIN_TERMINDATO_DAGER = 60;
    private static final int SVP_DAGER_FERIEKVOTE = 64;
    private static final DatoIntervallEntitet ÅR_2021 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2021,1,1),
        LocalDate.of(2021,12,31));

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BeregningsresultatRepository beregningsresultatRepository;


    protected SVPFeriepengekontrollTjeneste() {
        // CDI
    }

    @Inject
    public SVPFeriepengekontrollTjeneste(FagsakRepository fagsakRepository,
                                         BehandlingRepository behandlingRepository,
                                         FamilieHendelseRepository familieHendelseRepository,
                                         BeregningsresultatRepository beregningsresultatRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    // Tjeneste som kontrollerer om aktuell søker har fått for mye feriepenger innvilget i sine SVP vedtak
    public void utledOmForMyeFeriepenger(AktørId aktørId) {
        var fagsakerPåSøker = fagsakRepository.hentForBruker(aktørId);
        var svpSaker = fagsakerPåSøker.stream()
            .filter(fs -> fs.getYtelseType().equals(FagsakYtelseType.SVANGERSKAPSPENGER))
            .collect(Collectors.toList());
        var gjeldendeVedtakForSVP = svpSaker.stream()
            .map(fs -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fs.getId()))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        // Grupper behandlinger på termindato for å finne ut hvor mange som gjelder samme fødsel
        var grupperteBehandlinger = grupperBehandlingerPåSammeTermin(gjeldendeVedtakForSVP);
        grupperteBehandlinger.entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .forEach(e -> harHattUtbetalingI2021OgHarBruktOppFeriepengedager(e.getValue()));
    }

    private void harHattUtbetalingI2021OgHarBruktOppFeriepengedager(List<Behandling> behandlinger) {

        var tilkjenteYtelser = behandlinger.stream()
            .map(beh -> beregningsresultatRepository.hentUtbetBeregningsresultat(beh.getId()))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        // Early return
        if (tilkjenteYtelser.isEmpty() || tilkjenteYtelser.stream().noneMatch(ty -> erOpptjentI2021(ty.getBeregningsresultatFeriepenger()))) {
            return;
        }
        var saksnummer = behandlinger.stream()
            .map(b -> b.getFagsak().getSaksnummer().getVerdi())
            .collect(Collectors.toList());
        var bruktFeriekvote = summerBruktFeriekvote(tilkjenteYtelser);
        var info = String.format("Følgende saksnummer %s har brukt til sammen %s dager av feriepengekvoten", saksnummer, bruktFeriekvote);
        LOG.info("FP-985860: " + info);
        if (bruktFeriekvote > SVP_DAGER_FERIEKVOTE) {
            var msg = String.format("Følgende saksnummer tilhører samme svangerskap, har overskridet"
                + " feriepengekvoten (dager brukt var: %s) og har feriepenger opptjent i 2021: %s", bruktFeriekvote, saksnummer);
            LOG.info("FP-985861: " + msg);
        }
    }

    private int summerBruktFeriekvote(List<BeregningsresultatEntitet> tilkjenteYtelser) {
        return tilkjenteYtelser.stream().mapToInt(this::finnVirkedagerIFerieperiode).sum();
    }

    private int finnVirkedagerIFerieperiode(BeregningsresultatEntitet ty) {
        var feriepengeperiode = lagFeriepengeperiode(ty.getBeregningsresultatFeriepenger());
        if (feriepengeperiode.isEmpty()) {
            return 0;
        }
        return Virkedager.beregnAntallVirkedager(feriepengeperiode.get().getFomDato(), feriepengeperiode.get().getTomDato());
    }

    private boolean erOpptjentI2021(Optional<BeregningsresultatFeriepenger> beregningsresultatFeriepenger) {
        var feriepengeperiode = lagFeriepengeperiode(beregningsresultatFeriepenger);
        if (feriepengeperiode.isEmpty()) {
            return false;
        }
        return feriepengeperiode.get().overlapper(ÅR_2021);
    }

    private Optional<DatoIntervallEntitet> lagFeriepengeperiode(Optional<BeregningsresultatFeriepenger> beregningsresultatFeriepenger) {
        var fom = beregningsresultatFeriepenger.map(BeregningsresultatFeriepenger::getFeriepengerPeriodeFom);
        var tom = beregningsresultatFeriepenger.map(BeregningsresultatFeriepenger::getFeriepengerPeriodeTom);
        if (fom.isPresent() && tom.isPresent()) {
            return Optional.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom.get(), tom.get()));
        }
        return Optional.empty();
    }

    private Map<LocalDate, List<Behandling>> grupperBehandlingerPåSammeTermin(List<Behandling> gjeldendeVedtakForSVP) {
        Map<LocalDate, List<Behandling>> grupperteBehandlinger = new HashMap<>();
        gjeldendeVedtakForSVP.forEach(behandling -> {
            var termindatoOpt = finnTermindato(behandling.getId());
            if (termindatoOpt.isPresent()) {
                var termindato = termindatoOpt.get();
                var matchendeTermindato = finnEksisterendeTermindatoInnenforTerskel(grupperteBehandlinger.keySet(), termindato);
                if (matchendeTermindato.isPresent()) {
                    var info = String.format("Fant match på termindato mellom %s og %s på saksnummer %s", matchendeTermindato.get(), termindato,
                        behandling.getFagsak().getSaksnummer().getVerdi());
                    LOG.info("FP-985859: " + info);
                    var behandlinger = grupperteBehandlinger.get(matchendeTermindato.get());
                    behandlinger.add(behandling);
                    grupperteBehandlinger.put(matchendeTermindato.get(), behandlinger);
                } else {
                    var nyListe = new ArrayList<Behandling>();
                    nyListe.add(behandling);
                    grupperteBehandlinger.put(termindato, nyListe);
                }
            }
        });
        return grupperteBehandlinger;
    }

    private Optional<LocalDate> finnEksisterendeTermindatoInnenforTerskel(Set<LocalDate> keySet, LocalDate termindato) {
        var periodeViSerEtterMatchendeTermindato = DatoIntervallEntitet.fraOgMedTilOgMed(termindato.minusDays(AKSEPTERT_FEILMARGIN_TERMINDATO_DAGER),
            termindato.plusDays(AKSEPTERT_FEILMARGIN_TERMINDATO_DAGER));
        var termindatoerSomKanGjeldeSammeSvangerskap = keySet.stream()
            .filter(periodeViSerEtterMatchendeTermindato::inkluderer)
            .collect(Collectors.toList());
        if (termindatoerSomKanGjeldeSammeSvangerskap.size() > 1) {
            throw new IllegalStateException("Forventet maks 1 dato som kan matche termindato "
                + termindato + ". Listen over matchende datoer er " + termindatoerSomKanGjeldeSammeSvangerskap);
        }
        return termindatoerSomKanGjeldeSammeSvangerskap.stream().findFirst();

    }

    private Optional<LocalDate> finnTermindato(Long behandlingId) {
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        return familieHendelseGrunnlag.flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeTerminbekreftelse)
            .map(TerminbekreftelseEntitet::getTermindato);
    }
}
