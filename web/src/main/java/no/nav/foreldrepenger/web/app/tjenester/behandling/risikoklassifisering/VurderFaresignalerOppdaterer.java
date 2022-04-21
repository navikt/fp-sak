package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
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
    private BehandlingRepository behandlingRepository;

    VurderFaresignalerOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaresignalerOppdaterer(RisikovurderingTjeneste risikovurderingTjeneste,
                                        HistorikkTjenesteAdapter historikkAdapter,
                                        BehandlingRepository behandlingRepository) {
        this.historikkAdapter = historikkAdapter;
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaresignalerDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);

        var risikoklassifisering = risikovurderingTjeneste.hentRisikoklassifisering(referanse)
            .filter(rk -> Kontrollresultat.HØY.equals(rk.kontrollresultat()))
            .orElseThrow(() -> new IllegalStateException("Skal ikke kunne vurdere faresignaler for behandling med id " + referanse.behandlingId()));

        var originalFaresignalVurdering = risikoklassifisering.faresignalVurdering();

        risikovurderingTjeneste.lagreVurderingAvFaresignalerForBehandling(referanse, dto.getFaresignalVurdering());
        lagHistorikkInnslag(dto, dto.getFaresignalVurdering(), originalFaresignalVurdering, param);

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
        }
        return Objects.equals(faresignalVurdering, FaresignalVurdering.INGEN_INNVIRKNING)
            ? HistorikkEndretFeltVerdiType.INGEN_INNVIRKNING : HistorikkEndretFeltVerdiType.INNVIRKNING;
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltVerdiType nyVerdi, HistorikkEndretFeltVerdiType gammelVerdi) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.FARESIGNALER, gammelVerdi, nyVerdi);
    }

}
