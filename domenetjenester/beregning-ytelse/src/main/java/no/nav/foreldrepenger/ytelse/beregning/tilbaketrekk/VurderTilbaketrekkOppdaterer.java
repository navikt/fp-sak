package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.ytelse.beregning.rest.VurderTilbaketrekkDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderTilbaketrekkDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderTilbaketrekkOppdaterer implements AksjonspunktOppdaterer<VurderTilbaketrekkDto> {

    private BeregningsresultatRepository beregningsresultatRepository;
    private HistorikkTjenesteAdapter historikkAdapter;

    VurderTilbaketrekkOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderTilbaketrekkOppdaterer(BehandlingRepositoryProvider repositoryProvider, HistorikkTjenesteAdapter historikkAdapter) {
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(VurderTilbaketrekkDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        Optional<Boolean> gammelVerdi = beregningsresultatRepository.lagreMedTilbaketrekk(behandling, dto.skalHindreTilbaketrekk());
        Boolean originalSkalHindreTilbaketrekk = gammelVerdi.orElse(null);

        lagHistorikkInnslag(dto, originalSkalHindreTilbaketrekk, param);

        return OppdateringResultat.utenOveropp();
    }

    private void lagHistorikkInnslag(VurderTilbaketrekkDto dto, Boolean originalSkalUtføreTilbaketrekk, AksjonspunktOppdaterParameter param) {

        oppdaterVedEndretVerdi(finnEndretVerdiType(originalSkalUtføreTilbaketrekk), finnEndretVerdiType(dto.skalHindreTilbaketrekk()));

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.TILKJENT_YTELSE);
    }

    private HistorikkEndretFeltVerdiType finnEndretVerdiType(Boolean skalHindreTilbaketrekk) {
        if (skalHindreTilbaketrekk == null) {
            return null;
        } else return skalHindreTilbaketrekk ? HistorikkEndretFeltVerdiType.HINDRE_TILBAKETREKK : HistorikkEndretFeltVerdiType.UTFØR_TILBAKETREKK;
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltVerdiType gammelVerdi, HistorikkEndretFeltVerdiType nyVerdi) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.TILBAKETREKK, gammelVerdi, nyVerdi);
    }
}
