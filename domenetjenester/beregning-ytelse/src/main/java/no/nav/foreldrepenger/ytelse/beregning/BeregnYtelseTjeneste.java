package no.nav.foreldrepenger.ytelse.beregning;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.regler.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraVLTilRegel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static no.nav.vedtak.felles.integrasjon.rest.jersey.tokenx.TokenProvider.LOG;

@ApplicationScoped
public class BeregnYtelseTjeneste {
    private final JacksonJsonConfig jackson = new JacksonJsonConfig();
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel;

    public BeregnYtelseTjeneste() {
        // CDI
    }

    @Inject
    public BeregnYtelseTjeneste(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                UttakInputTjeneste uttakInputTjeneste,
                                MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.mapBeregningsresultatFraVLTilRegel = mapBeregningsresultatFraVLTilRegel;
    }

    public BeregningsresultatEntitet beregnYtelse(BehandlingReferanse referanse) {
        var behandlingId = referanse.behandlingId();
        var input = uttakInputTjeneste.lagInput(behandlingId);

        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandlingId);

        // Map til regelmodell
        var regelmodell = mapBeregningsresultatFraVLTilRegel.mapFra(beregningsgrunnlag, input);

        // Verifiser input til regel
        if (andelerIBeregningMåLiggeIUttak(referanse)) {
            BeregningsresultatInputVerifiserer.verifiserAlleAndelerIBeregningErIUttak(regelmodell);
        } else {
            BeregningsresultatInputVerifiserer.verifiserAndelerIUttakLiggerIBeregning(regelmodell);
        }

        // Kalle regeltjeneste
        var beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(regelmodell);

        // Verifiser beregningsresultat
        try {
            BeregningsresultatOutputVerifiserer.verifiserOutput(beregningsresultat);
        } catch (Exception e) {
            log(regelmodell);
            throw new RuntimeException("Validering av beregningsresultat feilet", e);
        }

        return beregningsresultat;
    }

    private void log(BeregningsresultatRegelmodell grunnlag) {
        try {
            String maskertRegelinput = jackson.toJson(grunnlag).replaceAll("\\d{13}|\\d{11}|\\d{9}", "*");
            LOG.info("Regelinput til beregning av tilkjent ytelse {}", maskertRegelinput);
        } catch (JsonProcessingException jsonProcessingException) {
            LOG.warn("Feil ved logging av regelinput", jsonProcessingException);
        }
    }

    private boolean andelerIBeregningMåLiggeIUttak(BehandlingReferanse ref) {
        return ref.fagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER);
    }
}
