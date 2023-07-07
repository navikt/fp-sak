package no.nav.foreldrepenger.domene.rest.historikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;

    VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    public void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, VurderVarigEndringEllerNyoppstartetSNDto dto) {

        oppdaterVedEndretVerdi(HistorikkEndretFeltType.ENDRING_NAERING,
            konvertBooleanTilFaktaEndretVerdiType(dto.getErVarigEndretNaering()));

        if (dto.getBruttoBeregningsgrunnlag() != null) {
            oppdaterVedEndretVerdi(HistorikkEndretFeltType.BRUTTO_NAERINGSINNTEKT, dto.getBruttoBeregningsgrunnlag());
        }

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean endringNæring) {
        if (endringNæring == null) {
            return null;
        }
        return endringNæring ? HistorikkEndretFeltVerdiType.VARIG_ENDRET_NAERING : HistorikkEndretFeltVerdiType.INGEN_VARIG_ENDRING_NAERING;
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType,
                                        HistorikkEndretFeltVerdiType bekreftet) {
        historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, null, bekreftet);
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, Integer sum) {
        historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, null, sum);
    }

}
