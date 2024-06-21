package no.nav.foreldrepenger.ytelse.beregning;


import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapInputFraVLTilRegelGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegler;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
public class BeregnYtelseTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BeregnYtelseTjeneste.class);

    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;
    private MapInputFraVLTilRegelGrunnlag mapBeregningsresultatFraVLTilRegel;

    public BeregnYtelseTjeneste() {
        // CDI
    }

    @Inject
    public BeregnYtelseTjeneste(@Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste,
                                MapInputFraVLTilRegelGrunnlag mapBeregningsresultatFraVLTilRegel) {
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
        this.mapBeregningsresultatFraVLTilRegel = mapBeregningsresultatFraVLTilRegel;
    }

    public BeregningsresultatEntitet beregnYtelse(BehandlingReferanse referanse) {
        // Map til regelmodell
        var regelmodell = mapBeregningsresultatFraVLTilRegel.mapFra(referanse);

        // Verifiser input til regel
        if (andelerIBeregningMåLiggeIUttak(referanse)) {
            BeregningsresultatInputVerifiserer.verifiserAlleAndelerIBeregningErIUttak(regelmodell);
        } else {
            BeregningsresultatInputVerifiserer.verifiserAndelerIUttakLiggerIBeregning(regelmodell);
        }

        // Kalle regeltjeneste
        var beregningsresultat = fastsettBeregningsresultat(regelmodell);

        // Verifiser beregningsresultat
        try {
            BeregningsresultatOutputVerifiserer.verifiserOutput(beregningsresultat);
        } catch (Exception e) {
            log(regelmodell);
            throw new RuntimeException("Validering av beregningsresultat feilet", e);
        }

        return beregningsresultat;
    }

    public BeregningsresultatFeriepenger beregnFeriepenger(BehandlingReferanse referanse, BeregningsresultatEntitet beregningsresultat) {
        // Map til regelmodell
        var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, referanse.fagsakYtelseType()).orElseThrow();
        var feriepenger = feriepengerTjeneste.beregnFeriepenger(referanse, beregningsresultat);
        return feriepenger;
    }

    private static BeregningsresultatEntitet fastsettBeregningsresultat(BeregningsresultatGrunnlag regelmodell) {
        // Kjør regel
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(regelmodell);

        // Map tilbake til domenemodell fra regelmodell
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput(resultat.regelInput())
            .medRegelSporing(resultat.regelSporing())
            .medRegelVersjon(Optional.ofNullable(resultat.versjon()).map(v -> v.startsWith("f") ? v : "fp-ytelse-beregn:" + v).orElse(null))
            .build();

        MapBeregningsresultatFraRegelTilVL.mapFra(resultat.beregningsresultat(), beregningsresultat);

        return beregningsresultat;
    }

    private void log(BeregningsresultatGrunnlag grunnlag) {
        try {
            var maskertRegelinput = DefaultJsonMapper.toPrettyJson(grunnlag).replaceAll("\\d{13}|\\d{11}|\\d{9}", "*");
            LOG.info("Regelinput til beregning av tilkjent ytelse {}", maskertRegelinput);
        } catch (TekniskException jsonProcessingException) {
            LOG.warn("Feil ved logging av regelinput", jsonProcessingException);
        }
    }

    private boolean andelerIBeregningMåLiggeIUttak(BehandlingReferanse ref) {
        return ref.fagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER);
    }
}
