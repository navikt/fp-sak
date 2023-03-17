package no.nav.foreldrepenger.ytelse.beregning.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ApplicationScoped
public class SvangerskapspengerFeriekvoteTjeneste {
    private static final int AKSEPTERT_FEILMARGIN_TERMINDATO_DAGER = 60;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private SvangerskapFeriepengeKvoteBeregner svangerskapFeriepengeKvoteBeregner;

    public SvangerskapspengerFeriekvoteTjeneste() {
        // CDI
    }

    @Inject
    public SvangerskapspengerFeriekvoteTjeneste(FagsakRepository fagsakRepository,
                                                BehandlingRepository behandlingRepository,
                                                FamilieHendelseRepository familieHendelseRepository,
                                                SvangerskapFeriepengeKvoteBeregner svangerskapFeriepengeKvoteBeregner,
                                                BeregningsresultatRepository beregningsresultatRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.svangerskapFeriepengeKvoteBeregner = svangerskapFeriepengeKvoteBeregner;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }


    public Optional<Integer> beregnTilgjengeligFeriekvote(BehandlingReferanse behandlingReferanse, BeregningsresultatEntitet beregnetYtelse) {
        var fagsakerPåSøker = fagsakRepository.hentForBruker(behandlingReferanse.aktørId());
        var svpSaker = fagsakerPåSøker.stream()
            .filter(fs -> fs.getYtelseType().equals(FagsakYtelseType.SVANGERSKAPSPENGER))
            .filter(fs -> !fs.getSaksnummer().equals(behandlingReferanse.saksnummer()))
            .toList();
        var gjeldendeVedtakForSVP = svpSaker.stream()
            .map(fs -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fs.getId()))
            .flatMap(Optional::stream)
            .toList();
        var termindato = finnTermindato(behandlingReferanse.behandlingId()).orElseThrow();

        // Finner behandlinger som gjelder samme svangerskap
        var behandlingerSomAngårSammeSvangerskap = finnBehandlingerSomGjelderSammeSvangerskap(gjeldendeVedtakForSVP, termindato);
        var annenTilkjentYtelsePåSammeSvangerskap = behandlingerSomAngårSammeSvangerskap.stream()
            .map(b -> beregningsresultatRepository.hentUtbetBeregningsresultat(b.getId()))
            .flatMap(Optional::stream)
            .toList();
        return svangerskapFeriepengeKvoteBeregner.beregn(beregnetYtelse, annenTilkjentYtelsePåSammeSvangerskap);
    }

    private List<Behandling> finnBehandlingerSomGjelderSammeSvangerskap(List<Behandling> gjeldendeVedtakForSVP,
                                                                        LocalDate termindato) {
        List<Behandling> behandlingerSomGjelderSammeSvangerskap = new ArrayList<>();
        gjeldendeVedtakForSVP.forEach(behandling -> {
            var termindatoOpt = finnTermindato(behandling.getId());
            var gjelderSammeSvangerskap = termindatoOpt.map(td -> erInnenForTerskel(td, termindato)).orElse(false);
            if (gjelderSammeSvangerskap) {
                behandlingerSomGjelderSammeSvangerskap.add(behandling);
            }
        });
        return behandlingerSomGjelderSammeSvangerskap;
    }

    private boolean erInnenForTerskel(LocalDate termindatoSomSjekkes, LocalDate termindatoSomBehandles) {
        var periodeViSerEtterMatchendeTermindato = DatoIntervallEntitet.fraOgMedTilOgMed(termindatoSomBehandles.minusDays(AKSEPTERT_FEILMARGIN_TERMINDATO_DAGER),
            termindatoSomBehandles.plusDays(AKSEPTERT_FEILMARGIN_TERMINDATO_DAGER));
        return periodeViSerEtterMatchendeTermindato.inkluderer(termindatoSomSjekkes);

    }

    private Optional<LocalDate> finnTermindato(Long behandlingId) {
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        return familieHendelseGrunnlag.flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeTerminbekreftelse)
            .map(TerminbekreftelseEntitet::getTermindato);
    }


}
