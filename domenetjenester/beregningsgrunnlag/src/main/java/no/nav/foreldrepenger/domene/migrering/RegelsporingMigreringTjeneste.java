package no.nav.foreldrepenger.domene.migrering;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRegelSporing;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

// For å håndtere migrering berørt av TFP-6040
@ApplicationScoped
public class RegelsporingMigreringTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(RegelsporingMigreringTjeneste.class);

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public RegelsporingMigreringTjeneste() {
        // CDI
    }

    @Inject
    public RegelsporingMigreringTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> finnRegelsporingGrunnlag(BeregningsgrunnlagGrunnlagEntitet aktivtGrunnlag, BehandlingReferanse referanse) {
        if (aktivtGrunnlag.getBeregningsgrunnlagTilstand().erFør(BeregningsgrunnlagTilstand.KOFAKBER_UT)) {
            // Ikke rammet av feil
            return aktivtGrunnlag.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::getRegelSporinger).orElse(Map.of());
        }
        var sporingerFraAktivtGrunnlag = aktivtGrunnlag.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getRegelSporinger)
            .orElse(Map.of());
        if (sporingerFraAktivtGrunnlag.containsKey(BeregningsgrunnlagRegelType.BRUKERS_STATUS)
            && sporingerFraAktivtGrunnlag.containsKey(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT)) {
            // Ikke rammet av feil
            return sporingerFraAktivtGrunnlag;
        }
        // Rammet av feil
        LOG.info("Behandling {} mangler regelsporinger, henter fra tidligere grunnlag", referanse.behandlingId());
        var sporingerFraGammeltGrunnlag = finnSporingFraGammelGrunnlag(referanse);
        if (sporingerFraGammeltGrunnlag.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS) != null && sporingerFraAktivtGrunnlag.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS) == null) {
            logg(referanse.behandlingId(), BeregningsgrunnlagRegelType.BRUKERS_STATUS);
            sporingerFraAktivtGrunnlag.put(BeregningsgrunnlagRegelType.BRUKERS_STATUS, sporingerFraGammeltGrunnlag.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS));
        }
        // Denne regeltypen ble introdusert senere, og finnes ikke på grunnlag før juli 2020.
        // Kopierer derfor også den hvis den finnes, men tar kun utgangspuiinkt i at BRUKERS_STATUS og SKJÆRINGSTIDSPUNKT må være til stede.
        if (sporingerFraGammeltGrunnlag.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE) != null && sporingerFraAktivtGrunnlag.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE) == null) {
            logg(referanse.behandlingId(), BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE);
            sporingerFraAktivtGrunnlag.put(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE, sporingerFraGammeltGrunnlag.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE));
        }
        if (sporingerFraGammeltGrunnlag.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT) != null && sporingerFraAktivtGrunnlag.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT) == null) {
            logg(referanse.behandlingId(), BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT);
            sporingerFraAktivtGrunnlag.put(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT, sporingerFraGammeltGrunnlag.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT));
        }
        return sporingerFraAktivtGrunnlag;
    }

    private void logg(Long behandlingId, BeregningsgrunnlagRegelType beregningsgrunnlagRegelType) {
        LOG.info("Behandling {} legger til regelsporing {} fra gammelt grunnlag", behandlingId, beregningsgrunnlagRegelType);
    }

    private Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> finnSporingFraGammelGrunnlag(BehandlingReferanse referanse) {
        var kofakGrunnlag = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId(),
            BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        // Behandlinger som starter i uttak har ikke grunnlag med type OPPDATERT_MED_ANDELER, er derfor ingen regelsporinger å kopiere
        return kofakGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(BeregningsgrunnlagEntitet::getRegelSporinger)
            .orElse(Map.of());
    }


}
