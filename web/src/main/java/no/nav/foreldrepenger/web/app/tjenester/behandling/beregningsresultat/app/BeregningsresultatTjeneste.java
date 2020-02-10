package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatEngangsstønadDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatMedUttaksplanDto;

@ApplicationScoped
public class BeregningsresultatTjeneste {

    private LegacyESBeregningRepository beregningRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private UttakRepository uttakRepository;
    private BeregningsresultatMedUttaksplanMapper beregningsresultatMedUttaksplanMapper;

    public BeregningsresultatTjeneste() {
        // For CDI
    }

    @Inject
    public BeregningsresultatTjeneste(LegacyESBeregningRepository beregningRepository,
                                      BeregningsresultatRepository beregningsresultatRepository,
                                      UttakRepository uttakRepository,
                                      BeregningsresultatMedUttaksplanMapper beregningsresultatMedUttaksplanMapper) {
        this.beregningRepository = beregningRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.uttakRepository = uttakRepository;
        this.beregningsresultatMedUttaksplanMapper = beregningsresultatMedUttaksplanMapper;
    }

    public Optional<BeregningsresultatMedUttaksplanDto> lagBeregningsresultatMedUttaksplan(Behandling behandling) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatFPAggregatEntitet = beregningsresultatRepository
            .hentBeregningsresultatAggregat(behandling.getId());
        return beregningsresultatFPAggregatEntitet
            .map(bresAggregat -> beregningsresultatMedUttaksplanMapper.lagBeregningsresultatMedUttaksplan(behandling, bresAggregat, uttakResultat));
    }

    public Optional<BeregningsresultatEngangsstønadDto> lagBeregningsresultatEnkel(Long behandlingId) {
        Optional<LegacyESBeregning> sisteBeregningOpt = beregningRepository.getSisteBeregning(behandlingId);
        if (sisteBeregningOpt.isPresent()) {
            BeregningsresultatEngangsstønadDto dto = new BeregningsresultatEngangsstønadDto();
            LegacyESBeregning beregning = sisteBeregningOpt.get();
            dto.setBeregnetTilkjentYtelse(beregning.getBeregnetTilkjentYtelse());
            dto.setAntallBarn((int) beregning.getAntallBarn());
            dto.setSatsVerdi(beregning.getSatsVerdi());
            return Optional.of(dto);
        }
        return Optional.empty();
    }
}
