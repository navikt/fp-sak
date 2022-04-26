package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
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

import no.nav.folketrygdloven.beregningsgrunnlag.util.Virkedager;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
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
import no.nav.foreldrepenger.domene.typer.AktørId;


@ApplicationScoped
public class SVPFeriepengekontrollTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(SVPFeriepengekontrollTjeneste.class);

    private static final int AKSEPTERT_FEILMARGIN_TERMINDATO_DAGER = 60;
    private static final int SVP_DAGER_FERIEKVOTE = 64;
    private static final Intervall ÅR_2021 = Intervall.fraOgMedTilOgMed(LocalDate.of(2021,1,1),
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
            .filter(e -> harHattUtbetalingI2021OgHarBruktOppFeriepengedager(e.getValue()));
    }

    private boolean harHattUtbetalingI2021OgHarBruktOppFeriepengedager(List<Behandling> behandlinger) {

        var tilkjenteYtelser = behandlinger.stream()
            .map(beh -> beregningsresultatRepository.hentUtbetBeregningsresultat(beh.getId()))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        // Early return
        if (tilkjenteYtelser.isEmpty() || tilkjenteYtelser.stream().noneMatch(ty -> erOpptjentI2021(ty.getBeregningsresultatFeriepenger()))) {
            return false;
        }

        var bruktFeriekvote = summerBruktFeriekvote(tilkjenteYtelser);
        if (bruktFeriekvote > SVP_DAGER_FERIEKVOTE) {
            var saksnummerMedFeil = behandlinger.stream()
                .map(b -> b.getFagsak().getSaksnummer().getVerdi())
                .collect(Collectors.toList());
            var msg = String.format("Følgende saksnummer tilhører samme svangerskap, har overskridet"
                + " feriepengekvoten (dager brukt var: %s) og har feriepenger opptjent i 2021: %s", bruktFeriekvote, saksnummerMedFeil);
            LOG.info("FP-985861: " + msg);
            return true;
        }
        return false;
    }

    private int summerBruktFeriekvote(List<BeregningsresultatEntitet> tilkjenteYtelser) {
        return tilkjenteYtelser.stream().mapToInt(this::finnVirkedagerIFerieperiode).sum();
    }

    private int finnVirkedagerIFerieperiode(BeregningsresultatEntitet ty) {
        if (ty.getBeregningsresultatFeriepenger().isEmpty()) {
            return 0;
        }
        var feriepengeperiode = lagFeriepengeperiode(ty.getBeregningsresultatFeriepenger().get());
        return Virkedager.beregnAntallVirkedager(feriepengeperiode.getFomDato(), feriepengeperiode.getTomDato());
    }

    private boolean erOpptjentI2021(Optional<BeregningsresultatFeriepenger> beregningsresultatFeriepenger) {
        if (beregningsresultatFeriepenger.isEmpty()) {
            return false;
        }
        var feriepengeperiode = lagFeriepengeperiode(beregningsresultatFeriepenger.get());
        return feriepengeperiode.overlapper(ÅR_2021);
    }

    private Intervall lagFeriepengeperiode(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
        return Intervall.fraOgMedTilOgMed(beregningsresultatFeriepenger.getFeriepengerPeriodeFom(),
            beregningsresultatFeriepenger.getFeriepengerPeriodeTom());
    }

    private Map<LocalDate, List<Behandling>> grupperBehandlingerPåSammeTermin(List<Behandling> gjeldendeVedtakForSVP) {
        Map<LocalDate, List<Behandling>> grupperteBehandlinger = new HashMap<>();
        gjeldendeVedtakForSVP.forEach(behandling -> {
            var termindatoOpt = finnTermindato(behandling.getId());
            if (termindatoOpt.isPresent()) {
                var termindato = termindatoOpt.get();
                var matchendeTermindato = finnEksisterendeTermindatoInnenforTerskel(grupperteBehandlinger.keySet(), termindato);
                if (matchendeTermindato.isPresent()) {
                    var behandlinger = grupperteBehandlinger.get(matchendeTermindato.get());
                    behandlinger.add(behandling);
                    grupperteBehandlinger.put(matchendeTermindato.get(), behandlinger);
                } else {
                    grupperteBehandlinger.put(termindato, List.of(behandling));
                }
            }
        });
        return grupperteBehandlinger;
    }

    private Optional<LocalDate> finnEksisterendeTermindatoInnenforTerskel(Set<LocalDate> keySet, LocalDate termindato) {
        var periodeViSerEtterMatchendeTermindato = Intervall.fraOgMedTilOgMed(termindato.minusDays(AKSEPTERT_FEILMARGIN_TERMINDATO_DAGER),
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
