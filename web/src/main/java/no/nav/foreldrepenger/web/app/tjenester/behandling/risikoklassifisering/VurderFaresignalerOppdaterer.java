package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaresignalerDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaresignalerOppdaterer implements AksjonspunktOppdaterer<VurderFaresignalerDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private RisikovurderingTjeneste risikovurderingTjeneste;

    VurderFaresignalerOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaresignalerOppdaterer(RisikovurderingTjeneste risikovurderingTjeneste,
                                        HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
        this.risikovurderingTjeneste = risikovurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaresignalerDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        var risikoklassifiseringEntitet = risikovurderingTjeneste.hentRisikoklassifiseringForBehandling(behandlingId)
            .filter(rk -> Kontrollresultat.HØY.equals(rk.getKontrollresultat()))
            .orElseThrow(() -> new IllegalStateException("Skal ikke kunne vurdere faresignaler for behandling med id " + behandlingId));

        var originalFaresignalVurdering = risikoklassifiseringEntitet.getFaresignalVurdering();

        if (dto.getHarInnvirketBehandlingen() == null && dto.getFaresignalVurdering() == null) {
            throw new IllegalStateException("Har ikke mottatt vurdering av faresignaler for behandling med id " + behandlingId);
        }

        if (dto.getFaresignalVurdering() != null && !FaresignalVurdering.UDEFINERT.equals(dto.getFaresignalVurdering())) {
            risikovurderingTjeneste.lagreVurderingAvFaresignalerForBehandling(behandlingId, dto.getFaresignalVurdering());
            lagHistorikkInnslag(dto, dto.getFaresignalVurdering(), originalFaresignalVurdering, param);
        } else if (dto.getHarInnvirketBehandlingen()) {
            risikovurderingTjeneste.lagreVurderingAvFaresignalerForBehandling(behandlingId, FaresignalVurdering.INNVIRKNING);
            lagHistorikkInnslag(dto, FaresignalVurdering.INNVIRKNING, originalFaresignalVurdering, param);
        } else {
            risikovurderingTjeneste.lagreVurderingAvFaresignalerForBehandling(behandlingId, FaresignalVurdering.INGEN_INNVIRKNING);
            lagHistorikkInnslag(dto, FaresignalVurdering.INGEN_INNVIRKNING, originalFaresignalVurdering, param);
        }

        return OppdateringResultat.utenOveropp();
    }

    private void lagHistorikkInnslag(VurderFaresignalerDto dto, FaresignalVurdering vurdering, FaresignalVurdering orginalVurdering, AksjonspunktOppdaterParameter param) {

        oppdaterVedEndretVerdi(finnEndretVerdiType(vurdering), finnEndretVerdiType(orginalVurdering));

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.VURDER_FARESIGNALER);
    }

    private HistorikkEndretFeltVerdiType finnEndretVerdiType(FaresignalVurdering faresignalVurdering) {
        if (faresignalVurdering == null || FaresignalVurdering.UDEFINERT.equals(faresignalVurdering)) {
            return null;
        } else return Objects.equals(faresignalVurdering, FaresignalVurdering.INGEN_INNVIRKNING)
            ? HistorikkEndretFeltVerdiType.INGEN_INNVIRKNING : HistorikkEndretFeltVerdiType.INNVIRKNING;
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltVerdiType nyVerdi, HistorikkEndretFeltVerdiType gammelVerdi) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.FARESIGNALER, gammelVerdi, nyVerdi);
    }

}
