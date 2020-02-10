package no.nav.foreldrepenger.behandling.steg.beregnytelse;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.foreldrepenger.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;

/** Felles steg for å beregne tilkjent ytelse for foreldrepenger og svangerskapspenger (ikke engangsstønad) */

@BehandlingStegRef(kode = "BERYT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class BeregneYtelseStegImpl implements BeregneYtelseSteg {

    private BehandlingRepository behandlingRepository;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;
    private Instance<FinnEndringsdatoBeregningsresultatTjeneste> finnEndringsdatoBeregningsresultatTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    protected BeregneYtelseStegImpl() {
        // for proxy
    }

    @Inject
    public BeregneYtelseStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                 HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                 UttakInputTjeneste uttakInputTjeneste,
                                 FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                 @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste,
                                 @Any Instance<FinnEndringsdatoBeregningsresultatTjeneste> finnEndringsdatoBeregningsresultatTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.finnEndringsdatoBeregningsresultatTjeneste = finnEndringsdatoBeregningsresultatTjeneste;
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var input = uttakInputTjeneste.lagInput(behandlingId);
        var ref = input.getBehandlingReferanse();

        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandlingId);

        // Kalle regeltjeneste
        BeregningsresultatEntitet beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag, input);

        // Verifiser beregningsresultat
        BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);

        // Beregn feriepenger
        var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, ref.getFagsakYtelseType()).orElseThrow();
        feriepengerTjeneste.beregnFeriepenger(behandling, beregningsresultat);

        // Sett endringsdato
        if (behandling.erRevurdering()) {
            var endringsdatoBeregningsresultatTjeneste = FagsakYtelseTypeRef.Lookup.find(finnEndringsdatoBeregningsresultatTjeneste, ref.getFagsakYtelseType())
                .orElseThrow();
            Optional<LocalDate> endringsDato = endringsdatoBeregningsresultatTjeneste.finnEndringsdato(behandling, beregningsresultat);
            endringsDato.ifPresent(endringsdato -> BeregningsresultatEntitet.builder(beregningsresultat).medEndringsdato(endringsdato));
        }

        // Lagre beregningsresultat
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }
}
